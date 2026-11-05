package vegabobo.dsusideloader.ui.screen.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeviceHub
import androidx.compose.material.icons.outlined.InstallMobile
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import kotlin.system.exitProcess
import kotlinx.coroutines.flow.collectLatest
import vegabobo.dsusideloader.R
import vegabobo.dsusideloader.ui.cards.DsuInfoCard
import vegabobo.dsusideloader.ui.cards.installation.InstallationCard
import vegabobo.dsusideloader.ui.cards.warnings.GrantingPermissionCard
import vegabobo.dsusideloader.ui.cards.warnings.RequiresLogPermissionCard
import vegabobo.dsusideloader.ui.cards.warnings.SetupStorage
import vegabobo.dsusideloader.ui.cards.warnings.StorageWarningCard
import vegabobo.dsusideloader.ui.cards.warnings.UnlockedBootloaderCard
import vegabobo.dsusideloader.ui.cards.warnings.UnsupportedCard
import vegabobo.dsusideloader.ui.components.AppBottomBar
import vegabobo.dsusideloader.ui.components.ApplicationScreen
import vegabobo.dsusideloader.ui.components.SimpleCard
import vegabobo.dsusideloader.ui.components.Title
import vegabobo.dsusideloader.ui.components.TopBar
import vegabobo.dsusideloader.ui.screen.Destinations
import vegabobo.dsusideloader.ui.sdialogs.CancelSheet
import vegabobo.dsusideloader.ui.sdialogs.ConfirmInstallationSheet
import vegabobo.dsusideloader.ui.sdialogs.DiscardDSUSheet
import vegabobo.dsusideloader.ui.sdialogs.ViewLogsBottomSheet
import vegabobo.dsusideloader.ui.util.KeepScreenOn
import vegabobo.dsusideloader.util.DevicePropUtils
import vegabobo.dsusideloader.util.collectAsStateWithLifecycle

object HomeLinks {
    const val DSU_LEARN_MORE = "https://developer.android.com/topic/dsu"
    const val DSU_DOCS = "https://source.android.com/devices/tech/ota/dynamic-system-updates"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Home(
    navigate: (String) -> Unit,
    homeViewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { _ -> }

    if (uiState.shouldKeepScreenOn) {
        KeepScreenOn()
    }

    LaunchedEffect(Unit) {
        homeViewModel.setupUserPreferences()
        homeViewModel.session.operationMode.collectLatest {
            homeViewModel.initialChecks()
        }
    }

    ApplicationScreen(
        modifier = Modifier.padding(start = 12.dp, end = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        topBar = {
            TopBar(
                barTitle = stringResource(id = R.string.app_name),
                scrollBehavior = it,
            )
        },
        bottomBar = {
            AppBottomBar(
                currentDestination = Destinations.Homepage,
                onNavigate = navigate,
            )
        },
        content = {
            Box(modifier = Modifier.animateContentSize()) {
                when (uiState.additionalCard) {
                    AdditionalCardState.NO_DYNAMIC_PARTITIONS ->
                        UnsupportedCard(
                            onClickClose = { exitProcess(0) },
                            onClickContinueAnyway = { homeViewModel.overrideDynamicPartitionCheck() },
                        )

                    AdditionalCardState.SETUP_STORAGE ->
                        SetupStorage { homeViewModel.takeUriPermission(it) }

                    AdditionalCardState.UNAVAIABLE_STORAGE ->
                        StorageWarningCard(
                            minPercentageFreeStorage = homeViewModel.allocPercentageInt.toString(),
                            onClick = { homeViewModel.overrideUnavaiableStorage() },
                        )

                    AdditionalCardState.MISSING_READ_LOGS_PERMISSION ->
                        RequiresLogPermissionCard(
                            onClickGrant = { homeViewModel.grantReadLogs() },
                            onClickRefuse = { homeViewModel.refuseReadLogs() },
                        )

                    AdditionalCardState.GRANTING_READ_LOGS_PERMISSION ->
                        GrantingPermissionCard()

                    AdditionalCardState.BOOTLOADER_UNLOCKED_WARNING ->
                        UnlockedBootloaderCard { homeViewModel.onClickBootloaderUnlockedWarning() }

                    AdditionalCardState.NONE -> {}
                }
            }

            if (uiState.passedInitialChecks && uiState.additionalCard == AdditionalCardState.NONE) {
                // If installation is active or DSU installed, show direct card
                if (uiState.isInstalling() || uiState.isDsuInstalled) {
                    InstallationCard(
                        uiState = uiState.installationCard,
                        onClickInstall = { homeViewModel.onClickInstall() },
                        onClickUnmountSdCardAndRetry = { homeViewModel.onClickUnmountSdCardAndRetry() },
                        onClickSetSeLinuxPermissive = { homeViewModel.onClickSetSeLinuxPermissive() },
                        onClickRetryInstallation = { homeViewModel.onClickRetryInstallation() },
                        onClickClear = { homeViewModel.resetInstallationCard() },
                        onSelectFilesSuccess = { homeViewModel.onFilesSelectionResult(it) },
                        onClickCancelInstallation = { homeViewModel.onClickCancel() },
                        onClickDiscardInstalledGsiAndInstall = { homeViewModel.onClickDiscardGsiAndStartInstallation() },
                        onClickDiscardDsu = { homeViewModel.showDiscardSheet() },
                        onClickRebootToDynOS = { homeViewModel.onClickRebootToDynOS() },
                        onClickViewLogs = { homeViewModel.showLogsWarning() },
                        onClickViewCommands = { navigate(Destinations.ADBInstallation) },
                        minPercentageOfFreeStorage = homeViewModel.allocPercentageInt.toString(),
                    )
                }

                // Hero Action Card: Install GSI
                HeroInstallCard(
                    onStartWizard = { navigate(Destinations.Install) },
                )

                // Quick Actions Section
                Title(
                    title = stringResource(id = R.string.quick_actions),
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    QuickActionPill(
                        icon = Icons.Outlined.RocketLaunch,
                        label = stringResource(id = R.string.action_install_gsi),
                        onClick = { navigate(Destinations.Install) },
                        modifier = Modifier.weight(1F),
                    )
                    QuickActionPill(
                        icon = Icons.Outlined.Layers,
                        label = stringResource(id = R.string.action_manage_systems),
                        onClick = { navigate(Destinations.Systems) },
                        modifier = Modifier.weight(1F),
                    )
                    QuickActionPill(
                        icon = Icons.Outlined.Terminal,
                        label = stringResource(id = R.string.action_diagnostics),
                        onClick = { homeViewModel.showLogsWarning() },
                        modifier = Modifier.weight(1F),
                    )
                }

                // System & DSU Status Card
                Title(
                    title = stringResource(id = R.string.system_status),
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                )
                SystemStatusCard(
                    operationMode = homeViewModel.session.getOperationMode().name,
                    allocPercentageInt = homeViewModel.allocPercentageInt,
                    isDsuInstalled = uiState.isDsuInstalled,
                )

                // Device Specs Card
                Title(
                    title = stringResource(id = R.string.device_specs),
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                )
                DeviceSpecsCard()

                // What is DSU Informational Card
                DsuInfoCard(
                    onClickViewDocs = { uriHandler.openUri(HomeLinks.DSU_DOCS) },
                    onClickLearnMore = { uriHandler.openUri(HomeLinks.DSU_LEARN_MORE) },
                )
            }
        },
    )

    when (uiState.sheetDisplay) {
        SheetDisplayState.CONFIRM_INSTALLATION ->
            ConfirmInstallationSheet(
                filename = homeViewModel.obtainSelectedFilename(),
                userdata = homeViewModel.session.userSelection.getUserDataSizeAsGB(),
                onClickConfirm = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS,
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                    homeViewModel.onConfirmInstallationSheet()
                },
                onClickCancel = { homeViewModel.dismissSheet() },
            )

        SheetDisplayState.CANCEL_INSTALLATION ->
            CancelSheet(
                onClickConfirm = { homeViewModel.onClickCancelInstallationButton() },
                onClickCancel = { homeViewModel.dismissSheet() },
            )

        SheetDisplayState.DISCARD_DSU ->
            DiscardDSUSheet(
                onClickConfirm = { homeViewModel.onClickDiscardGsi() },
                onClickCancel = { homeViewModel.dismissSheet() },
            )

        SheetDisplayState.VIEW_LOGS ->
            ViewLogsBottomSheet(
                logs = uiState.installationLogs,
                onClickSaveLogs = { homeViewModel.saveLogs(it) },
                onDismiss = { homeViewModel.dismissSheet() },
            )

        SheetDisplayState.NONE -> {}
    }
}

@Composable
private fun HeroInstallCard(
    onStartWizard: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onStartWizard),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.InstallMobile,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(28.dp),
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1F)) {
                Text(
                    text = stringResource(id = R.string.action_install_gsi),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(id = R.string.action_install_gsi_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85F),
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun QuickActionPill(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SystemStatusCard(
    operationMode: String,
    allocPercentageInt: Int,
    isDsuInstalled: Boolean,
) {
    SimpleCard(addPadding = false) {
        Column(modifier = Modifier.padding(14.dp)) {
            StatusRow(
                icon = Icons.Outlined.DeviceHub,
                label = stringResource(R.string.dynamic_partitions),
                value = if (DevicePropUtils.hasDynamicPartitions()) stringResource(R.string.supported) else stringResource(R.string.unsupported),
                isPositive = DevicePropUtils.hasDynamicPartitions(),
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            StatusRow(
                icon = Icons.Outlined.Security,
                label = stringResource(R.string.operation_mode),
                value = operationMode,
                isPositive = true,
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            StatusRow(
                icon = Icons.Outlined.Storage,
                label = "DSU State",
                value = if (isDsuInstalled) "GSI Present" else "Ready",
                isPositive = true,
            )
        }
    }
}

@Composable
private fun StatusRow(
    icon: ImageVector,
    label: String,
    value: String,
    isPositive: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isPositive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isPositive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun DeviceSpecsCard() {
    SimpleCard(addPadding = false) {
        Column(modifier = Modifier.padding(14.dp)) {
            SpecRow(
                icon = Icons.Outlined.PhoneAndroid,
                label = stringResource(R.string.device_model),
                value = Build.MODEL,
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SpecRow(
                icon = Icons.Outlined.CheckCircle,
                label = stringResource(R.string.android_version),
                value = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SpecRow(
                icon = Icons.Outlined.Storage,
                label = stringResource(R.string.architecture),
                value = Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown",
            )
        }
    }
}

@Composable
private fun SpecRow(
    icon: ImageVector,
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
