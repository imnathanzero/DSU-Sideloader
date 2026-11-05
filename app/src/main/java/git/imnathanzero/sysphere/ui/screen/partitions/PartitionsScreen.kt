package git.imnathanzero.sysphere.ui.screen.partitions

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import git.imnathanzero.sysphere.R
import git.imnathanzero.sysphere.ui.components.AppBottomBar
import git.imnathanzero.sysphere.ui.components.ApplicationScreen
import git.imnathanzero.sysphere.ui.components.Dialog
import git.imnathanzero.sysphere.ui.components.SimpleCard
import git.imnathanzero.sysphere.ui.components.Title
import git.imnathanzero.sysphere.ui.components.TopBar
import git.imnathanzero.sysphere.ui.components.buttons.PrimaryButton
import git.imnathanzero.sysphere.ui.components.buttons.SecondaryButton
import git.imnathanzero.sysphere.ui.screen.Destinations
import git.imnathanzero.sysphere.util.FilenameUtils
import git.imnathanzero.sysphere.util.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartitionsScreen(
    navigate: (String) -> Unit,
    viewModel: PartitionsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    ApplicationScreen(
        modifier = Modifier.padding(start = 10.dp, end = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        topBar = {
            TopBar(
                barTitle = stringResource(id = R.string.partitions_manager),
                scrollBehavior = it,
            )
        },
        bottomBar = {
            AppBottomBar(
                currentDestination = Destinations.Partitions,
                onNavigate = navigate,
            )
        },
    ) {
        if (!uiState.isRoot) {
            SimpleCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.WarningAmber,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(36.dp),
                    )
                    Spacer(modifier = Modifier.size(16.dp))
                    Text(
                        text = stringResource(id = R.string.requires_root_partitions),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            return@ApplicationScreen
        }

        if (!uiState.hasLoopSupport) {
            SimpleCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.WarningAmber,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp),
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                    Text(
                        text = stringResource(id = R.string.kernel_no_loop_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        // Overview / Mount Target Card
        SimpleCard {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }

                    Spacer(modifier = Modifier.size(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = R.string.mount_target),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = PartitionsViewModel.MOUNT_BASE_PATH,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    IconButton(onClick = { viewModel.scanPartitions() }) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = stringResource(id = R.string.refresh),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val mountedCount = uiState.partitions.count { it.isMounted }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(id = R.string.status),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "$mountedCount / ${uiState.partitions.size} ${stringResource(id = R.string.mounted).lowercase()}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (mountedCount > 0) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PrimaryButton(
                        modifier = Modifier.weight(1f),
                        text = stringResource(id = R.string.mount_all),
                        onClick = { viewModel.mountAll() },
                        isEnabled = uiState.partitions.any { !it.isMounted },
                    )
                    SecondaryButton(
                        modifier = Modifier.weight(1f),
                        text = stringResource(id = R.string.unmount_all),
                        onClick = { viewModel.unmountAll() },
                        isEnabled = uiState.partitions.any { it.isMounted },
                    )
                }
            }
        }

        if (uiState.partitions.isNotEmpty()) {
            SimpleCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.RestartAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                    Text(
                        text = stringResource(id = R.string.partitions_reboot_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Title(title = stringResource(id = R.string.partitions))

        AnimatedVisibility(visible = uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        if (uiState.partitions.isEmpty() && !uiState.isLoading) {
            SimpleCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(id = R.string.no_partitions_found),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(id = R.string.no_partitions_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        for (partition in uiState.partitions) {
            SimpleCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (partition.isMounted) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHighest
                                    },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Storage,
                                contentDescription = null,
                                tint = if (partition.isMounted) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }

                        Spacer(modifier = Modifier.size(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = partition.name,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = if (partition.formattedSize.isNotEmpty()) {
                                    "${partition.formattedSize} • ${partition.path.substringAfterLast("/")}"
                                } else {
                                    partition.path.substringAfterLast("/")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (partition.isMounted) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHighest
                                    },
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = if (partition.isMounted) {
                                    "${stringResource(id = R.string.mounted)} (${if (partition.isReadOnly) "ro" else "rw"})"
                                } else {
                                    stringResource(id = R.string.unmounted)
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = if (partition.isMounted) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = partition.path,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )

                    if (partition.isMounted) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                .clickable {
                                    clipboardManager.setText(AnnotatedString(partition.mountPoint))
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.mount_path_copied),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = partition.mountPoint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                            )
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = stringResource(id = R.string.copy_mount_path),
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (!partition.isMounted) {
                            IconButton(
                                onClick = { viewModel.promptDeletePartition(partition) },
                                modifier = Modifier.size(36.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = stringResource(id = R.string.discard),
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Spacer(modifier = Modifier.size(4.dp))
                            IconButton(
                                onClick = { viewModel.promptOverridePartition(partition) },
                                modifier = Modifier.size(36.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = stringResource(id = R.string.override_partition),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        if (partition.isMounted) {
                            SecondaryButton(
                                text = stringResource(id = R.string.unmount),
                                onClick = { viewModel.unmountPartition(partition) },
                            )
                        } else {
                            SecondaryButton(
                                text = stringResource(id = R.string.mount_rw),
                                onClick = { viewModel.mountPartition(partition, readOnly = false) },
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            PrimaryButton(
                                text = stringResource(id = R.string.mount),
                                onClick = { viewModel.mountPartition(partition, readOnly = true) },
                            )
                        }
                    }
                }
            }
        }
    }

    // Override confirmation / selection dialog
    uiState.partitionToOverride?.let { partition ->
        var selectedUri by remember { mutableStateOf<Uri?>(null) }
        var selectedFileName by remember { mutableStateOf("") }

        val chooseFileIntent = remember {
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }
        }

        val launcherFile = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            val uri = result.data?.data
            if (uri != null) {
                selectedUri = uri
                selectedFileName = FilenameUtils.queryName(context.contentResolver, uri)
            }
        }

        Dialog(
            icon = Icons.Outlined.Edit,
            title = stringResource(id = R.string.override_partition),
            text = stringResource(id = R.string.override_partition_desc, partition.name),
            confirmText = stringResource(id = R.string.override),
            cancelText = stringResource(id = R.string.cancel),
            isConfirmEnabled = selectedUri != null,
            onClickConfirm = {
                selectedUri?.let { uri ->
                    viewModel.overridePartition(partition, uri)
                }
            },
            onClickCancel = { viewModel.dismissOverridePrompt() },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .clickable { launcherFile.launch(chooseFileIntent) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FolderOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.size(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (selectedFileName.isNotEmpty()) {
                                selectedFileName
                            } else {
                                stringResource(id = R.string.select_image_file)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selectedFileName.isNotEmpty()) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedFileName.isNotEmpty()) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                        )
                        if (selectedFileName.isNotEmpty()) {
                            Text(
                                text = stringResource(id = R.string.selected_file),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    uiState.partitionToDelete?.let { partition ->
        Dialog(
            icon = Icons.Outlined.Delete,
            title = stringResource(id = R.string.delete_partition),
            text = stringResource(id = R.string.delete_partition_warn, partition.name),
            confirmText = stringResource(id = R.string.discard),
            cancelText = stringResource(id = R.string.cancel),
            onClickConfirm = { viewModel.confirmDeletePartition() },
            onClickCancel = { viewModel.dismissDeletePrompt() },
        )
    }

    // Success message dialog
    if (uiState.successMessage.isNotEmpty()) {
        Dialog(
            icon = Icons.Outlined.CheckCircle,
            title = stringResource(id = R.string.override),
            text = uiState.successMessage,
            confirmText = stringResource(id = R.string.ok),
            onClickConfirm = { viewModel.clearSuccessMessage() },
        )
    }

    // Error message dialog
    if (uiState.errorMessage.isNotEmpty()) {
        Dialog(
            icon = Icons.Outlined.WarningAmber,
            title = stringResource(id = R.string.error),
            text = uiState.errorMessage,
            confirmText = stringResource(id = R.string.ok),
            onClickConfirm = { viewModel.clearErrorMessage() },
        )
    }
}
