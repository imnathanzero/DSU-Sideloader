package git.imnathanzero.sysphere.ui.screen.install

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import git.imnathanzero.sysphere.R
import git.imnathanzero.sysphere.ui.cards.UserdataCard
import git.imnathanzero.sysphere.ui.cards.installation.InstallationCardStep
import git.imnathanzero.sysphere.ui.components.AppBottomBar
import git.imnathanzero.sysphere.ui.components.ApplicationScreen
import git.imnathanzero.sysphere.ui.components.SimpleCard
import git.imnathanzero.sysphere.ui.components.TopBar
import git.imnathanzero.sysphere.ui.components.buttons.PrimaryButton
import git.imnathanzero.sysphere.ui.screen.Destinations
import git.imnathanzero.sysphere.ui.screen.home.HomeViewModel
import git.imnathanzero.sysphere.ui.screen.home.SheetDisplayState
import git.imnathanzero.sysphere.ui.sdialogs.CancelSheet
import git.imnathanzero.sysphere.ui.sdialogs.ConfirmInstallationSheet
import git.imnathanzero.sysphere.ui.sdialogs.DiscardDSUSheet
import git.imnathanzero.sysphere.ui.sdialogs.ViewLogsBottomSheet
import git.imnathanzero.sysphere.ui.util.launcherMultipleAcResult
import git.imnathanzero.sysphere.util.OperationMode
import git.imnathanzero.sysphere.util.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallScreen(
    navigate: (String) -> Unit,
    homeViewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var currentStep by rememberSaveable { mutableIntStateOf(1) }

    // If an installation is active or completed, show Step 4 (Progress)
    val effectiveStep = if (uiState.isInstalling()) 4 else currentStep

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { _ -> }

    // File selection launcher
    var chooseFile = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        type = "*/*"
        val mimetypes = arrayOf(
            "application/gzip",
            "application/x-gzip",
            "application/x-xz",
            "application/zip",
            "application/octet-stream",
        )
        putExtra(Intent.EXTRA_MIME_TYPES, mimetypes)
        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
    }
    chooseFile = Intent.createChooser(chooseFile, "")

    val filePickerLauncher = launcherMultipleAcResult {
        homeViewModel.onFilesSelectionResult(it)
    }

    ApplicationScreen(
        modifier = Modifier.padding(start = 12.dp, end = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        topBar = {
            TopBar(
                barTitle = stringResource(id = R.string.nav_install),
                scrollBehavior = it,
            )
        },
        bottomBar = {
            AppBottomBar(
                currentDestination = Destinations.Install,
                onNavigate = navigate,
            )
        },
    ) {
        // Step Indicator Tabs
        StepIndicatorBar(
            currentStep = effectiveStep,
            onStepSelected = { if (!uiState.isInstalling()) currentStep = it },
        )

        AnimatedContent(
            targetState = effectiveStep,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "wizard_step_transition",
        ) { step ->
            when (step) {
                1 -> StepOneSelectGsi(
                    selectedFileName = homeViewModel.obtainSelectedFilename(),
                    isError = uiState.installationCard.isError,
                    onPickFile = { filePickerLauncher.launch(chooseFile) },
                    onClearFile = { homeViewModel.resetInstallationCard() },
                    onNext = { currentStep = 2 },
                )
                2 -> StepTwoConfigure(
                    uiState = uiState,
                    homeViewModel = homeViewModel,
                    onBack = { currentStep = 1 },
                    onNext = { currentStep = 3 },
                )
                3 -> StepThreeReview(
                    selectedFileName = homeViewModel.obtainSelectedFilename(),
                    userdataSize = homeViewModel.session.userSelection.getUserDataSizeAsGB(),
                    preserveUserdata = homeViewModel.session.preferences.preserveUserdata,
                    operationMode = homeViewModel.session.getOperationMode().name,
                    onBack = { currentStep = 2 },
                    onStartInstall = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            if (ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS,
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                        currentStep = 4
                        homeViewModel.onConfirmInstallationSheet()
                    },
                )
                4 -> StepFourProgress(
                    uiState = uiState,
                    homeViewModel = homeViewModel,
                    onNavigateToCommands = { navigate(Destinations.ADBInstallation) },
                    onResetWizard = { currentStep = 1 },
                )
            }
        }
    }

    // Dialog Sheets
    when (uiState.sheetDisplay) {
        SheetDisplayState.CONFIRM_INSTALLATION ->
            ConfirmInstallationSheet(
                filename = homeViewModel.obtainSelectedFilename(),
                userdata = homeViewModel.session.userSelection.getUserDataSizeAsGB(),
                onClickConfirm = { homeViewModel.onConfirmInstallationSheet() },
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
private fun StepIndicatorBar(
    currentStep: Int,
    onStepSelected: (Int) -> Unit,
) {
    val steps = listOf(
        1 to stringResource(R.string.wizard_step_select),
        2 to stringResource(R.string.wizard_step_configure),
        3 to stringResource(R.string.wizard_step_review),
        4 to stringResource(R.string.wizard_step_progress),
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        ScrollableTabRow(
            selectedTabIndex = (currentStep - 1).coerceIn(0, 3),
            edgePadding = 8.dp,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            steps.forEach { (index, title) ->
                Tab(
                    selected = currentStep == index,
                    onClick = { onStepSelected(index) },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (currentStep == index) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun StepOneSelectGsi(
    selectedFileName: String,
    isError: Boolean,
    onPickFile: () -> Unit,
    onClearFile: () -> Unit,
    onNext: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SimpleCard(addPadding = false) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CloudUpload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp),
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = stringResource(R.string.select_gsi_info),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Supported formats: .img, .gz, .xz, and DSU .zip packages",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (isError) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.WarningAmber,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.file_unsupported),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }

                if (selectedFileName.isEmpty()) {
                    PrimaryButton(
                        text = "Browse Storage",
                        onClick = onPickFile,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.InsertDriveFile,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = selectedFileName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1F),
                            )
                            IconButton(onClick = onClearFile) {
                                Icon(
                                    imageVector = Icons.Outlined.Clear,
                                    contentDescription = stringResource(R.string.clear),
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            FilledTonalButton(
                onClick = onNext,
                enabled = selectedFileName.isNotEmpty(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(text = stringResource(R.string.wizard_next))
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun StepTwoConfigure(
    uiState: git.imnathanzero.sysphere.ui.screen.home.HomeUiState,
    homeViewModel: HomeViewModel,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        UserdataCard(
            isEnabled = true,
            uiState = uiState.userDataCard,
            isDsuInstalled = uiState.isDsuInstalled,
            onValueChange = { homeViewModel.updateUserdataSize(it) },
            onCheckedChange = { homeViewModel.onCheckUserdataCard() },
            onPreserveCheckedChange = { homeViewModel.onCheckPreserveUserdata(it) },
        )

        // Operation Mode Details Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Operation Mode: ${homeViewModel.session.getOperationMode().name}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = when (homeViewModel.session.getOperationMode()) {
                            OperationMode.ROOT, OperationMode.SYSTEM_AND_ROOT ->
                                "Direct installation via native GsiService APIs (Built-in installer)"
                            OperationMode.SHIZUKU ->
                                "Privileged installation via DSU VerificationActivity with live logcat tracking"
                            OperationMode.SYSTEM ->
                                "System privileged mode"
                            OperationMode.ADB ->
                                "Script generation mode for ADB execution on PC or terminal"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = stringResource(R.string.wizard_back))
            }

            FilledTonalButton(
                onClick = onNext,
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(text = stringResource(R.string.wizard_next))
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun StepThreeReview(
    selectedFileName: String,
    userdataSize: String,
    preserveUserdata: Boolean,
    operationMode: String,
    onBack: () -> Unit,
    onStartInstall: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SimpleCard(addPadding = false) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = stringResource(R.string.proceed_installation),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.proceed_installation_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(16.dp))

                ReviewRow(
                    icon = Icons.AutoMirrored.Outlined.InsertDriveFile,
                    label = stringResource(R.string.summary_target_gsi),
                    value = selectedFileName.ifEmpty { "None selected" },
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                ReviewRow(
                    icon = Icons.Outlined.Storage,
                    label = stringResource(R.string.summary_userdata),
                    value = "${userdataSize}GB (Preserve: ${if (preserveUserdata) "Yes" else "No"})",
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                ReviewRow(
                    icon = Icons.Outlined.Settings,
                    label = stringResource(R.string.summary_operation_mode),
                    value = operationMode,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = stringResource(R.string.wizard_back))
            }

            PrimaryButton(
                text = stringResource(R.string.wizard_start_install),
                onClick = onStartInstall,
            )
        }
    }
}

@Composable
private fun ReviewRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun StepFourProgress(
    uiState: git.imnathanzero.sysphere.ui.screen.home.HomeUiState,
    homeViewModel: HomeViewModel,
    onNavigateToCommands: () -> Unit,
    onResetWizard: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    InstallationCardStep(
        uiState = uiState.installationCard,
        textFieldInteraction = interactionSource,
        onClickClear = {
            homeViewModel.resetInstallationCard()
            onResetWizard()
        },
        onClickInstall = { homeViewModel.onClickInstall() },
        onClickRetryInstallation = { homeViewModel.onClickRetryInstallation() },
        onClickUnmountSdCardAndRetry = { homeViewModel.onClickUnmountSdCardAndRetry() },
        onClickSetSeLinuxPermissive = { homeViewModel.onClickSetSeLinuxPermissive() },
        onClickCancelInstallation = { homeViewModel.onClickCancel() },
        onClickDiscardInstalledGsiAndInstall = { homeViewModel.onClickDiscardGsiAndStartInstallation() },
        onClickDiscardDsu = { homeViewModel.showDiscardSheet() },
        onClickRebootToDynOS = { homeViewModel.onClickRebootToDynOS() },
        onClickViewLogs = { homeViewModel.showLogsWarning() },
        onClickViewCommands = onNavigateToCommands,
    )
}
