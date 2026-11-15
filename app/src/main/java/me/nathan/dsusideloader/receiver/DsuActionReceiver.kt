package me.nathan.dsusideloader.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.topjohnwu.superuser.Shell
import me.nathan.dsusideloader.notification.InstallationNotificationManager
import me.nathan.dsusideloader.service.DsuInstallationService
import me.nathan.dsusideloader.service.PrivilegedProvider

class DsuActionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "DsuActionReceiver"
        const val ACTION_REBOOT_DSU = "me.nathan.dsusideloader.action.REBOOT_DSU"
        const val ACTION_DISCARD_DSU = "me.nathan.dsusideloader.action.DISCARD_DSU"
        const val ACTION_CANCEL_INSTALLATION = "me.nathan.dsusideloader.action.CANCEL_INSTALLATION"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d(TAG, "onReceive action: $action")

        when (action) {
            ACTION_REBOOT_DSU -> {
                try {
                    NotificationManagerCompat.from(context).cancel(InstallationNotificationManager.NOTIFICATION_ID)
                } catch (_: Exception) {
                }
                DsuInstallationService.stop(context)

                PrivilegedProvider.run(
                    onFail = {
                        Shell.cmd("gsi_tool enable", "svc power reboot || reboot").exec()
                    },
                ) {
                    setEnable(true, true)
                    Shell.cmd("svc power reboot || reboot").exec()
                }
            }

            ACTION_DISCARD_DSU -> {
                try {
                    NotificationManagerCompat.from(context).cancel(InstallationNotificationManager.NOTIFICATION_ID)
                } catch (_: Exception) {
                }
                DsuInstallationService.stop(context)
                DsuInstallationService.onDiscardListener?.invoke()

                PrivilegedProvider.run(
                    onFail = {
                        Shell.cmd("gsi_tool wipe").exec()
                    },
                ) {
                    remove()
                    forceStopPackage("com.android.dynsystem")
                }
            }

            ACTION_CANCEL_INSTALLATION -> {
                DsuInstallationService.cancelActiveInstallation()
                DsuInstallationService.stop(context)
            }
        }
    }
}
