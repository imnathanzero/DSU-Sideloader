package vegabobo.dsusideloader.ui.screen.partitions

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.topjohnwu.superuser.Shell
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vegabobo.dsusideloader.model.Session

@HiltViewModel
class PartitionsViewModel @Inject constructor(
    val session: Session,
    val application: Application,
) : ViewModel() {

    companion object {
        const val MOUNT_BASE_PATH = "/data/local/DSUMounts"
    }

    private data class MountEntry(
        val device: String,
        val mountPoint: String,
        val filesystem: String,
        val isReadOnly: Boolean,
    )

    private val _uiState = MutableStateFlow(PartitionsUiState())
    val uiState: StateFlow<PartitionsUiState> = _uiState.asStateFlow()

    init {
        val root = session.isRoot()
        _uiState.update { it.copy(isRoot = root) }
        if (root) {
            scanPartitions()
        }
    }

    fun isRoot(): Boolean = session.isRoot()

    fun scanPartitions() {
        if (!session.isRoot()) {
            _uiState.update { it.copy(isRoot = false, isLoading = false) }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, errorMessage = "") }

            // Ensure base mount directory exists
            Shell.cmd("mkdir -p $MOUNT_BASE_PATH").exec()

            // 1. Inspect active mounts from /proc/mounts
            val mountsResult = Shell.cmd("cat /proc/mounts | grep '$MOUNT_BASE_PATH'").exec()
            val activeMounts = mutableMapOf<String, MountEntry>()
            for (line in mountsResult.out) {
                val parts = line.trim().split(Regex("\\s+"))
                if (parts.size >= 4) {
                    val dev = parts[0]
                    val mountPoint = parts[1]
                    val fs = parts[2]
                    val opts = parts[3].split(",")
                    val isRo = opts.contains("ro")
                    val partitionName = mountPoint.removePrefix("$MOUNT_BASE_PATH/").removePrefix("/")
                    if (partitionName.isNotEmpty()) {
                        activeMounts[partitionName] = MountEntry(
                            device = dev,
                            mountPoint = mountPoint,
                            filesystem = fs,
                            isReadOnly = isRo,
                        )
                    }
                }
            }

            // 2. Discover partition images and mapper devices
            val scanCommand = "find /data/gsi -type f 2>/dev/null; ls -1 /dev/block/mapper/dsu* /dev/block/mapper/gsi* 2>/dev/null"
            val scanResult = Shell.cmd(scanCommand).exec()

            val partitionsList = mutableListOf<PartitionInfo>()
            val seenNames = mutableSetOf<String>()

            for (rawPath in scanResult.out) {
                val path = rawPath.trim()
                if (path.isEmpty()) continue

                val filename = path.substringAfterLast("/")
                val name = filename
                    .removeSuffix(".img")
                    .removeSuffix("_gsi")
                    .removePrefix("dsu-")
                    .removePrefix("gsi_")

                if (name.isEmpty() || seenNames.contains(name)) continue
                seenNames.add(name)

                // Query size
                val sizeOut = Shell.cmd("stat -c %s \"$path\" 2>/dev/null || blockdev --getsize64 \"$path\" 2>/dev/null")
                    .exec().out.firstOrNull()?.trim() ?: "0"
                val sizeBytes = sizeOut.toLongOrNull() ?: 0L

                val mountEntry = activeMounts[name]
                partitionsList.add(
                    PartitionInfo(
                        name = name,
                        path = path,
                        sizeBytes = sizeBytes,
                        isMounted = mountEntry != null,
                        mountPoint = mountEntry?.mountPoint ?: "$MOUNT_BASE_PATH/$name",
                        filesystem = mountEntry?.filesystem ?: "",
                        isReadOnly = mountEntry?.isReadOnly ?: true,
                    ),
                )
            }

            // 3. Include any other active mounts under MOUNT_BASE_PATH not found in /data/gsi
            for ((name, entry) in activeMounts) {
                if (!seenNames.contains(name)) {
                    partitionsList.add(
                        PartitionInfo(
                            name = name,
                            path = entry.device,
                            sizeBytes = 0L,
                            isMounted = true,
                            mountPoint = entry.mountPoint,
                            filesystem = entry.filesystem,
                            isReadOnly = entry.isReadOnly,
                        ),
                    )
                }
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    partitions = partitionsList.sortedBy { p -> p.name },
                )
            }
        }
    }

    fun mountPartition(partition: PartitionInfo, readOnly: Boolean = true) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, errorMessage = "") }
            val target = "$MOUNT_BASE_PATH/${partition.name}"
            val loopOpt = if (readOnly) "loop,ro" else "loop,rw"
            val fallbackOpt = if (readOnly) "ro" else "rw"
            val cmd = "mkdir -p \"$target\" && (mount -o $loopOpt \"${partition.path}\" \"$target\" || " +
                "mount -o $fallbackOpt \"${partition.path}\" \"$target\" || " +
                "mount \"${partition.path}\" \"$target\")"
            val result = Shell.cmd(cmd).exec()
            if (!result.isSuccess) {
                val err = result.err.joinToString("\n").ifEmpty { "Failed to mount ${partition.name}" }
                _uiState.update { it.copy(errorMessage = err) }
            }
            scanPartitions()
        }
    }

    fun unmountPartition(partition: PartitionInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, errorMessage = "") }
            val target = partition.mountPoint.ifEmpty { "$MOUNT_BASE_PATH/${partition.name}" }
            val cmd = "(umount \"$target\" || umount -l \"$target\") && rmdir \"$target\""
            val result = Shell.cmd(cmd).exec()
            if (!result.isSuccess) {
                val err = result.err.joinToString("\n").ifEmpty { "Failed to unmount ${partition.name}" }
                _uiState.update { it.copy(errorMessage = err) }
            }
            scanPartitions()
        }
    }

    fun mountAll(readOnly: Boolean = true) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }
            val unmounted = _uiState.value.partitions.filter { !it.isMounted }
            for (p in unmounted) {
                val target = "$MOUNT_BASE_PATH/${p.name}"
                val loopOpt = if (readOnly) "loop,ro" else "loop,rw"
                val fallbackOpt = if (readOnly) "ro" else "rw"
                val cmd = "mkdir -p \"$target\" && (mount -o $loopOpt \"${p.path}\" \"$target\" || " +
                    "mount -o $fallbackOpt \"${p.path}\" \"$target\" || " +
                    "mount \"${p.path}\" \"$target\")"
                Shell.cmd(cmd).exec()
            }
            scanPartitions()
        }
    }

    fun unmountAll() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }
            val mounted = _uiState.value.partitions.filter { it.isMounted }
            for (p in mounted) {
                val target = p.mountPoint.ifEmpty { "$MOUNT_BASE_PATH/${p.name}" }
                val cmd = "(umount \"$target\" || umount -l \"$target\") && rmdir \"$target\""
                Shell.cmd(cmd).exec()
            }
            scanPartitions()
        }
    }

    fun promptDeletePartition(partition: PartitionInfo) {
        _uiState.update { it.copy(partitionToDelete = partition) }
    }

    fun confirmDeletePartition() {
        val partition = _uiState.value.partitionToDelete ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, partitionToDelete = null) }
            if (partition.isMounted) {
                val target = partition.mountPoint.ifEmpty { "$MOUNT_BASE_PATH/${partition.name}" }
                Shell.cmd("(umount \"$target\" || umount -l \"$target\") && rmdir \"$target\"").exec()
            }
            Shell.cmd("rm -f \"${partition.path}\"").exec()
            scanPartitions()
        }
    }

    fun dismissDeletePrompt() {
        _uiState.update { it.copy(partitionToDelete = null) }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = "") }
    }
}
