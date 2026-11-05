package vegabobo.dsusideloader.ui.screen.partitions

import java.util.Locale

data class PartitionInfo(
    val name: String,
    val path: String,
    val sizeBytes: Long = 0L,
    val isMounted: Boolean = false,
    val mountPoint: String = "",
    val filesystem: String = "",
    val isReadOnly: Boolean = true,
) {
    val formattedSize: String
        get() {
            if (sizeBytes <= 0L) return "Unknown size"
            val kb = sizeBytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
                mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
                else -> String.format(Locale.US, "%.0f KB", kb)
            }
        }
}
