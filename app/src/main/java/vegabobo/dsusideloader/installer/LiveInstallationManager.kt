package vegabobo.dsusideloader.installer

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.topjohnwu.superuser.Shell
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import vegabobo.dsusideloader.MainActivity
import vegabobo.dsusideloader.R
import vegabobo.dsusideloader.preparation.InstallationStep
import vegabobo.dsusideloader.service.PrivilegedProvider

@Singleton
class LiveInstallationManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val tag = this.javaClass.simpleName

    companion object {
        const val CHANNEL_INSTALLATION = "dsu_live_installation"
        const val CHANNEL_ALERTS = "dsu_alerts"
        const val NOTIFICATION_ID = 2001
        const val ALERT_NOTIFICATION_ID = 2002

        @Volatile
        private var instance: LiveInstallationManager? = null

        fun getInstance(context: Context): LiveInstallationManager {
            return instance ?: synchronized(this) {
                instance ?: LiveInstallationManager(context.applicationContext).also { instance = it }
            }
        }
    }

    init {
        instance = this
        createNotificationChannels()
    }

    private val _liveState = MutableStateFlow(LiveInstallationState())
    val liveState: StateFlow<LiveInstallationState> = _liveState.asStateFlow()

    var onCancelRequested: (() -> Unit)? = null
    var onRebootRequested: (() -> Unit)? = null

    private var lastNotificationUpdateTime = 0L
    private var lastPublishedPercentage = -1

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val installChannel = NotificationChannel(
                CHANNEL_INSTALLATION,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(R.string.notification_channel_description)
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }

            val alertsChannel = NotificationChannel(
                CHANNEL_ALERTS,
                context.getString(R.string.notification_alerts_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.notification_alerts_channel_description)
                setShowBadge(true)
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(installChannel)
            notificationManager.createNotificationChannel(alertsChannel)
        }
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun createOpenAppPendingIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            100,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createCancelPendingIntent(): PendingIntent {
        val intent = Intent(context, LiveNotificationActionReceiver::class.java).apply {
            action = LiveNotificationActionReceiver.ACTION_CANCEL_INSTALLATION
        }
        return PendingIntent.getBroadcast(
            context,
            101,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createRebootPendingIntent(): PendingIntent {
        val intent = Intent(context, LiveNotificationActionReceiver::class.java).apply {
            action = LiveNotificationActionReceiver.ACTION_REBOOT_DSU
        }
        return PendingIntent.getBroadcast(
            context,
            102,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun startInstallation(
        step: InstallationStep,
        title: String,
        stepText: String = "",
        isIndeterminate: Boolean = true,
    ) {
        lastNotificationUpdateTime = 0L
        lastPublishedPercentage = -1
        NotificationManagerCompat.from(context).cancel(ALERT_NOTIFICATION_ID)

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

        updateLiveNotification(
            title = title,
            content = stepText.ifEmpty { title },
            progress = 0F,
            isIndeterminate = isIndeterminate,
        )
    }

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

        val percentage = (progress * 100).toInt()
        val now = System.currentTimeMillis()
        if (now - lastNotificationUpdateTime >= 400L || percentage != lastPublishedPercentage) {
            lastNotificationUpdateTime = now
            lastPublishedPercentage = percentage

            val state = _liveState.value
            val content = buildString {
                append("$percentage%")
                if (state.partition.isNotEmpty()) {
                    append(" • ${state.partition}")
                } else if (state.stepText.isNotEmpty()) {
                    append(" • ${state.stepText}")
                }
            }

            updateLiveNotification(
                title = if (state.partition.isNotEmpty()) {
                    context.getString(R.string.installing_partition, state.partition)
                } else {
                    state.title.ifEmpty { context.getString(R.string.notification_installing_title) }
                },
                content = content,
                progress = progress,
                isIndeterminate = false,
            )
        }
    }

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

        updateLiveNotification(
            title = if (partition.isNotEmpty()) {
                context.getString(R.string.installing_partition, partition)
            } else {
                stepText
            },
            content = stepText,
            progress = _liveState.value.progress,
            isIndeterminate = isIndeterminate,
        )
    }

    private fun updateLiveNotification(
        title: String,
        content: String,
        progress: Float,
        isIndeterminate: Boolean,
    ) {
        if (!hasNotificationPermission()) return

        try {
            val progressInt = (progress * 100).toInt().coerceIn(0, 100)
            val builder = NotificationCompat.Builder(context, CHANNEL_INSTALLATION)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(content)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(createOpenAppPendingIntent())
                .addAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    context.getString(R.string.notification_cancel_action),
                    createCancelPendingIntent(),
                )

            if (isIndeterminate) {
                builder.setProgress(0, 0, true)
            } else {
                builder.setProgress(100, progressInt, false)
            }

            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            Log.w(tag, "Notification permission missing: ${e.message}")
        } catch (e: Exception) {
            Log.e(tag, "Failed to update notification: ${e.message}")
        }
    }

    fun notifySuccess(canReboot: Boolean, title: String = "", text: String = "") {
        _liveState.update {
            it.copy(
                isInstalling = false,
                isSuccess = true,
                canReboot = canReboot,
                title = title.ifEmpty { context.getString(R.string.notification_completed_title) },
                stepText = text.ifEmpty { context.getString(R.string.notification_completed_text) },
                progress = 1.0F,
                isError = false,
            )
        }

        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)

        if (!hasNotificationPermission()) return

        try {
            val alertTitle = title.ifEmpty { context.getString(R.string.notification_completed_title) }
            val alertText = text.ifEmpty { context.getString(R.string.notification_completed_text) }

            val builder = NotificationCompat.Builder(context, CHANNEL_ALERTS)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(alertTitle)
                .setContentText(alertText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(alertText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setOngoing(false)
                .setContentIntent(createOpenAppPendingIntent())
                .addAction(
                    android.R.drawable.ic_menu_view,
                    context.getString(R.string.notification_open_action),
                    createOpenAppPendingIntent(),
                )

            if (canReboot) {
                builder.addAction(
                    android.R.drawable.ic_media_play,
                    context.getString(R.string.notification_reboot_action),
                    createRebootPendingIntent(),
                )
            }

            NotificationManagerCompat.from(context).notify(ALERT_NOTIFICATION_ID, builder.build())
        } catch (e: Exception) {
            Log.e(tag, "Failed to post success notification: ${e.message}")
        }
    }

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

        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)

        if (!hasNotificationPermission()) return

        try {
            val builder = NotificationCompat.Builder(context, CHANNEL_ALERTS)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(alertTitle)
                .setContentText(alertText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(alertText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setOngoing(false)
                .setContentIntent(createOpenAppPendingIntent())
                .addAction(
                    android.R.drawable.ic_menu_view,
                    context.getString(R.string.notification_open_action),
                    createOpenAppPendingIntent(),
                )

            NotificationManagerCompat.from(context).notify(ALERT_NOTIFICATION_ID, builder.build())
        } catch (e: Exception) {
            Log.e(tag, "Failed to post error notification: ${e.message}")
        }
    }

    fun cancel() {
        _liveState.value = LiveInstallationState()
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        NotificationManagerCompat.from(context).cancel(ALERT_NOTIFICATION_ID)
    }

    fun dismissAlert() {
        _liveState.update {
            it.copy(isSuccess = false, isError = false)
        }
        NotificationManagerCompat.from(context).cancel(ALERT_NOTIFICATION_ID)
    }

    fun requestCancel() {
        Log.d(tag, "requestCancel() triggered")
        onCancelRequested?.invoke()
        cancel()
    }

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
