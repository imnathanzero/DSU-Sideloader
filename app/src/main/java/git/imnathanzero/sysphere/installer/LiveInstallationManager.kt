package git.imnathanzero.sysphere.installer

import android.content.Context
import android.util.Log
import com.topjohnwu.superuser.Shell
import dagger.hilt.android.qualifiers.ApplicationContext
import git.imnathanzero.sysphere.R
import git.imnathanzero.sysphere.preparation.InstallationStep
import git.imnathanzero.sysphere.service.PrivilegedProvider
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Coordinates live installation reactive UI state (for composables such as [LiveInstallationBanner])
 * with system notifications dispatched via [InstallationLiveUpdateNotifier].
 */
@Singleton
class LiveInstallationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    val notifier: InstallationLiveUpdateNotifier,
) {

    private val tag = this.javaClass.simpleName

    /**
     * Fallback constructor for manual non-Hilt resolution.
     */
    constructor(context: Context) : this(
        context,
        InstallationLiveUpdateNotifier.getInstance(context),
    )

    companion object {
        const val CHANNEL_INSTALLATION = InstallationLiveUpdateNotifier.CHANNEL_INSTALLATION
        const val CHANNEL_ALERTS = InstallationLiveUpdateNotifier.CHANNEL_ALERTS
        const val NOTIFICATION_ID = InstallationLiveUpdateNotifier.NOTIFICATION_ID
        const val ALERT_NOTIFICATION_ID = InstallationLiveUpdateNotifier.ALERT_NOTIFICATION_ID

        @Volatile
        private var instance: LiveInstallationManager? = null

        fun getInstance(context: Context): LiveInstallationManager {
            return instance ?: synchronized(this) {
                instance ?: LiveInstallationManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    init {
        instance = this
    }

    private val _liveState = MutableStateFlow(LiveInstallationState())
    val liveState: StateFlow<LiveInstallationState> = _liveState.asStateFlow()

    var onCancelRequested: (() -> Unit)? = null
    var onRebootRequested: (() -> Unit)? = null

    /**
     * Begins installation lifecycle and displays initial progress notification.
     */
    fun startInstallation(
        step: InstallationStep,
        title: String,
        stepText: String = "",
        isIndeterminate: Boolean = true,
    ) {
        notifier.cancelAlertNotification()

        _liveState.value = LiveInstallationState(
            isInstalling = true,
            step = step,
            progress = 0F,
            isIndeterminate = isIndeterminate,
            title = title,
            stepText = stepText.ifEmpty { title },
            isSuccess = false,
            isError = false,
        )

        notifier.notifyProgress(
            title = title,
            content = stepText.ifEmpty { title },
            progress = 0F,
            isIndeterminate = isIndeterminate,
            force = true,
        )
    }

    /**
     * Updates ongoing installation progress and triggers throttled notification update.
     */
    fun updateProgress(
        progress: Float,
        partition: String = "",
        stepText: String = "",
    ) {
        _liveState.update { current ->
            current.copy(
                progress = progress,
                partition = partition.ifEmpty { current.partition },
                stepText = stepText.ifEmpty { current.stepText },
                isIndeterminate = false,
            )
        }

        val state = _liveState.value
        val percentage = (progress * 100).toInt()
        val title = if (state.partition.isNotEmpty()) {
            context.getString(R.string.installing_partition, state.partition)
        } else {
            state.title.ifEmpty { context.getString(R.string.notification_installing_title) }
        }
        val content = buildString {
            append("$percentage%")
            if (state.partition.isNotEmpty()) {
                append(" • ${state.partition}")
            } else if (state.stepText.isNotEmpty()) {
                append(" • ${state.stepText}")
            }
        }

        notifier.notifyProgress(
            title = title,
            content = content,
            progress = progress,
            isIndeterminate = false,
        )
    }

    /**
     * Updates installation step and forces an immediate notification update.
     */
    fun updateStep(
        step: InstallationStep,
        stepText: String,
        partition: String = "",
        isIndeterminate: Boolean = false,
    ) {
        _liveState.update { current ->
            current.copy(
                step = step,
                stepText = stepText,
                partition = partition.ifEmpty { current.partition },
                isIndeterminate = isIndeterminate,
            )
        }

        val title = if (partition.isNotEmpty()) {
            context.getString(R.string.installing_partition, partition)
        } else {
            stepText
        }

        notifier.notifyProgress(
            title = title,
            content = stepText,
            progress = _liveState.value.progress,
            isIndeterminate = isIndeterminate,
            force = true,
        )
    }

    /**
     * Marks installation successful and triggers success alert notification.
     */
    fun notifySuccess(canReboot: Boolean, title: String = "", text: String = "") {
        val finalTitle = title.ifEmpty { context.getString(R.string.notification_completed_title) }
        val finalText = text.ifEmpty { context.getString(R.string.notification_completed_text) }

        _liveState.update {
            it.copy(
                isInstalling = false,
                isSuccess = true,
                canReboot = canReboot,
                title = finalTitle,
                stepText = finalText,
                progress = 1.0F,
                isError = false,
            )
        }

        notifier.notifySuccess(
            canReboot = canReboot,
            title = finalTitle,
            message = finalText,
        )
    }

    /**
     * Marks installation failed and triggers error alert notification.
     */
    fun notifyError(
        step: InstallationStep,
        errorContent: String,
        title: String = "",
    ) {
        val alertTitle = title.ifEmpty { context.getString(R.string.notification_failed_title) }
        val alertText = errorContent.ifEmpty { context.getString(R.string.unknown_error) }

        _liveState.update {
            it.copy(
                isInstalling = false,
                isError = true,
                step = step,
                errorText = alertText,
                title = alertTitle,
                stepText = alertText,
                isSuccess = false,
            )
        }

        notifier.notifyError(
            title = alertTitle,
            errorText = alertText,
        )
    }

    /**
     * Resets reactive state and clears all active notifications.
     */
    fun cancel() {
        _liveState.value = LiveInstallationState()
        notifier.cancelAll()
    }

    /**
     * Dismisses the in-app alert banner and alert notification.
     */
    fun dismissAlert() {
        _liveState.update {
            it.copy(isSuccess = false, isError = false)
        }
        notifier.cancelAlertNotification()
    }

    /**
     * Invoked when cancellation is requested (e.g. from notification cancel action).
     */
    fun requestCancel() {
        Log.d(tag, "requestCancel() triggered")
        onCancelRequested?.invoke()
        cancel()
    }

    /**
     * Invoked when reboot is requested (e.g. from notification reboot action).
     */
    fun requestReboot() {
        Log.d(tag, "requestReboot() triggered")
        if (onRebootRequested != null) {
            onRebootRequested?.invoke()
        } else {
            try {
                PrivilegedProvider.run {
                    setEnable(true, true)
                    Shell.cmd("reboot").exec()
                }
            } catch (e: Exception) {
                Log.e(tag, "Error during reboot: ${e.message}")
            }
        }
        dismissAlert()
    }
}
