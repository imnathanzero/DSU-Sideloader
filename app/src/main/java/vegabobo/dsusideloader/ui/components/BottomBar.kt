package vegabobo.dsusideloader.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import vegabobo.dsusideloader.R
import vegabobo.dsusideloader.ui.screen.Destinations

@Composable
fun AppBottomBar(
    currentDestination: String,
    onNavigate: (String) -> Unit,
    showPartitions: Boolean = true,
) {
    NavigationBar {
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
                    contentDescription = stringResource(R.string.installation),
                )
            },
            label = { Text(text = stringResource(R.string.installation)) },
        )
        if (showPartitions) {
            NavigationBarItem(
                selected = currentDestination == Destinations.Partitions,
                onClick = {
                    if (currentDestination != Destinations.Partitions) {
                        onNavigate(Destinations.Partitions)
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (currentDestination == Destinations.Partitions) {
                            Icons.Filled.Storage
                        } else {
                            Icons.Outlined.Storage
                        },
                        contentDescription = stringResource(R.string.partitions),
                    )
                },
                label = { Text(text = stringResource(R.string.partitions)) },
            )
        }
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
                    contentDescription = stringResource(R.string.settings),
                )
            },
            label = { Text(text = stringResource(R.string.settings)) },
        )
    }
}
