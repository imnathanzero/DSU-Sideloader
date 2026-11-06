package me.nathan.dsusideloader.ui.screen.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.SdCard
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import me.nathan.dsusideloader.R
import me.nathan.dsusideloader.preferences.AppPrefs
import me.nathan.dsusideloader.ui.components.AppBottomBar
import me.nathan.dsusideloader.ui.components.ApplicationScreen
import me.nathan.dsusideloader.ui.components.DialogLikeBottomSheet
import me.nathan.dsusideloader.ui.components.SimpleCard
import me.nathan.dsusideloader.ui.components.Title
import me.nathan.dsusideloader.ui.components.TopBar
import me.nathan.dsusideloader.ui.screen.Destinations
import me.nathan.dsusideloader.util.OperationMode
import me.nathan.dsusideloader.util.collectAsStateWithLifecycle

@Composable
private fun SettingsItemRow(
    title: String,
    description: String = "",
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    iconBackground: Color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
    showToggle: Boolean = false,
    isChecked: Boolean = false,
    isEnabled: Boolean = true,
    trailingContent: @Composable (() -> Unit)? = null,
    onClick: (Boolean) -> Unit = {},
) {
    val contentAlpha = if (isEnabled) 1f else 0.45f

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isEnabled) {
                    Modifier.clickable(onClick = { onClick(isChecked) })
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .alpha(contentAlpha),
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBackground),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (showToggle) {
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = isChecked,
                enabled = isEnabled,
                onCheckedChange = { onClick(isChecked) },
            )
        } else if (trailingContent != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailingContent()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Settings(
    navigate: (String) -> Unit,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        settingsViewModel.checkDevOpt()
    }

    ApplicationScreen(
        modifier = Modifier.padding(start = 10.dp, end = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        topBar = {
            TopBar(
                barTitle = stringResource(id = R.string.settings),
                scrollBehavior = it,
            )
        },
        bottomBar = {
            AppBottomBar(
                currentDestination = Destinations.Preferences,
                onNavigate = navigate,
            )
        },
    ) {
        // Installation Settings Group
        Title(
            title = stringResource(id = R.string.installation),
            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
        )
        SimpleCard(addPadding = false) {
            val isAndroidQ = settingsViewModel.isAndroidQ()
            val isBuiltinInstallerEnabled = uiState.isRoot && !isAndroidQ
            val builtinInstallerDesc = if (isAndroidQ) {
                stringResource(id = R.string.unsupported)
            } else if (uiState.isRoot) {
                stringResource(id = R.string.builtin_installer_description)
            } else {
                stringResource(R.string.requires_root)
            }

            SettingsItemRow(
                title = stringResource(id = R.string.builtin_installer),
                description = builtinInstallerDesc,
                icon = Icons.Outlined.Bolt,
                iconTint = MaterialTheme.colorScheme.primary,
                iconBackground = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                showToggle = true,
                isEnabled = isBuiltinInstallerEnabled,
                isChecked = uiState.preferences[AppPrefs.USE_BUILTIN_INSTALLER] ?: false,
                onClick = {
                    if (!it) {
                        settingsViewModel.updateSheetDisplay(DialogSheetState.BUILT_IN_INSTALLER)
                    }
                    settingsViewModel.togglePreference(AppPrefs.USE_BUILTIN_INSTALLER, !it)
                },
            )

            HorizontalDivider(
                modifier = Modifier.padding(start = 68.dp, end = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
            )

            SettingsItemRow(
                title = stringResource(id = R.string.unmount_sd_title),
                description = stringResource(id = R.string.unmount_sd_description),
                icon = Icons.Outlined.SdCard,
                iconTint = MaterialTheme.colorScheme.secondary,
                iconBackground = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                showToggle = true,
                isChecked = uiState.preferences[AppPrefs.UMOUNT_SD] ?: false,
                onClick = { settingsViewModel.togglePreference(AppPrefs.UMOUNT_SD, !it) },
            )

            HorizontalDivider(
                modifier = Modifier.padding(start = 68.dp, end = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
            )

            SettingsItemRow(
                title = stringResource(id = R.string.keep_screen_on),
                description = stringResource(id = R.string.keep_screen_on_description),
                icon = Icons.Outlined.Lightbulb,
                iconTint = MaterialTheme.colorScheme.tertiary,
                iconBackground = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                showToggle = true,
                isChecked = uiState.preferences[AppPrefs.KEEP_SCREEN_ON] ?: false,
                onClick = { settingsViewModel.togglePreference(AppPrefs.KEEP_SCREEN_ON, !it) },
            )
        }

        // Developer Options Group
        AnimatedVisibility(
            visible = uiState.isDevOptEnabled,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Title(
                    title = stringResource(id = R.string.developer_options),
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                )
                SimpleCard(addPadding = false) {
                    SettingsItemRow(
                        title = stringResource(id = R.string.storage_check_title),
                        description = stringResource(id = R.string.storage_check_description),
                        icon = Icons.Outlined.WarningAmber,
                        iconTint = MaterialTheme.colorScheme.error,
                        iconBackground = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
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
                            modifier = Modifier.padding(start = 68.dp, end = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        )
                        SettingsItemRow(
                            title = stringResource(id = R.string.full_logcat_logging_title),
                            description = stringResource(id = R.string.full_logcat_logging_description),
                            icon = Icons.Outlined.Terminal,
                            iconTint = MaterialTheme.colorScheme.secondary,
                            iconBackground = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                            showToggle = true,
                            isChecked = uiState.preferences[AppPrefs.FULL_LOGCAT_LOGGING] ?: false,
                            onClick = { settingsViewModel.togglePreference(AppPrefs.FULL_LOGCAT_LOGGING, !it) },
                        )
                    }
                }
            }
        }

        // Other Group
        Title(
            title = stringResource(id = R.string.other),
            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
        )
        SimpleCard(addPadding = false) {
            val operationModeText = settingsViewModel.checkOperationMode()
            SettingsItemRow(
                title = stringResource(id = R.string.operation_mode),
                description = operationModeText,
                icon = Icons.Outlined.Security,
                iconTint = MaterialTheme.colorScheme.primary,
                iconBackground = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                trailingContent = {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            text = operationModeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                },
            )

            HorizontalDivider(
                modifier = Modifier.padding(start = 68.dp, end = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
            )

            SettingsItemRow(
                title = stringResource(id = R.string.about),
                description = stringResource(id = R.string.about_description),
                icon = Icons.Outlined.Info,
                iconTint = MaterialTheme.colorScheme.secondary,
                iconBackground = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                onClick = { navigate(Destinations.About) },
                trailingContent = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                },
            )
        }
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
