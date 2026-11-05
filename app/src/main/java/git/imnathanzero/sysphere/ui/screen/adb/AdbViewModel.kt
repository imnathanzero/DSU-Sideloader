package git.imnathanzero.sysphere.ui.screen.adb

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import git.imnathanzero.sysphere.model.Session
import javax.inject.Inject

@HiltViewModel
class AdbViewModel @Inject constructor(
    private val session: Session,
) : ViewModel() {

    fun obtainScriptPath(): String = session.installationScriptPath
}
