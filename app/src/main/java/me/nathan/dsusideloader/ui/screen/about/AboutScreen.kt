package me.nathan.dsusideloader.ui.screen.about

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.flow.collectLatest
import me.nathan.dsusideloader.R
import me.nathan.dsusideloader.ui.cards.updater.UpdaterCard
import me.nathan.dsusideloader.ui.components.ApplicationScreen
import me.nathan.dsusideloader.ui.components.PreferenceItem
import me.nathan.dsusideloader.ui.components.Title
import me.nathan.dsusideloader.ui.components.TopBar
import me.nathan.dsusideloader.ui.screen.Destinations
import me.nathan.dsusideloader.util.DevicePropUtils
import me.nathan.dsusideloader.util.collectAsStateWithLifecycle

object AboutLinks {
    const val CONTRIBUTORS_URL = "https://github.com/VegaBobo/DSU-Sideloader/graphs/contributors"
    const val REPOSITORY_URL = "https://github.com/imnathanzero/DSU-Sideloader"
    const val VEGABOBO_REPO = "https://github.com/VegaBobo/DSU-Sideloader"
    const val WSTXDA_GITHUB = "https://github.com/WSTxda"
    const val VEGABOBO_GITHUB = "https://github.com/VegaBobo"
    const val NATHAN_GITHUB = "https://github.com/imnathanzero"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    navigate: (String) -> Unit,
    aboutViewModel: AboutViewModel = hiltViewModel(),
    isSubScreen: Boolean = false,
) {
    val uiState by aboutViewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        aboutViewModel.resetDeveloperOptionsCounter()
        uiState.toastDisplay.collectLatest {
            when (it) {
                DevOptToastDisplay.ENABLED_DEV_OPT ->
                    Toast.makeText(
                        context,
                        context.getString(R.string.developer_options_enabled),
                        Toast.LENGTH_LONG,
                    ).show()

                DevOptToastDisplay.DISABLED_DEV_OPT ->
                    Toast.makeText(
                        context,
                        context.getString(R.string.developer_options_disabled),
                        Toast.LENGTH_LONG,
                    ).show()

                else -> {}
            }
        }
    }

    ApplicationScreen(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        topBar = {
            TopBar(
                barTitle = stringResource(id = R.string.about),
                scrollBehavior = it,
                onClickBackButton = if (isSubScreen) { { navigate(Destinations.Up) } } else null,
            )
        },
    ) {
        // App header & updater
        UpdaterCard(
            uiState = uiState.updaterCardState,
            isUpdaterAvailable = uiState.isUpdaterAvailable,
            onClickImage = { aboutViewModel.onClickImage() },
            onClickCheckUpdates = { aboutViewModel.onClickCheckUpdates() },
            onClickDownloadUpdate = { aboutViewModel.onClickDownloadUpdate() },
            onClickViewChangelog = { uriHandler.openUri(aboutViewModel.response.changelogUrl) },
        )

        // Section: Device Information
        Title(
            "Device Information",
            modifier = Modifier.padding(top = 4.dp),
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            tonalElevation = 1.dp,
        ) {
            Column {
                val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
                PreferenceItem(
                    title = "$manufacturer ${Build.MODEL}",
                    description = "Device Model (${Build.DEVICE})",
                    icon = Icons.Outlined.Smartphone,
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                )
                PreferenceItem(
                    title = "Android ${Build.VERSION.RELEASE}",
                    description = "API Level ${Build.VERSION.SDK_INT}",
                    icon = Icons.Outlined.Android,
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                )
                PreferenceItem(
                    title = Build.VERSION.SECURITY_PATCH,
                    description = "Security Patch Level",
                    icon = Icons.Outlined.Security,
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                )
                val hasDynamic = DevicePropUtils.hasDynamicPartitions()
                PreferenceItem(
                    title = if (hasDynamic) "Supported" else "Unsupported",
                    description = "Dynamic Partitions Status",
                    icon = Icons.Outlined.Layers,
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                )
                val primaryAbi = Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown"
                PreferenceItem(
                    title = primaryAbi,
                    description = "Primary Architecture / ABI",
                    icon = Icons.Outlined.Memory,
                )
            }
        }

        // Section: Application & Links
        Title(
            stringResource(id = R.string.application),
            modifier = Modifier.padding(top = 4.dp),
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            tonalElevation = 1.dp,
        ) {
            Column {
                PreferenceItem(
                    title = stringResource(id = R.string.github_repo),
                    description = stringResource(id = R.string.github_repo_description),
                    icon = Icons.Outlined.Code,
                    onClick = { uriHandler.openUri(AboutLinks.REPOSITORY_URL) },
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                )
                PreferenceItem(
                    title = stringResource(id = R.string.libraries_title),
                    description = stringResource(id = R.string.libraries_description),
                    icon = Icons.Outlined.CollectionsBookmark,
                    onClick = { navigate(Destinations.Libraries) },
                )
            }
        }

        // Section: Collaborators & Credits
        Title(
            stringResource(id = R.string.collaborators),
            modifier = Modifier.padding(top = 4.dp),
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            tonalElevation = 1.dp,
        ) {
            Column {
                PreferenceItem(
                    title = "Nathan (@imnathanzero)",
                    description = "Fork Maintainer / Material 3 Revamp",
                    icon = Icons.Outlined.Person,
                    onClick = { uriHandler.openUri(AboutLinks.NATHAN_GITHUB) },
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                )
                PreferenceItem(
                    title = "VegaBobo",
                    description = stringResource(id = R.string.role_developer),
                    icon = Icons.Outlined.Person,
                    onClick = { uriHandler.openUri(AboutLinks.VEGABOBO_GITHUB) },
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                )
                PreferenceItem(
                    title = "WSTxda",
                    description = stringResource(id = R.string.role_design_icon),
                    icon = Icons.Outlined.Person,
                    onClick = { uriHandler.openUri(AboutLinks.WSTXDA_GITHUB) },
                )
                val translators = stringResource(id = R.string.translators_list)
                if (translators.isNotEmpty() && translators != "translators_list") {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    )
                    PreferenceItem(
                        title = stringResource(id = R.string.translators_title),
                        description = stringResource(id = R.string.translators_list),
                        icon = Icons.Outlined.Translate,
                    )
                }
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                )
                PreferenceItem(
                    title = stringResource(id = R.string.contributors_title),
                    description = stringResource(id = R.string.contributors_text),
                    icon = Icons.Outlined.Group,
                    onClick = { uriHandler.openUri(AboutLinks.CONTRIBUTORS_URL) },
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
