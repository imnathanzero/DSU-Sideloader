package git.imnathanzero.sysphere.ui.util

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

@Composable
fun launcherAcResult(
    result: (Uri) -> Unit,
): ManagedActivityResultLauncher<Intent, ActivityResult> {
    return rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        if (it.resultCode == Activity.RESULT_OK && it.data != null) {
            val uri = it.data!!.data ?: it.data!!.clipData?.getItemAt(0)?.uri
            if (uri != null) {
                result(uri)
            }
        }
    }
}

@Composable
fun launcherMultipleAcResult(
    result: (List<Uri>) -> Unit,
): ManagedActivityResultLauncher<Intent, ActivityResult> {
    return rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        if (it.resultCode == Activity.RESULT_OK && it.data != null) {
            val data = it.data!!
            val uris = mutableListOf<Uri>()
            val clipData = data.clipData
            if (clipData != null) {
                for (i in 0 until clipData.itemCount) {
                    clipData.getItemAt(i).uri?.let { uri -> uris.add(uri) }
                }
            } else if (data.data != null) {
                uris.add(data.data!!)
            }
            if (uris.isNotEmpty()) {
                result(uris)
            }
        }
    }
}
