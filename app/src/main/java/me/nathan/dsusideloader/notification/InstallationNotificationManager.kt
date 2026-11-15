package me.nathan.dsusideloader.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import me.nathan.dsusideloader.MainActivity
import me.nathan.dsusideloader.R
import me.nathan.dsusideloader.preparation.InstallationStep
import me.nathan.dsusideloader.receiver.DsuActionReceiver

@Singleton
class InstallationNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val TAG = "InstallationNotificationMgr"
        const val CHANNEL_ID_PROGRESS = "dsu_installation_progress"
        const val CHANNEL_ID_ALERTS = "dsu_installation_alerts"
        const val NOTIFICATION_ID = 1337
    }

    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private var cachedLargeIcon: Bitmap? = null

    private var lastProgressPercent: Int = -1
    private var lastNotifyTimeMs: Long = 0L
    private var lastStep: InstallationStep? = null
    private var lastPartition: String = ""

    private fun getLargeIcon(): Bitmap? {
        if (cachedLargeIcon == null) {
            try {
                cachedLargeIcon = BitmapFactory.decodeResource(context.resources, R.drawable.app_icon_mini)
            } catch (_: Exception) {
            }
        }
        return cachedLargeIcon
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val progressChannel = NotificationChannel(
                CHANNEL_ID_PROGRESS,
                context.getString(R.string.notification_channel_progress),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Live progress for DSU installation"
                setShowBadge(true)
            }

            val alertsChannel = NotificationChannel(
                CHANNEL_ID_ALERTS,
                context.getString(R.string.notification_channel_alerts),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Alerts and action notifications when DSU finishes"
                setShowBadge(true)
                enableVibration(true)
            }

            notificationManager.createNotificationChannels(listOf(progressChannel, alertsChannel))
        }
    }

    private fun getPendingIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getActivity(context, 0, intent, flags)
    }

    private fun getActionPendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, DsuActionReceiver::class.java).apply {
            this.action = action
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }

    fun stepToNotificationText(
        step: InstallationStep,
        partition: String,
    ): String {
        return when (step) {
            InstallationStep.COPYING_FILE -> context.getString(R.string.copying_file)
            InstallationStep.DECOMPRESSING_XZ -> context.getString(R.string.decompressing_xz)
            InstallationStep.COMPRESSING_TO_GZ -> context.getString(R.string.compressing_to_gz)
            InstallationStep.DECOMPRESSING_GZIP -> context.getString(R.string.decompressing_gz)
            InstallationStep.EXTRACTING_FILE -> context.getString(R.string.extracting_file)
            InstallationStep.CREATING_PARTITION ->
                if (partition.isNotBlank()) {
                    context.getString(R.string.creating_partition, partition)
                } else {
                    context.getString(R.string.processing)
                }

            InstallationStep.INSTALLING,
            InstallationStep.INSTALLING_ROOTED,
            InstallationStep.PROCESSING_LOG_READABLE,
            -> if (partition.isNotBlank()) {
                context.getString(R.string.installing_partition, partition)
            } else {
                context.getString(R.string.installing)
            }

            InstallationStep.WAITING_USER_CONFIRMATION -> context.getString(R.string.installation_prompt)
            InstallationStep.PROCESSING -> context.getString(R.string.processing)
            else -> context.getString(R.string.processing)
        }
    }

    private fun isAndroid16OrNewer(): Boolean {
        return Build.VERSION.SDK_INT >= 36 ||
            (Build.VERSION.SDK_INT >= 35 && (Build.VERSION.CODENAME == "Baklava" || Build.VERSION.PREVIEW_SDK_INT > 0))
    }

    private fun buildNativeAndroid16Notification(
        title: String,
        contentText: String,
        percent: Int,
        isIndeterminate: Boolean,
    ): Notification? {
        if (!isAndroid16OrNewer()) return null
        return try {
            val builder = Notification.Builder(context, CHANNEL_ID_PROGRESS)
                .setSmallIcon(R.drawable.app_icon_mono)
                .setContentTitle(title)
                .setContentText(contentText)
                .setContentIntent(getPendingIntent())
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(Notification.CATEGORY_PROGRESS)
                .setProgress(100, percent, isIndeterminate)
                .addAction(
                    Notification.Action.Builder(
                        Icon.createWithResource(context, R.drawable.app_icon_mono),
                        context.getString(R.string.cancel),
                        getActionPendingIntent(DsuActionReceiver.ACTION_CANCEL_INSTALLATION, 103),
                    ).build(),
                )

            getLargeIcon()?.let {
                builder.setLargeIcon(Icon.createWithBitmap(it))
            }

            try {
                val progressStyleClass = Class.forName("android.app.Notification\$ProgressStyle")
                val styleObj = progressStyleClass.getDeclaredConstructor().newInstance()
                progressStyleClass.getMethod("setProgress", Int::class.javaPrimitiveType).invoke(styleObj, percent)
                val styleClass = Class.forName("android.app.Notification\$Style")
                Notification.Builder::class.java.getMethod("setStyle", styleClass).invoke(builder, styleObj)
            } catch (e: Exception) {
                Log.d(TAG, "android.app.Notification.ProgressStyle not available: ${e.message}")
            }

            try {
                Notification.Builder::class.java.getMethod("setRequestPromotedOngoing", Boolean::class.javaPrimitiveType)
                    .invoke(builder, true)
            } catch (e: Exception) {
                Log.d(TAG, "setRequestPromotedOngoing not available on Builder: ${e.message}")
            }

            try {
                Notification.Builder::class.java.getMethod("setShortCriticalText", CharSequence::class.java)
                    .invoke(builder, "$percent%")
            } catch (e: Exception) {
                Log.d(TAG, "setShortCriticalText not available on Builder: ${e.message}")
            }

            builder.build()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to build native Android 16 notification", e)
            null
        }
    }

    fun createProgressNotification(
        step: InstallationStep = InstallationStep.PROCESSING,
        progress: Float = 0f,
        partition: String = "",
    ): Notification {
        val safeProgress = progress.coerceIn(0f, 1f)
        val percent = (safeProgress * 100).toInt().coerceIn(0, 100)
        val isIndeterminate = percent == 0 && progress <= 0f

        val title = if (partition.isNotEmpty()) {
            context.getString(R.string.notification_installing_title, partition)
        } else {
            context.getString(R.string.notification_preparing_title)
        }

        val contentText = if (step != InstallationStep.PROCESSING || partition.isEmpty()) {
            stepToNotificationText(step, partition)
        } else if (!isIndeterminate) {
            context.getString(R.string.notification_writing_partition, partition, percent)
        } else {
            stepToNotificationText(step, partition)
        }

        val notification = buildNativeAndroid16Notification(title, contentText, percent, isIndeterminate)
            ?: run {
                val builder = NotificationCompat.Builder(context, CHANNEL_ID_PROGRESS)
                    .setSmallIcon(R.drawable.app_icon_mono)
                    .setLargeIcon(getLargeIcon())
                    .setContentTitle(title)
                    .setContentText(contentText)
                    .setContentIntent(getPendingIntent())
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setProgress(100, percent, isIndeterminate)
                    .addAction(
                        R.drawable.app_icon_mono,
                        context.getString(R.string.cancel),
                        getActionPendingIntent(DsuActionReceiver.ACTION_CANCEL_INSTALLATION, 103),
                    )
                builder.build()
            }

        // Live Updates & Promoted Ongoing attributes (Android 16+ Live Notifications)
        notification.extras.putBoolean("android.requestPromotedOngoing", true)
        notification.extras.putCharSequence("android.shortCriticalText", "$percent%")

        // ColorOS / OxygenOS (OPlus Aqua Dynamics & Live Alerts Capsule)
        notification.extras.putBoolean("oplus.live_alert", true)
        notification.extras.putBoolean("com.oplus.notification.extra.LIVE_ALERT", true)
        notification.extras.putBoolean("oplus_capsule", true)
        notification.extras.putString("capsule_style", "capsule")

        // HyperOS / MIUI Focus Notification (Live Capsule & Island)
        notification.extras.putBoolean("miui.focus_notification", true)
        notification.extras.putBoolean("miui.show_on_capsule", true)
        notification.extras.putBoolean("miui.enable_float", true)
        notification.extras.putString("miui.float_tag", "dsu_install")

        return notification
    }

    fun showProgress(
        step: InstallationStep = InstallationStep.PROCESSING,
        progress: Float = 0f,
        partition: String = "",
        force: Boolean = false,
    ) {
        val safeProgress = progress.coerceIn(0f, 1f)
        val percent = (safeProgress * 100).toInt().coerceIn(0, 100)
        val now = SystemClock.elapsedRealtime()

        val stepChanged = step != lastStep
        val partitionChanged = partition != lastPartition
        val percentChanged = percent != lastProgressPercent
        val timeElapsed = (now - lastNotifyTimeMs) >= 400L

        if (!force && !stepChanged && !partitionChanged && !percentChanged && !timeElapsed) {
            return
        }

        lastStep = step
        lastPartition = partition
        lastProgressPercent = percent
        lastNotifyTimeMs = now

        val notification = createProgressNotification(step, progress, partition)
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
        }
    }

    fun showProgress(progress: Float, partition: String) {
        showProgress(InstallationStep.PROCESSING, progress, partition, force = false)
    }

    fun showSuccess() {
        lastProgressPercent = -1
        lastStep = null
        lastPartition = ""

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_ALERTS)
            .setSmallIcon(R.drawable.app_icon_mono)
            .setLargeIcon(getLargeIcon())
            .setContentTitle(context.getString(R.string.notification_success_title))
            .setContentText(context.getString(R.string.notification_success_desc))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(context.getString(R.string.notification_success_title))
                    .bigText(context.getString(R.string.notification_success_desc)),
            )
            .setContentIntent(getPendingIntent())
            .setOngoing(false)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .addAction(
                R.drawable.app_icon_mono,
                context.getString(R.string.reboot_into_dsu),
                getActionPendingIntent(DsuActionReceiver.ACTION_REBOOT_DSU, 101),
            )
            .addAction(
                R.drawable.app_icon_mono,
                context.getString(R.string.discard),
                getActionPendingIntent(DsuActionReceiver.ACTION_DISCARD_DSU, 102),
            )

        val notification = builder.build().apply {
            extras.putBoolean("android.requestPromotedOngoing", false)
            extras.putBoolean("oplus.live_alert", false)
            extras.putBoolean("miui.focus_notification", false)
        }

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
        }
    }

    fun showError(errorMessage: String) {
        lastProgressPercent = -1
        lastStep = null
        lastPartition = ""

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_ALERTS)
            .setSmallIcon(R.drawable.app_icon_mono)
            .setLargeIcon(getLargeIcon())
            .setContentTitle(context.getString(R.string.notification_error_title))
            .setContentText(errorMessage.ifEmpty { "An error occurred during installation." })
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(context.getString(R.string.notification_error_title))
                    .bigText(errorMessage.ifEmpty { "An error occurred during installation." }),
            )
            .setContentIntent(getPendingIntent())
            .setOngoing(false)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ERROR)

        val notification = builder.build().apply {
            extras.putBoolean("android.requestPromotedOngoing", false)
            extras.putBoolean("oplus.live_alert", false)
            extras.putBoolean("miui.focus_notification", false)
        }

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
        }
    }

    fun cancel() {
        lastProgressPercent = -1
        lastStep = null
        lastPartition = ""
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
