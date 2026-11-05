package git.imnathanzero.sysphere.model

import android.net.Uri
import java.util.Locale

data class SelectedImageFile(
    val uri: Uri,
    val name: String,
    val partitionName: String,
    val fileSize: Long = -1L,
) {
    val formattedSize: String
        get() {
            if (fileSize <= 0L) return ""
            val mb = fileSize / (1024.0 * 1024.0)
            val gb = mb / 1024.0
            return if (gb >= 1.0) {
                String.format(Locale.US, "%.2f GB", gb)
            } else {
                String.format(Locale.US, "%.1f MB", mb)
            }
        }
}
