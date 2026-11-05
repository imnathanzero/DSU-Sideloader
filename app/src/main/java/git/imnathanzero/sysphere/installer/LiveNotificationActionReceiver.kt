package git.imnathanzero.sysphere.installer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class LiveNotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_CANCEL_INSTALLATION = "git.imnathanzero.sysphere.ACTION_CANCEL_INSTALLATION"
        const val ACTION_REBOOT_DSU = "git.imnathanzero.sysphere.ACTION_REBOOT_DSU"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_CANCEL_INSTALLATION -> {
                Log.d("LiveNotificationReceiver", "Cancel installation requested via notification")
                LiveInstallationManager.getInstance(context).requestCancel()
            }
            ACTION_REBOOT_DSU -> {
                Log.d("LiveNotificationReceiver", "Reboot DSU requested via notification")
                LiveInstallationManager.getInstance(context).requestReboot()
            }
        }
    }
}
