package vegabobo.dsusideloader.ui.screen.partitions

import android.widget.Toast
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
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import vegabobo.dsusideloader.R
import vegabobo.dsusideloader.ui.components.AppBottomBar
import vegabobo.dsusideloader.ui.components.ApplicationScreen
import vegabobo.dsusideloader.ui.components.Dialog
import vegabobo.dsusideloader.ui.components.SimpleCard
import vegabobo.dsusideloader.ui.components.Title
import vegabobo.dsusideloader.ui.components.TopBar
import vegabobo.dsusideloader.ui.components.buttons.ActionButton
import vegabobo.dsusideloader.ui.components.buttons.ErrorButton
import vegabobo.dsusideloader.ui.components.buttons.PrimaryButton
import vegabobo.dsusideloader.ui.components.buttons.SecondaryButton
import vegabobo.dsusideloader.ui.screen.Destinations
import vegabobo.dsusideloader.util.collectAsStateWithLifecycle

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
                showPartitions = true,
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

        // Overview / Controls Card
        SimpleCard {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = R.string.partitions_manager),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = stringResource(id = R.string.mount_output_path),
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

                Spacer(modifier = Modifier.height(12.dp))

                val mountedCount = uiState.partitions.count { it.isMounted }
                Text(
                    text = "$mountedCount / ${uiState.partitions.size} ${stringResource(id = R.string.mounted).lowercase()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PrimaryButton(
                        text = stringResource(id = R.string.mount_all),
                        onClick = { viewModel.mountAll() },
                        isEnabled = uiState.partitions.any { !it.isMounted },
                    )
                    SecondaryButton(
                        text = stringResource(id = R.string.unmount_all),
                        onClick = { viewModel.unmountAll() },
                        isEnabled = uiState.partitions.any { it.isMounted },
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
                                text = "${partition.formattedSize} • ${partition.path}",
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
                                color = if (partition.isMounted) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }

                    if (partition.isMounted) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                .clickable {
                                    clipboardManager.setText(AnnotatedString(partition.mountPoint))
                                    Toast
                                        .makeText(
                                            context,
                                            context.getString(R.string.mount_path_copied),
                                            Toast.LENGTH_SHORT,
                                        )
                                        .show()
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = partition.mountPoint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = stringResource(id = R.string.copy_mount_path),
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        if (partition.isMounted) {
                            SecondaryButton(
                                text = stringResource(id = R.string.unmount),
                                onClick = { viewModel.unmountPartition(partition) },
                            )
                        } else {
                            ActionButton(
                                text = stringResource(id = R.string.mount_ro),
                                onClick = { viewModel.mountPartition(partition, readOnly = true) },
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            ActionButton(
                                text = stringResource(id = R.string.mount_rw),
                                onClick = { viewModel.mountPartition(partition, readOnly = false) },
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            ErrorButton(
                                text = stringResource(id = R.string.discard),
                                onClick = { viewModel.promptDeletePartition(partition) },
                            )
                        }
                    }
                }
            }
        }

        // Custom mount option
        Title(title = stringResource(id = R.string.mount_custom_image))
        SimpleCard {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(id = R.string.mount_custom_image),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(id = R.string.mount_custom_image_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                PrimaryButton(
                    text = stringResource(id = R.string.mount_custom_image),
                    onClick = { viewModel.setShowCustomMountDialog(true) },
                )
            }
        }
    }

    // Custom mount dialog
    if (uiState.showCustomMountDialog) {
        var customPath by remember { mutableStateOf("") }
        var customName by remember { mutableStateOf("") }
        var readOnly by remember { mutableStateOf(true) }

        Dialog(
            icon = Icons.Outlined.FolderOpen,
            title = stringResource(id = R.string.mount_custom_image),
            confirmText = stringResource(id = R.string.mount),
            cancelText = stringResource(id = R.string.cancel),
            onClickConfirm = {
                viewModel.mountCustomImage(customPath, customName, readOnly)
            },
            onClickCancel = { viewModel.setShowCustomMountDialog(false) },
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = customPath,
                    onValueChange = { customPath = it },
                    label = { Text(stringResource(id = R.string.image_path)) },
                    placeholder = { Text("/data/gsi/dsu/system_gsi.img") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = customName,
                    onValueChange = { customName = it },
                    label = { Text(stringResource(id = R.string.partition_name)) },
                    placeholder = { Text("system") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                )
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
