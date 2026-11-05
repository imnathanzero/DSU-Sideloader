package git.imnathanzero.sysphere

import android.app.Application
import com.topjohnwu.superuser.Shell
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SysphereApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_REDIRECT_STDERR or Shell.FLAG_MOUNT_MASTER)
                .setTimeout(15),
        )
    }
}
