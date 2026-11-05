package git.imnathanzero.sysphere.ui.screen.partitions

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.topjohnwu.superuser.Shell
import dagger.hilt.android.lifecycle.HiltViewModel
import git.imnathanzero.sysphere.model.Session
import git.imnathanzero.sysphere.service.PrivilegedProvider
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

            // Ensure base mount directory exists with proper permissions and traverse access
            Shell.cmd(
                "chmod 755 /data/local 2>/dev/null; " +
                    "mkdir -p $MOUNT_BASE_PATH && chmod 777 $MOUNT_BASE_PATH && " +
                    "chcon u:object_r:system_data_file:s0 $MOUNT_BASE_PATH 2>/dev/null",
            ).exec()

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

            val partitionsList = mutableListOf<PartitionInfo>()
            val seenNames = mutableSetOf<String>()

            // 2. Discover DSU partitions via gsid (official Android ImageManager API)
            if (PrivilegedProvider.isConnected()) {
                try {
                    val service = PrivilegedProvider.getService()
                    val prefixes = runCatching { service.imagePrefixes }.getOrDefault(listOf("dsu/dsu/", "dsu/"))
                    for (prefix in prefixes) {
                        val images = runCatching { service.getDsuImages(prefix) }.getOrDefault(emptyList())
                        for (img in images) {
                            val cleanName = img.removeSuffix(".img").removeSuffix("_gsi").removePrefix("dsu-").removePrefix("gsi_")
                            if (cleanName.isEmpty() || seenNames.contains(cleanName)) continue
                            seenNames.add(cleanName)

                            val isMapped = runCatching { service.isDsuImageMapped(prefix, img) }.getOrDefault(false)
                            val mappedDev = if (isMapped) {
                                Shell.cmd("ls -1 /dev/block/mapper/dsu-$cleanName /dev/block/mapper/gsi_$cleanName /dev/block/mapper/$img 2>/dev/null")
                                    .exec().out.firstOrNull()?.trim() ?: ""
                            } else {
                                ""
                            }

                            // Resolve backing file path (.0000 extent or base file)
                            val candidatePaths = listOf(
                                "/data/gsi/$prefix$img.0000",
                                "/data/gsi/$prefix$img.img.0000",
                                "/data/gsi/$prefix$img",
                                "/data/gsi/$prefix$img.img",
                            )
                            val realPath = candidatePaths.firstOrNull {
                                Shell.cmd("[ -e \"$it\" ] && echo 1").exec().out.firstOrNull() == "1"
                            } ?: "/data/gsi/$prefix$img"

                            val sizeCmd = if (mappedDev.isNotEmpty()) {
                                "blockdev --getsize64 \"$mappedDev\" 2>/dev/null"
                            } else {
                                "stat -c %s \"$realPath\" 2>/dev/null || blockdev --getsize64 \"$realPath\" 2>/dev/null"
                            }
                            val sizeBytes = Shell.cmd(sizeCmd).exec().out.firstOrNull()?.trim()?.toLongOrNull() ?: 0L

                            val mountEntry = activeMounts[cleanName]
                            partitionsList.add(
                                PartitionInfo(
                                    name = cleanName,
                                    path = realPath,
                                    sizeBytes = sizeBytes,
                                    isMounted = mountEntry != null,
                                    mountPoint = mountEntry?.mountPoint ?: "$MOUNT_BASE_PATH/$cleanName",
                                    filesystem = mountEntry?.filesystem ?: "",
                                    isReadOnly = mountEntry?.isReadOnly ?: true,
                                    dsuPrefix = prefix,
                                    dsuImageName = img,
                                    mappedDevice = mappedDev,
                                ),
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.w("PartitionsVM", "gsid partition discovery failed: ${e.message}")
                }
            }

            // 3. Fallback discovery via filesystem scan & mapper device inspection
            val scanCommand = "ls -1 /dev/block/mapper/dsu* /dev/block/mapper/gsi* 2>/dev/null; " +
                "find /data/gsi /mnt/media_rw/*/gsi /storage/*/*/gsi \\( -type f -o -type l \\) 2>/dev/null"
            val scanResult = Shell.cmd(scanCommand).exec()

            for (rawPath in scanResult.out) {
                val path = rawPath.trim()
                if (path.isEmpty()) continue

                val filename = path.substringAfterLast("/")

                // Filter out metadata, control files, and non-image files
                if (filename == "active" || filename == "booted" || filename.contains("metadata") ||
                    filename.startsWith(".") || filename.endsWith(".json") || filename.endsWith(".txt")
                ) {
                    continue
                }

                // Strip trailing numeric extent suffix like .0000, .0001 (used by libfiemap/ImageManager)
                val baseFilename = filename.replace(Regex("\\.\\d{4,}$"), "")

                // If path is a regular file or symlink, only consider valid image files
                if (!path.startsWith("/dev/block/") && !baseFilename.endsWith(".img") && !baseFilename.endsWith("_gsi")) {
                    continue
                }

                val name = baseFilename
                    .removeSuffix(".img")
                    .removeSuffix("_gsi")
                    .removePrefix("dsu-")
                    .removePrefix("gsi_")

                if (name.isEmpty() || seenNames.contains(name)) continue

                // Resolve realpath if it's a symlink or extent backing file (e.g. odm_gsi.img -> odm_gsi.img.0000)
                var resolvedPath = path
                if (!path.startsWith("/dev/block/")) {
                    val realPathOut = Shell.cmd("realpath \"$path\" 2>/dev/null || readlink -f \"$path\" 2>/dev/null")
                        .exec().out.firstOrNull()?.trim() ?: ""
                    if (realPathOut.isNotEmpty()) {
                        resolvedPath = realPathOut
                    }
                }

                seenNames.add(name)

                // Query size (properly handling block devices vs regular files)
                val sizeCmd = if (resolvedPath.startsWith("/dev/block/")) {
                    "blockdev --getsize64 \"$resolvedPath\" 2>/dev/null"
                } else {
                    "stat -c %s \"$resolvedPath\" 2>/dev/null || blockdev --getsize64 \"$resolvedPath\" 2>/dev/null"
                }
                val sizeOut = Shell.cmd(sizeCmd).exec().out.firstOrNull()?.trim() ?: "0"
                val sizeBytes = sizeOut.toLongOrNull() ?: 0L

                val mountEntry = activeMounts[name]
                partitionsList.add(
                    PartitionInfo(
                        name = name,
                        path = resolvedPath,
                        sizeBytes = sizeBytes,
                        isMounted = mountEntry != null,
                        mountPoint = mountEntry?.mountPoint ?: "$MOUNT_BASE_PATH/$name",
                        filesystem = mountEntry?.filesystem ?: "",
                        isReadOnly = mountEntry?.isReadOnly ?: true,
                    ),
                )
            }

            // 3. Include any other active mounts under MOUNT_BASE_PATH not found in scan
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

            // Check kernel loop device support (CONFIG_BLK_DEV_LOOP)
            val loopCheckCmd = "grep -q 'loop' /proc/devices || [ -e /dev/loop-control ] || " +
                "[ -e /dev/block/loop-control ] || ls /dev/block/loop* >/dev/null 2>&1 || ls /dev/loop* >/dev/null 2>&1"
            val hasLoop = Shell.cmd(loopCheckCmd).exec().isSuccess

            _uiState.update {
                it.copy(
                    isLoading = false,
                    partitions = partitionsList.sortedBy { p -> p.name },
                    hasLoopSupport = hasLoop,
                )
            }
        }
    }

    private fun buildMountCommand(source: String, target: String, readOnly: Boolean): String {
        val opt = if (readOnly) "ro,noatime" else "rw,noatime"
        return """
            chmod 755 /data/local 2>/dev/null
            mkdir -p "$target" && chmod 777 "$target"
            chcon u:object_r:system_data_file:s0 "$target" 2>/dev/null

            # Relabel to media_rw_data_file temporarily to prevent kernel SELinux denial in Enforcing
            if [ -f "$source" ] && echo "$source" | grep -q '/data/gsi'; then
                chcon u:object_r:media_rw_data_file:s0 "$source" 2>/dev/null
            fi

            # Ensure loop-control exists in both /dev and /dev/block
            if [ -e /dev/block/loop-control ] && [ ! -e /dev/loop-control ]; then
                ln -sf /dev/block/loop-control /dev/loop-control 2>/dev/null
            elif [ -e /dev/loop-control ] && [ ! -e /dev/block/loop-control ]; then
                ln -sf /dev/loop-control /dev/block/loop-control 2>/dev/null
            elif [ ! -e /dev/loop-control ] && [ ! -e /dev/block/loop-control ]; then
                mknod /dev/loop-control c 10 237 2>/dev/null
                ln -sf /dev/loop-control /dev/block/loop-control 2>/dev/null
            fi

            # Ensure loop block device nodes exist
            for i in ${'$'}(seq 0 63); do
                if [ -b "/dev/block/loop${'$'}i" ] && [ ! -e "/dev/loop${'$'}i" ]; then
                    ln -sf "/dev/block/loop${'$'}i" "/dev/loop${'$'}i" 2>/dev/null
                elif [ -b "/dev/loop${'$'}i" ] && [ ! -e "/dev/block/loop${'$'}i" ]; then
                    ln -sf "/dev/loop${'$'}i" "/dev/block/loop${'$'}i" 2>/dev/null
                elif [ ! -e "/dev/block/loop${'$'}i" ] && [ ! -e "/dev/loop${'$'}i" ]; then
                    mknod "/dev/block/loop${'$'}i" b 7 "${'$'}i" 2>/dev/null
                    ln -sf "/dev/block/loop${'$'}i" "/dev/loop${'$'}i" 2>/dev/null
                fi
            done

            DEV=""
            if [ -b "$source" ]; then
                DEV="$source"
            else
                # 1. Check if source already attached to a loop device
                RAW_J=${'$'}(losetup -j "$source" 2>/dev/null)
                LDEV=${'$'}(echo "${'$'}RAW_J" | grep -oE '/dev/(block/)?loop[0-9]+' | head -n1)

                # 2. If not, find next unused loop device
                if [ -z "${'$'}LDEV" ]; then
                    RAW_FREE=${'$'}(losetup -f 2>/dev/null)
                    LDEV=${'$'}(echo "${'$'}RAW_FREE" | grep -oE '/dev/(block/)?loop[0-9]+' | head -n1)
                fi

                if [ -z "${'$'}LDEV" ]; then
                    for i in ${'$'}(seq 0 63); do
                        for d in "/dev/block/loop${'$'}i" "/dev/loop${'$'}i"; do
                            [ -b "${'$'}d" ] || continue
                            if ! losetup "${'$'}d" >/dev/null 2>&1; then
                                LDEV="${'$'}d"
                                break 2
                            fi
                        done
                    done
                fi

                # 3. Associate with loop device
                if [ -n "${'$'}LDEV" ]; then
                    if ! losetup "${'$'}LDEV" 2>/dev/null | grep -q "$source"; then
                        losetup ${if (readOnly) "-r" else ""} "${'$'}LDEV" "$source" 2>/dev/null || \
                        losetup "${'$'}LDEV" "$source" 2>/dev/null
                    fi
                    DEV="${'$'}LDEV"
                else
                    DEV="$source"
                fi
            fi

            # Clean and sanitize DEV: ensure it is strictly a device path without extra info
            CLEAN_DEV=${'$'}(echo "${'$'}DEV" | grep -oE '/dev/[^ :]+' | head -n1)
            [ -n "${'$'}CLEAN_DEV" ] && DEV="${'$'}CLEAN_DEV"

            # Detect filesystem type via blkid if available
            FSTYPE=${'$'}(blkid "${'$'}DEV" 2>/dev/null | grep -o 'TYPE="[^"]*"' | cut -d'"' -f2)

            MOUNTED=0
            MOUNT_ERR=""

            # Try mounting with detected or known filesystems
            if [ -n "${'$'}FSTYPE" ]; then
                if [ "${'$'}FSTYPE" = "erofs" ]; then
                    MOUNT_ERR=${'$'}(mount -t erofs -o ro,noatime "${'$'}DEV" "$target" 2>&1) && MOUNTED=1
                else
                    MOUNT_ERR=${'$'}(mount -t "${'$'}FSTYPE" -o $opt "${'$'}DEV" "$target" 2>&1) && MOUNTED=1
                    [ "${'$'}MOUNTED" -eq 0 ] && MOUNT_ERR=${'$'}(mount -t "${'$'}FSTYPE" -o ro,noatime "${'$'}DEV" "$target" 2>&1) && MOUNTED=1
                fi
            fi

            if [ "${'$'}MOUNTED" -eq 0 ]; then
                mount -t erofs -o ro,noatime "${'$'}DEV" "$target" 2>/dev/null && MOUNTED=1 || \
                mount -t ext4 -o $opt "${'$'}DEV" "$target" 2>/dev/null && MOUNTED=1 || \
                mount -t ext4 -o ro,noatime "${'$'}DEV" "$target" 2>/dev/null && MOUNTED=1 || \
                mount -t f2fs -o $opt "${'$'}DEV" "$target" 2>/dev/null && MOUNTED=1 || \
                mount -t f2fs -o ro,noatime "${'$'}DEV" "$target" 2>/dev/null && MOUNTED=1 || \
                mount -o $opt "${'$'}DEV" "$target" 2>/dev/null && MOUNTED=1 || \
                mount -o ro,noatime "${'$'}DEV" "$target" 2>/dev/null && MOUNTED=1 || \
                mount -o $opt,context=u:object_r:system_file:s0 "${'$'}DEV" "$target" 2>/dev/null && MOUNTED=1 || \
                mount -o ro,noatime,context=u:object_r:system_file:s0 "${'$'}DEV" "$target" 2>/dev/null && MOUNTED=1 || \
                mount "${'$'}DEV" "$target" 2>/dev/null && MOUNTED=1 || \
                { MOUNT_ERR=${'$'}(mount -o $opt "${'$'}DEV" "$target" 2>&1); }
            fi

            if grep -q " $target " /proc/mounts; then
                exit 0
            else
                [ -n "${'$'}MOUNT_ERR" ] && echo "${'$'}MOUNT_ERR" >&2
                echo "Failed to mount ${'$'}DEV to $target (fstype: ${'$'}{FSTYPE:-unknown})" >&2
                exit 1
            fi
        """.trimIndent()
    }

    private fun buildUnmountCommand(target: String, source: String): String {
        return """
            LDEV=${'$'}(grep " $target " /proc/mounts 2>/dev/null | awk '{print ${'$'}1}' | head -n1)
            CLEAN_LDEV=${'$'}(echo "${'$'}LDEV" | grep -oE '/dev/(block/)?loop[0-9]+' | head -n1)

            umount "$target" 2>/dev/null || \
            umount -l "$target" 2>/dev/null || \
            umount -f "$target" 2>/dev/null

            if [ -n "${'$'}CLEAN_LDEV" ]; then
                losetup -d "${'$'}CLEAN_LDEV" 2>/dev/null
            fi
            if [ -n "$source" ] && [ -f "$source" ]; then
                RAW_J=${'$'}(losetup -j "$source" 2>/dev/null)
                for d in ${'$'}(echo "${'$'}RAW_J" | grep -oE '/dev/(block/)?loop[0-9]+'); do
                    losetup -d "${'$'}d" 2>/dev/null
                done
                if echo "$source" | grep -q '/data/gsi'; then
                    chcon u:object_r:gsi_data_file:s0 "$source" 2>/dev/null
                fi
            fi
            rmdir "$target" 2>/dev/null

            if ! grep -q " $target " /proc/mounts; then
                exit 0
            else
                exit 1
            fi
        """.trimIndent()
    }

    fun mountPartition(partition: PartitionInfo, readOnly: Boolean = true) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, errorMessage = "") }
            val target = "$MOUNT_BASE_PATH/${partition.name}"
            val opt = if (readOnly) "ro,nodev,noatime" else "rw,nodev,noatime"

            var mounted = false

            // 1. Official DSU method via gsid (IImageService.mapImageDevice)
            if (PrivilegedProvider.isConnected() && partition.dsuPrefix.isNotEmpty()) {
                try {
                    val service = PrivilegedProvider.getService()
                    val prefix = partition.dsuPrefix
                    val imageName = partition.dsuImageName.ifEmpty { "${partition.name}_gsi" }
                    val mapped = service.mapDsuImage(prefix, imageName)
                    if (!mapped.isNullOrEmpty()) {
                        val mountCmd = """
                            chmod 755 /data/local 2>/dev/null
                            mkdir -p "$target" && chmod 777 "$target"
                            chcon u:object_r:system_data_file:s0 "$target" 2>/dev/null

                            # Check if filesystem exists on mapped block device
                            FSTYPE=${'$'}(blkid "$mapped" 2>/dev/null | grep -o 'TYPE="[^"]*"' | cut -d'"' -f2)

                            if [ -z "${'$'}FSTYPE" ] && [ "${partition.name}" = "userdata" ]; then
                                echo "Partisi userdata belum dapat di-mount karena belum pernah di-boot oleh GSI (Android butuh first-boot initialization & metadata encryption)." >&2
                                exit 2
                            fi

                            mount -t erofs -o ro,nodev,noatime "$mapped" "$target" 2>/dev/null || \
                            mount -t ext4 -o $opt "$mapped" "$target" 2>/dev/null || \
                            mount -t f2fs -o $opt "$mapped" "$target" 2>/dev/null || \
                            mount -o $opt "$mapped" "$target" 2>/dev/null || \
                            mount "$mapped" "$target"
                        """.trimIndent()
                        Shell.cmd(mountCmd).exec()
                        if (Shell.cmd("grep -q ' $target ' /proc/mounts").exec().isSuccess) {
                            mounted = true
                        } else {
                            service.unmapDsuImage(prefix, imageName)
                        }
                    }
                } catch (e: Exception) {
                    Log.w("PartitionsVM", "gsid mapImageDevice failed: ${e.message}")
                }
            }

            // 2. Fallback: Loop device mount (with temporary SELinux context handling)
            if (!mounted) {
                val cmd = buildMountCommand(partition.path, target, readOnly)
                val result = Shell.cmd(cmd).exec()
                if (!result.isSuccess) {
                    var err = (result.err + result.out).filter { it.isNotBlank() }.joinToString("\n")
                    if (!_uiState.value.hasLoopSupport && !partition.path.startsWith("/dev/block/")) {
                        err += (if (err.isNotEmpty()) "\n\n" else "") +
                            "Kernel lacks loop device support (CONFIG_BLK_DEV_LOOP). Cannot mount raw image files."
                    }
                    _uiState.update { it.copy(errorMessage = err.ifEmpty { "Failed to mount ${partition.name}" }) }
                }
            }
            scanPartitions()
        }
    }

    fun unmountPartition(partition: PartitionInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, errorMessage = "") }
            val target = partition.mountPoint.ifEmpty { "$MOUNT_BASE_PATH/${partition.name}" }

            // 1. Unmount filesystem
            Shell.cmd(
                """
                umount "$target" 2>/dev/null || \
                umount -l "$target" 2>/dev/null || \
                umount -f "$target" 2>/dev/null
                rmdir "$target" 2>/dev/null
                """.trimIndent(),
            ).exec()

            // 2. Unmap via gsid if it was mapped via gsid
            if (PrivilegedProvider.isConnected() && partition.dsuPrefix.isNotEmpty()) {
                try {
                    val service = PrivilegedProvider.getService()
                    val prefix = partition.dsuPrefix
                    val imageName = partition.dsuImageName.ifEmpty { "${partition.name}_gsi" }
                    service.unmapDsuImage(prefix, imageName)
                } catch (e: Exception) {
                    Log.w("PartitionsVM", "gsid unmapImageDevice failed: ${e.message}")
                }
            }

            // 3. Fallback unmount cleanup (loop detach & restore SELinux context)
            val cmd = buildUnmountCommand(target, partition.path)
            val result = Shell.cmd(cmd).exec()
            if (!result.isSuccess && Shell.cmd("grep -q ' $target ' /proc/mounts").exec().isSuccess) {
                val err = (result.err + result.out).filter { it.isNotBlank() }.joinToString("\n")
                    .ifEmpty { "Failed to unmount ${partition.name}" }
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
                mountPartition(p, readOnly)
            }
            scanPartitions()
        }
    }

    fun unmountAll() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }
            val mounted = _uiState.value.partitions.filter { it.isMounted }
            for (p in mounted) {
                unmountPartition(p)
            }
            scanPartitions()
        }
    }

    fun promptOverridePartition(partition: PartitionInfo) {
        _uiState.update { it.copy(partitionToOverride = partition) }
    }

    fun dismissOverridePrompt() {
        _uiState.update { it.copy(partitionToOverride = null) }
    }

    fun overridePartition(partition: PartitionInfo, sourceUri: android.net.Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, partitionToOverride = null, errorMessage = "") }

            // 1. If currently mounted, unmount first
            if (partition.isMounted) {
                val target = partition.mountPoint.ifEmpty { "$MOUNT_BASE_PATH/${partition.name}" }
                val unmountCmd = buildUnmountCommand(target, partition.path)
                Shell.cmd(unmountCmd).exec()
            }

            // 2. Stream replacement image to a cache file
            val tempFile = java.io.File(application.cacheDir, "override_temp_${partition.name}.img")
            try {
                application.contentResolver.openInputStream(sourceUri)?.use { input ->
                    java.io.FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to read replacement image: ${e.message}",
                    )
                }
                tempFile.delete()
                scanPartitions()
                return@launch
            }

            val tempPath = tempFile.absolutePath

            // 3. Write over target partition path
            val overrideCmd = if (partition.path.startsWith("/dev/block/")) {
                "dd if=\"$tempPath\" of=\"${partition.path}\" bs=4M conv=fsync 2>&1"
            } else {
                "cp -f \"$tempPath\" \"${partition.path}\" 2>&1 && chmod 644 \"${partition.path}\""
            }

            val result = Shell.cmd(overrideCmd).exec()
            tempFile.delete()

            if (!result.isSuccess) {
                val err = (result.err + result.out).filter { it.isNotBlank() }.joinToString("\n")
                    .ifEmpty { "Failed to override partition ${partition.name}" }
                _uiState.update { it.copy(errorMessage = err) }
            } else {
                _uiState.update { it.copy(successMessage = "Partition ${partition.name} successfully overridden") }
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
                val cmd = buildUnmountCommand(target, partition.path)
                Shell.cmd(cmd).exec()
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

    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = "") }
    }
}
