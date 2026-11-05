package git.imnathanzero.sysphere.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.InstallMobile
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import git.imnathanzero.sysphere.R
import git.imnathanzero.sysphere.ui.screen.Destinations

@Composable
fun AppBottomBar(
    currentDestination: String,
    onNavigate: (String) -> Unit,
) {
    NavigationBar {
        // 1. Home Dashboard
        NavigationBarItem(
            selected = currentDestination == Destinations.Homepage,
            onClick = {
                if (currentDestination != Destinations.Homepage) {
                    onNavigate(Destinations.Homepage)
                }
            },
            icon = {
                Icon(
                    imageVector = if (currentDestination == Destinations.Homepage) {
                        Icons.Filled.Home
                    } else {
                        Icons.Outlined.Home
                    },
                    contentDescription = stringResource(R.string.nav_home),
                )
            },
            label = { Text(text = stringResource(R.string.nav_home)) },
        )

        // 2. Installation Wizard
        NavigationBarItem(
            selected = currentDestination == Destinations.Install,
            onClick = {
                if (currentDestination != Destinations.Install) {
                    onNavigate(Destinations.Install)
                }
            },
            icon = {
                Icon(
                    imageVector = if (currentDestination == Destinations.Install) {
                        Icons.Filled.InstallMobile
                    } else {
                        Icons.Outlined.InstallMobile
                    },
                    contentDescription = stringResource(R.string.nav_install),
                )
            },
            label = { Text(text = stringResource(R.string.nav_install)) },
        )

        // 3. Systems Manager
        NavigationBarItem(
            selected = currentDestination == Destinations.Systems || currentDestination == Destinations.Partitions,
            onClick = {
                if (currentDestination != Destinations.Systems) {
                    onNavigate(Destinations.Systems)
                }
            },
            icon = {
                Icon(
                    imageVector = if (currentDestination == Destinations.Systems || currentDestination == Destinations.Partitions) {
                        Icons.Filled.Layers
                    } else {
                        Icons.Outlined.Layers
                    },
                    contentDescription = stringResource(R.string.nav_systems),
                )
            },
            label = { Text(text = stringResource(R.string.nav_systems)) },
        )

        // 4. Settings
        NavigationBarItem(
            selected = currentDestination == Destinations.Preferences,
            onClick = {
                if (currentDestination != Destinations.Preferences) {
                    onNavigate(Destinations.Preferences)
                }
            },
            icon = {
                Icon(
                    imageVector = if (currentDestination == Destinations.Preferences) {
                        Icons.Filled.Settings
                    } else {
                        Icons.Outlined.Settings
                    },
                    contentDescription = stringResource(R.string.nav_settings),
                )
            },
            label = { Text(text = stringResource(R.string.nav_settings)) },
        )
    }
}
