package me.nathan.dsusideloader.ui.screen

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.InstallMobile
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import me.nathan.dsusideloader.R
import me.nathan.dsusideloader.ui.components.NavigationTabItem
import me.nathan.dsusideloader.ui.components.SlidingBottomBar
import me.nathan.dsusideloader.ui.screen.about.AboutScreen
import me.nathan.dsusideloader.ui.screen.adb.AdbScreen
import me.nathan.dsusideloader.ui.screen.home.Home
import me.nathan.dsusideloader.ui.screen.libraries.LibrariesScreen
import me.nathan.dsusideloader.ui.screen.settings.Settings

object Destinations {
    const val Main = "main"
    const val Homepage = "home"
    const val Preferences = "preferences"
    const val ADBInstallation = "adb_installation"
    const val About = "about"
    const val Libraries = "libraries"
    const val Up = "up"
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    navigate: (String) -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = 0) { 3 }
    val coroutineScope = rememberCoroutineScope()

    val navItems = listOf(
        NavigationTabItem(
            title = stringResource(id = R.string.installation),
            selectedIcon = Icons.Filled.InstallMobile,
            unselectedIcon = Icons.Outlined.InstallMobile,
        ),
        NavigationTabItem(
            title = stringResource(id = R.string.settings),
            selectedIcon = Icons.Filled.Settings,
            unselectedIcon = Icons.Outlined.Settings,
        ),
        NavigationTabItem(
            title = stringResource(id = R.string.about),
            selectedIcon = Icons.Filled.Info,
            unselectedIcon = Icons.Outlined.Info,
        ),
    )

    Scaffold(
        bottomBar = {
            SlidingBottomBar(
                pagerState = pagerState,
                items = navItems,
                onTabSelected = { page ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(page)
                    }
                },
            )
        },
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()),
        ) { page ->
            when (page) {
                0 -> Home(navigate = navigate)
                1 -> Settings(navigate = navigate, isSubScreen = false)
                2 -> AboutScreen(navigate = navigate, isSubScreen = false)
            }
        }
    }
}

@Composable
fun Navigation() {
    val navController = rememberNavController()

    fun navigate(destination: String) {
        if (destination == Destinations.Up) {
            navController.navigateUp()
        } else {
            navController.navigate(destination)
        }
    }

    NavHost(
        navController = navController,
        startDestination = Destinations.Main,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
            ) + fadeIn(animationSpec = tween(350))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> -fullWidth / 4 },
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
            ) + fadeOut(animationSpec = tween(250))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> -fullWidth / 4 },
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
            ) + fadeIn(animationSpec = tween(350))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
            ) + fadeOut(animationSpec = tween(250))
        },
    ) {
        composable(Destinations.Main) { MainScreen(navigate = ::navigate) }
        composable(Destinations.Homepage) { MainScreen(navigate = ::navigate) }
        composable(Destinations.Preferences) { Settings(navigate = ::navigate, isSubScreen = true) }
        composable(Destinations.ADBInstallation) { AdbScreen(navigate = ::navigate) }
        composable(Destinations.About) { AboutScreen(navigate = ::navigate, isSubScreen = true) }
        composable(Destinations.Libraries) { LibrariesScreen(navigate = ::navigate) }
    }
}
