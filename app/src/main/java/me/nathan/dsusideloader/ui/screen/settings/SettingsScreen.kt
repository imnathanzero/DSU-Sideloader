package me.nathan.dsusideloader.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.SdCard
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import me.nathan.dsusideloader.R
import me.nathan.dsusideloader.preferences.AppPrefs
import me.nathan.dsusideloader.ui.components.ApplicationScreen
import me.nathan.dsusideloader.ui.components.DialogLikeBottomSheet
import me.nathan.dsusideloader.ui.components.PreferenceItem
import me.nathan.dsusideloader.ui.components.Title
import me.nathan.dsusideloader.ui.components.TopBar
import me.nathan.dsusideloader.ui.screen.Destinations
import me.nathan.dsusideloader.util.OperationMode
import me.nathan.dsusideloader.util.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Settings(
    navigate: (String) -> Unit,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    isSubScreen: Boolean = false,
) {
    val uiState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        settingsViewModel.checkDevOpt()
    }

    ApplicationScreen(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        topBar = {
            TopBar(
                barTitle = stringResource(id = R.string.settings),
                scrollBehavior = it,
                onClickBackButton = if (isSubScreen) { { navigate(Destinations.Up) } } else null,
            )
        },
    ) {
        // Section: Installation
        Title(title = stringResource(id = R.string.installation))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            tonalElevation = 1.dp,
        ) {
            Column {
                PreferenceItem(
                    title = stringResource(id = R.string.builtin_installer),
                    description =
                    if (settingsViewModel.isAndroidQ()) {
                        stringResource(id = R.string.unsupported)
                    } else if (uiState.isRoot) {
                        stringResource(id = R.string.builtin_installer_description)
                    } else {
                        stringResource(R.string.requires_root)
                    },
                    icon = Icons.Outlined.FlashOn,
                    showToggle = true,
                    isEnabled = uiState.isRoot && !settingsViewModel.isAndroidQ(),
                    isChecked = uiState.preferences[AppPrefs.USE_BUILTIN_INSTALLER] ?: false,
                    onClick = {
                        if (!it) {
                            settingsViewModel.updateSheetDisplay(DialogSheetState.BUILT_IN_INSTALLER)
                        }
                        settingsViewModel.togglePreference(AppPrefs.USE_BUILTIN_INSTALLER, !it)
                    },
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                )
                PreferenceItem(
                    title = stringResource(id = R.string.unmount_sd_title),
                    description = stringResource(id = R.string.unmount_sd_description),
                    icon = Icons.Outlined.SdCard,
                    showToggle = true,
                    isChecked = uiState.preferences[AppPrefs.UMOUNT_SD] ?: false,
                    onClick = { settingsViewModel.togglePreference(AppPrefs.UMOUNT_SD, !it) },
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                )
                PreferenceItem(
                    title = stringResource(id = R.string.keep_screen_on),
                    icon = Icons.Outlined.LightMode,
                    showToggle = true,
                    isChecked = uiState.preferences[AppPrefs.KEEP_SCREEN_ON] ?: false,
                    onClick = { settingsViewModel.togglePreference(AppPrefs.KEEP_SCREEN_ON, !it) },
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                )
                PreferenceItem(
                    title = stringResource(id = R.string.preserve_userdata_title),
                    description = stringResource(id = R.string.preserve_userdata_description),
                    icon = Icons.Outlined.Save,
                    showToggle = true,
                    isChecked = uiState.preferences[AppPrefs.PRESERVE_USERDATA] ?: false,
                    onClick = { settingsViewModel.togglePreference(AppPrefs.PRESERVE_USERDATA, !it) },
                )
            }
        }

        // Section: Developer Options
        if (uiState.isDevOptEnabled) {
            Title(title = stringResource(id = R.string.developer_options))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                tonalElevation = 1.dp,
            ) {
                Column {
                    PreferenceItem(
                        title = stringResource(id = R.string.storage_check_title),
                        description = stringResource(id = R.string.storage_check_description),
                        icon = Icons.Outlined.WarningAmber,
                        showToggle = true,
                        isChecked = uiState.preferences[AppPrefs.DISABLE_STORAGE_CHECK] ?: false,
                        onClick = {
                            if (!it) {
                                settingsViewModel.updateSheetDisplay(DialogSheetState.DISABLE_STORAGE_CHECK)
                            }
                            settingsViewModel.togglePreference(AppPrefs.DISABLE_STORAGE_CHECK, !it)
                        },
                    )
                    if (settingsViewModel.getOperationMode() != OperationMode.ADB) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        )
                        PreferenceItem(
                            title = stringResource(id = R.string.full_logcat_logging_title),
                            description = stringResource(id = R.string.full_logcat_logging_description),
                            icon = Icons.Outlined.Terminal,
                            showToggle = true,
                            isChecked = uiState.preferences[AppPrefs.FULL_LOGCAT_LOGGING] ?: false,
                            onClick = { settingsViewModel.togglePreference(AppPrefs.FULL_LOGCAT_LOGGING, !it) },
                        )
                    }
                }
            }
        }

        // Section: System & About
        Title(title = stringResource(id = R.string.other))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            tonalElevation = 1.dp,
        ) {
            Column {
                PreferenceItem(
                    title = stringResource(id = R.string.operation_mode),
                    description = settingsViewModel.checkOperationMode(),
                    icon = Icons.Outlined.Security,
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                )
                PreferenceItem(
                    title = stringResource(id = R.string.about),
                    description = stringResource(id = R.string.about_description),
                    icon = Icons.Outlined.Info,
                    onClick = { navigate(Destinations.About) },
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    when (uiState.dialogSheetDisplay) {
        DialogSheetState.BUILT_IN_INSTALLER ->
            DialogLikeBottomSheet(
                title = stringResource(id = R.string.experimental_feature),
                icon = Icons.Outlined.NewReleases,
                text = stringResource(id = R.string.experimental_feature_description),
                confirmText = stringResource(id = R.string.yes),
                cancelText = stringResource(id = R.string.cancel),
                onClickCancel = {
                    settingsViewModel.togglePreference(AppPrefs.USE_BUILTIN_INSTALLER, false)
                    settingsViewModel.updateSheetDisplay(DialogSheetState.NONE)
                },
                onClickConfirm = { settingsViewModel.updateSheetDisplay(DialogSheetState.NONE) },
            )

        DialogSheetState.DISABLE_STORAGE_CHECK ->
            DialogLikeBottomSheet(
                title = stringResource(id = R.string.warning_storage_check_title),
                icon = Icons.Outlined.WarningAmber,
                text = stringResource(id = R.string.warning_storage_check_description),
                confirmText = stringResource(id = R.string.continue_anyway),
                cancelText = stringResource(id = R.string.cancel),
                onClickCancel = {
                    settingsViewModel.togglePreference(AppPrefs.DISABLE_STORAGE_CHECK, false)
                    settingsViewModel.updateSheetDisplay(DialogSheetState.NONE)
                },
                onClickConfirm = { settingsViewModel.updateSheetDisplay(DialogSheetState.NONE) },
            )

        else -> {}
    }
}
