package git.imnathanzero.sysphere.installer

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
import dagger.hilt.android.qualifiers.ApplicationContext
import git.imnathanzero.sysphere.MainActivity
import git.imnathanzero.sysphere.R
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dedicated notifier responsible for posting, updating, and dismissing Android system
 * notifications representing live DSU installation progress, completion, and error states.
 */
@Singleton
open class InstallationLiveUpdateNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    companion object {
        private const val TAG = "LiveUpdateNotifier"

        const val CHANNEL_INSTALLATION = "dsu_live_installation"
        const val CHANNEL_ALERTS = "dsu_alerts"
        const val NOTIFICATION_ID = 2001
        const val ALERT_NOTIFICATION_ID = 2002

        const val ACTION_CANCEL_INSTALLATION = "git.imnathanzero.sysphere.ACTION_CANCEL_INSTALLATION"
        const val ACTION_REBOOT_DSU = "git.imnathanzero.sysphere.ACTION_REBOOT_DSU"

        // Minimum time interval (ms) between notification updates during continuous flashing
        private const val THROTTLE_INTERVAL_MS = 250L

        @Volatile
        private var instance: InstallationLiveUpdateNotifier? = null

        fun getInstance(context: Context): InstallationLiveUpdateNotifier {
            return instance ?: synchronized(this) {
                instance ?: InstallationLiveUpdateNotifier(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    private var lastUpdateTimeMs: Long = 0L
    private var lastPublishedPercentage: Int = -1

    init {
        instance = this
        createNotificationChannels()
    }

    /**
     * Creates required notification channels for live installation progress and alerts.
     */
    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    ?: return

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

    /**
     * Checks if the app has permission to post notifications (Android 13+).
     */
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Creates PendingIntent to launch the main application interface.
     */
    fun createOpenAppPendingIntent(): PendingIntent {
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

    /**
     * Creates PendingIntent to cancel the ongoing installation.
     */
    fun createCancelPendingIntent(): PendingIntent {
        val intent = Intent(context, LiveNotificationActionReceiver::class.java).apply {
            action = ACTION_CANCEL_INSTALLATION
        }
        return PendingIntent.getBroadcast(
            context,
            101,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /**
     * Creates PendingIntent to reboot device into the installed Dynamic System.
     */
    fun createRebootPendingIntent(): PendingIntent {
        val intent = Intent(context, LiveNotificationActionReceiver::class.java).apply {
            action = ACTION_REBOOT_DSU
        }
        return PendingIntent.getBroadcast(
            context,
            102,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /**
     * Posts or updates the ongoing live installation notification with smart throttling.
     *
     * @param title Title of notification (e.g. partition name or general step).
     * @param content Body description of current progress.
     * @param progress Normalized progress from 0.0 to 1.0.
     * @param isIndeterminate True if progress is indeterminate.
     * @param force True to bypass throttling (e.g. on step transitions).
     */
    fun notifyProgress(
        title: String,
        content: String,
        progress: Float,
        isIndeterminate: Boolean,
        force: Boolean = false,
    ) {
        if (!hasNotificationPermission()) return

        val percentage = (progress * 100).toInt().coerceIn(0, 100)
        val now = System.currentTimeMillis()

        if (!force && !isIndeterminate) {
            val isBoundary = percentage == 0 || percentage == 100
            val timeElapsed = now - lastUpdateTimeMs >= THROTTLE_INTERVAL_MS
            val percentChanged = percentage != lastPublishedPercentage

            if (!isBoundary && (!timeElapsed || !percentChanged)) {
                return
            }
        }

        lastUpdateTimeMs = now
        lastPublishedPercentage = percentage

        try {
            val builder = NotificationCompat.Builder(context, CHANNEL_INSTALLATION)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(content)
                .setSubText(context.getString(R.string.app_name))
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(createOpenAppPendingIntent())
                .addAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    context.getString(R.string.notification_cancel_action),
                    createCancelPendingIntent(),
                )

            if (isIndeterminate) {
                builder.setProgress(0, 0, true)
            } else {
                builder.setProgress(100, percentage, false)
            }

            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification permission missing: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update notification: ${e.message}")
        }
    }

    /**
     * Dismisses the ongoing progress notification and posts a completion alert.
     */
    fun notifySuccess(
        canReboot: Boolean,
        title: String = "",
        message: String = "",
    ) {
        cancelProgressNotification()
        if (!hasNotificationPermission()) return

        val alertTitle = title.ifEmpty { context.getString(R.string.notification_completed_title) }
        val alertText = message.ifEmpty { context.getString(R.string.notification_completed_text) }

        try {
            val builder = NotificationCompat.Builder(context, CHANNEL_ALERTS)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(alertTitle)
                .setContentText(alertText)
                .setSubText(context.getString(R.string.app_name))
                .setStyle(NotificationCompat.BigTextStyle().bigText(alertText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
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
            Log.e(TAG, "Failed to post success notification: ${e.message}")
        }
    }

    /**
     * Dismisses the ongoing progress notification and posts an error alert.
     */
    fun notifyError(
        title: String = "",
        errorText: String = "",
    ) {
        cancelProgressNotification()
        if (!hasNotificationPermission()) return

        val alertTitle = title.ifEmpty { context.getString(R.string.notification_failed_title) }
        val alertMessage = errorText.ifEmpty { context.getString(R.string.unknown_error) }

        try {
            val builder = NotificationCompat.Builder(context, CHANNEL_ALERTS)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(alertTitle)
                .setContentText(alertMessage)
                .setSubText(context.getString(R.string.app_name))
                .setStyle(NotificationCompat.BigTextStyle().bigText(alertMessage))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ERROR)
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
            Log.e(TAG, "Failed to post error notification: ${e.message}")
        }
    }

    /**
     * Synchronizes notification presentation directly with a LiveInstallationState snapshot.
     */
    fun notifyState(state: LiveInstallationState) {
        when {
            state.isSuccess -> {
                notifySuccess(
                    canReboot = state.canReboot,
                    title = state.title,
                    message = state.stepText,
                )
            }
            state.isError -> {
                notifyError(
                    title = state.title,
                    errorText = state.errorText.ifEmpty { state.stepText },
                )
            }
            state.isInstalling -> {
                val title = if (state.partition.isNotEmpty()) {
                    context.getString(R.string.installing_partition, state.partition)
                } else {
                    state.title.ifEmpty { context.getString(R.string.notification_installing_title) }
                }
                val percentage = (state.progress * 100).toInt()
                val content = buildString {
                    if (!state.isIndeterminate) {
                        append("$percentage%")
                        if (state.partition.isNotEmpty()) {
                            append(" • ${state.partition}")
                        } else if (state.stepText.isNotEmpty()) {
                            append(" • ${state.stepText}")
                        }
                    } else {
                        append(state.stepText.ifEmpty { title })
                    }
                }
                notifyProgress(
                    title = title,
                    content = content,
                    progress = state.progress,
                    isIndeterminate = state.isIndeterminate,
                )
            }
            else -> {
                cancelAll()
            }
        }
    }

    /**
     * Cancels the ongoing progress notification.
     */
    fun cancelProgressNotification() {
        lastPublishedPercentage = -1
        lastUpdateTimeMs = 0L
        try {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cancel progress notification: ${e.message}")
        }
    }

    /**
     * Cancels the completion or error alert notification.
     */
    fun cancelAlertNotification() {
        try {
            NotificationManagerCompat.from(context).cancel(ALERT_NOTIFICATION_ID)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cancel alert notification: ${e.message}")
        }
    }

    /**
     * Dismisses all notifications managed by this notifier.
     */
    fun cancelAll() {
        cancelProgressNotification()
        cancelAlertNotification()
    }
}
