package me.nathan.dsusideloader.ui.screen

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import me.nathan.dsusideloader.ui.screen.about.AboutScreen
import me.nathan.dsusideloader.ui.screen.adb.AdbScreen
import me.nathan.dsusideloader.ui.screen.home.Home
import me.nathan.dsusideloader.ui.screen.libraries.LibrariesScreen
import me.nathan.dsusideloader.ui.screen.partitions.PartitionsScreen
import me.nathan.dsusideloader.ui.screen.settings.Settings

object Destinations {
    const val Homepage = "home"
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
    NavHost(navController = navController, startDestination = Destinations.Homepage) {
        fun navigate(destination: String) {
            if (destination == Destinations.Up) {
                navController.navigateUp()
            } else if (
                destination == Destinations.Homepage ||
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
        composable(Destinations.Preferences) { Settings(navigate = { navigate(it) }) }
        composable(Destinations.Partitions) { PartitionsScreen(navigate = { navigate(it) }) }
        composable(Destinations.ADBInstallation) { AdbScreen(navigate = { navigate(it) }) }
        composable(Destinations.About) { AboutScreen(navigate = { navigate(it) }) }
        composable(Destinations.Libraries) { LibrariesScreen(navigate = { navigate(it) }) }
    }
}
