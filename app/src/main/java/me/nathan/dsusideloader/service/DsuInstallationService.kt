package me.nathan.dsusideloader.service

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Job
import me.nathan.dsusideloader.notification.InstallationNotificationManager
import me.nathan.dsusideloader.preparation.InstallationStep

@AndroidEntryPoint
class DsuInstallationService : Service() {

    @Inject
    lateinit var notificationManager: InstallationNotificationManager

    private var wakeLock: PowerManager.WakeLock? = null

    private var lastStep: InstallationStep = InstallationStep.PROCESSING
    private var lastProgress: Float = 0f
    private var lastPartition: String = ""

    companion object {
        private const val TAG = "DsuInstallationService"

        const val ACTION_START = "me.nathan.dsusideloader.action.START_INSTALLATION_SERVICE"
        const val ACTION_STOP = "me.nathan.dsusideloader.action.STOP_INSTALLATION_SERVICE"
        const val ACTION_UPDATE_PROGRESS = "me.nathan.dsusideloader.action.UPDATE_INSTALLATION_PROGRESS"
        const val ACTION_FINISH_SUCCESS = "me.nathan.dsusideloader.action.FINISH_INSTALLATION_SUCCESS"
        const val ACTION_FINISH_ERROR = "me.nathan.dsusideloader.action.FINISH_INSTALLATION_ERROR"

        const val EXTRA_STEP = "extra_step"
        const val EXTRA_PROGRESS = "extra_progress"
        const val EXTRA_PARTITION = "extra_partition"
        const val EXTRA_ERROR = "extra_error"

        @Volatile
        private var instance: DsuInstallationService? = null

        @Volatile
        var isRunning: Boolean = false
            private set

        @Volatile
        var activeJob: Job? = null

        var onCancelListener: (() -> Unit)? = null
        var onDiscardListener: (() -> Unit)? = null

        fun start(context: Context) {
            val intent = Intent(context, DsuInstallationService::class.java).apply {
                action = ACTION_START
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun updateProgress(
            context: Context,
            step: InstallationStep = InstallationStep.PROCESSING,
            progress: Float = 0f,
            partition: String = "",
        ) {
            instance?.notificationManager?.showProgress(step, progress, partition)
        }

        fun updateProgress(context: Context, progress: Float, partition: String) {
            updateProgress(context, InstallationStep.PROCESSING, progress, partition)
        }

        fun finishSuccess(context: Context) {
            val intent = Intent(context, DsuInstallationService::class.java).apply {
                action = ACTION_FINISH_SUCCESS
            }
            context.startService(intent)
        }

        fun finishError(context: Context, errorMessage: String) {
            val intent = Intent(context, DsuInstallationService::class.java).apply {
                action = ACTION_FINISH_ERROR
                putExtra(EXTRA_ERROR, errorMessage)
            }
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, DsuInstallationService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun cancelActiveInstallation() {
            activeJob?.cancel()
            onCancelListener?.invoke()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.d(TAG, "DsuInstallationService onCreate")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                handleStart()
            }

            ACTION_UPDATE_PROGRESS -> {
                val stepName = intent.getStringExtra(EXTRA_STEP)
                if (stepName != null) {
                    try {
                        lastStep = InstallationStep.valueOf(stepName)
                    } catch (_: Exception) {
                    }
                }
                lastProgress = intent.getFloatExtra(EXTRA_PROGRESS, lastProgress)
                val partition = intent.getStringExtra(EXTRA_PARTITION)
                if (partition != null) {
                    lastPartition = partition
                }
                notificationManager.showProgress(lastStep, lastProgress, lastPartition)
            }

            ACTION_FINISH_SUCCESS -> {
                handleSuccess()
            }

            ACTION_FINISH_ERROR -> {
                val error = intent.getStringExtra(EXTRA_ERROR) ?: ""
                handleError(error)
            }

            ACTION_STOP -> {
                handleStop()
            }
        }
        return START_NOT_STICKY
    }

    private fun handleStart() {
        isRunning = true
        lastStep = InstallationStep.PROCESSING
        lastProgress = 0f
        lastPartition = ""
        acquireWakeLock()
        val notification = notificationManager.createProgressNotification(InstallationStep.PROCESSING, 0f, "")
        startAsForeground(notification)
        Log.d(TAG, "Foreground service started with WakeLock")
    }

    private fun handleSuccess() {
        isRunning = false
        releaseWakeLock()
        notificationManager.showSuccess()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_DETACH)
        }
        stopSelf()
        Log.d(TAG, "Installation success, foreground detached, notification posted")
    }

    private fun handleError(errorMessage: String) {
        isRunning = false
        releaseWakeLock()
        notificationManager.showError(errorMessage)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_DETACH)
        }
        stopSelf()
        Log.d(TAG, "Installation error: $errorMessage")
    }

    private fun handleStop() {
        isRunning = false
        releaseWakeLock()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        notificationManager.cancel()
        stopSelf()
    }

    private fun startAsForeground(notification: Notification) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    InstallationNotificationManager.NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
                )
            } else {
                startForeground(InstallationNotificationManager.NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service", e)
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DSUSideloader:InstallationWakeLock").apply {
                setReferenceCounted(false)
            }
        }
        wakeLock?.let {
            if (!it.isHeld) {
                it.acquire(4 * 60 * 60 * 1000L) // 4 hours maximum timeout
            }
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
        isRunning = false
        releaseWakeLock()
        Log.d(TAG, "DsuInstallationService onDestroy")
    }
}
