package git.imnathanzero.sysphere.ui.screen.systems

import android.widget.Toast
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.InstallMobile
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import git.imnathanzero.sysphere.ui.screen.home.HomeViewModel
import git.imnathanzero.sysphere.ui.screen.partitions.PartitionInfo
import git.imnathanzero.sysphere.ui.screen.partitions.PartitionsViewModel
import git.imnathanzero.sysphere.ui.sdialogs.DiscardDSUSheet
import git.imnathanzero.sysphere.util.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemsScreen(
    navigate: (String) -> Unit,
    homeViewModel: HomeViewModel = hiltViewModel(),
    partitionsViewModel: PartitionsViewModel = hiltViewModel(),
) {
    val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val partitionsUiState by partitionsViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    ApplicationScreen(
        modifier = Modifier.padding(start = 12.dp, end = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        topBar = {
            TopBar(
                barTitle = stringResource(id = R.string.systems_manager),
                scrollBehavior = it,
            )
        },
        bottomBar = {
            AppBottomBar(
                currentDestination = Destinations.Systems,
                onNavigate = navigate,
            )
        },
    ) {
        // DSU Environment Status Card
        Title(
            title = stringResource(id = R.string.dsu_environment),
            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
        )

        SimpleCard(addPadding = false) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (homeUiState.isDsuInstalled) {
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
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1F)) {
                            Text(
                                text = stringResource(id = R.string.guest_system),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = stringResource(id = R.string.dsu_installed_ready),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        PrimaryButton(
                            text = stringResource(id = R.string.reboot_into_dsu),
                            onClick = { homeViewModel.onClickRebootToDynOS() },
                            modifier = Modifier.weight(1F),
                        )
                        SecondaryButton(
                            text = stringResource(id = R.string.discard_dsu),
                            onClick = { homeViewModel.showDiscardSheet() },
                            modifier = Modifier.weight(1F),
                        )
                    }
                } else {
                    // Empty State: No DSU Environment Active
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Layers,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp),
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = stringResource(id = R.string.no_dsu_installed),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = stringResource(id = R.string.no_dsu_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        FilledTonalButton(
                            onClick = { navigate(Destinations.Install) },
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.InstallMobile,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = stringResource(id = R.string.action_install_gsi))
                        }
                    }
                }
            }
        }

        // Partition Manager Section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Title(stringResource(id = R.string.partitions_manager))
            IconButton(onClick = { partitionsViewModel.scanPartitions() }) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = stringResource(id = R.string.refresh),
                )
            }
        }

        if (partitionsUiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (partitionsUiState.partitions.isNotEmpty()) {
            partitionsUiState.partitions.forEach { partition ->
                PartitionItemCard(
                    partition = partition,
                    onMount = {
                        if (partition.isMounted) {
                            partitionsViewModel.unmountPartition(partition)
                        } else {
                            partitionsViewModel.mountPartition(partition)
                        }
                    },
                    onDelete = { partitionsViewModel.promptDeletePartition(partition) },
                    onCopyPath = {
                        clipboardManager.setText(AnnotatedString(partition.mountPoint))
                        Toast.makeText(context, context.getString(R.string.mount_path_copied), Toast.LENGTH_SHORT).show()
                    },
                )
            }
        } else {
            SimpleCard {
                Text(
                    text = stringResource(R.string.no_partitions_found),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                )
            }
        }
    }

    // Delete Partition Confirmation Dialog
    if (partitionsUiState.partitionToDelete != null) {
        val part = partitionsUiState.partitionToDelete!!
        Dialog(
            title = stringResource(id = R.string.delete_partition),
            icon = Icons.Outlined.WarningAmber,
            text = stringResource(id = R.string.delete_partition_warn, part.name),
            confirmText = stringResource(id = R.string.delete_partition),
            cancelText = stringResource(id = R.string.cancel),
            onClickConfirm = { partitionsViewModel.confirmDeletePartition() },
            onClickCancel = { partitionsViewModel.dismissDeletePrompt() },
        )
    }

    // Discard DSU Dialog
    if (homeUiState.sheetDisplay == git.imnathanzero.sysphere.ui.screen.home.SheetDisplayState.DISCARD_DSU) {
        DiscardDSUSheet(
            onClickConfirm = { homeViewModel.onClickDiscardGsi() },
            onClickCancel = { homeViewModel.dismissSheet() },
        )
    }
}

@Composable
private fun PartitionItemCard(
    partition: PartitionInfo,
    onMount: () -> Unit,
    onDelete: () -> Unit,
    onCopyPath: () -> Unit,
) {
    SimpleCard(addPadding = false) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (partition.isMounted) Icons.Outlined.FolderOpen else Icons.Outlined.Folder,
                    contentDescription = null,
                    tint = if (partition.isMounted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1F)) {
                    Text(
                        text = partition.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${partition.formattedSize} • ${if (partition.isMounted) "Mounted" else "Unmounted"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.delete_partition),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }

            if (partition.isMounted && partition.mountPoint.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .clickable(onClick = onCopyPath)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = partition.mountPoint,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1F),
                    )
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            PrimaryButton(
                text = if (partition.isMounted) stringResource(R.string.unmount) else stringResource(R.string.mount),
                onClick = onMount,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
