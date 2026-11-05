package vegabobo.dsusideloader.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import vegabobo.dsusideloader.installer.LiveInstallationManager
import vegabobo.dsusideloader.ui.components.LiveInstallationBanner
import vegabobo.dsusideloader.ui.screen.about.AboutScreen
import vegabobo.dsusideloader.ui.screen.adb.AdbScreen
import vegabobo.dsusideloader.ui.screen.home.Home
import vegabobo.dsusideloader.ui.screen.install.InstallScreen
import vegabobo.dsusideloader.ui.screen.libraries.LibrariesScreen
import vegabobo.dsusideloader.ui.screen.settings.Settings
import vegabobo.dsusideloader.ui.screen.systems.SystemsScreen
import vegabobo.dsusideloader.util.collectAsStateWithLifecycle

object Destinations {
    const val Homepage = "home"
    const val Install = "install"
    const val Systems = "systems"
    const val Preferences = "preferences"
    const val Partitions = "partitions"
    const val ADBInstallation = "adb_installation"
    const val About = "about"
    const val Libraries = "libraries"
    const val Up = "up"
}

@Composable
fun Navigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination?.route ?: Destinations.Homepage

    val context = LocalContext.current
    val liveInstallationManager = remember { LiveInstallationManager.getInstance(context) }
    val liveState by liveInstallationManager.liveState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = Destinations.Homepage) {
            fun navigate(destination: String) {
                if (destination == Destinations.Up) {
                    navController.navigateUp()
                } else if (
                    destination == Destinations.Homepage ||
                    destination == Destinations.Install ||
                    destination == Destinations.Systems ||
                    destination == Destinations.Preferences ||
                    destination == Destinations.Partitions
                ) {
                    navController.navigate(destination) {
                        popUpTo(Destinations.Homepage) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                } else {
                    navController.navigate(destination)
                }
            }

            composable(Destinations.Homepage) { Home(navigate = { navigate(it) }) }
            composable(Destinations.Install) { InstallScreen(navigate = { navigate(it) }) }
            composable(Destinations.Systems) { SystemsScreen(navigate = { navigate(it) }) }
            composable(Destinations.Preferences) { Settings(navigate = { navigate(it) }) }
            composable(Destinations.Partitions) { SystemsScreen(navigate = { navigate(it) }) }
            composable(Destinations.ADBInstallation) { AdbScreen(navigate = { navigate(it) }) }
            composable(Destinations.About) { AboutScreen(navigate = { navigate(it) }) }
            composable(Destinations.Libraries) { LibrariesScreen(navigate = { navigate(it) }) }
        }

        LiveInstallationBanner(
            liveState = liveState,
            currentDestination = currentDestination,
            onNavigateHome = {
                navController.navigate(Destinations.Install) {
                    popUpTo(Destinations.Homepage) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            onCancel = { liveInstallationManager.requestCancel() },
            onReboot = { liveInstallationManager.requestReboot() },
            onDismiss = { liveInstallationManager.dismissAlert() },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
