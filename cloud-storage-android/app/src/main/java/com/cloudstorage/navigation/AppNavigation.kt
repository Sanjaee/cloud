package com.cloudstorage.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.cloudstorage.domain.repository.AuthRepository
import com.cloudstorage.feature.auth.authenticator.MfaVerificationScreen
import com.cloudstorage.feature.auth.google.GoogleLoginScreen
import com.cloudstorage.feature.drive.DriveScreen
import com.cloudstorage.feature.notifications.NotificationScreen
import com.cloudstorage.feature.preview.FilePreviewScreen
import com.cloudstorage.feature.recent.RecentScreen
import com.cloudstorage.feature.search.SearchScreen
import com.cloudstorage.feature.settings.SettingsScreen
import com.cloudstorage.feature.shared.SharedScreen
import com.cloudstorage.feature.splash.SplashScreen
import com.cloudstorage.feature.starred.StarredScreen
import com.cloudstorage.feature.storage.StorageScreen
import com.cloudstorage.feature.trash.TrashScreen

@Composable
fun AppNavigation(
    authRepository: AuthRepository
) {
    val rootNavController = rememberNavController()

    NavHost(
        navController = rootNavController,
        startDestination = Screen.Splash.route,
        enterTransition = { NavigationTransitions.defaultSlideEnter() },
        exitTransition = { NavigationTransitions.defaultSlideExit() },
        popEnterTransition = { NavigationTransitions.defaultSlidePopEnter() },
        popExitTransition = { NavigationTransitions.defaultSlidePopExit() }
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                authRepository = authRepository,
                onNavigateToLogin = {
                    rootNavController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToMain = {
                    rootNavController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            GoogleLoginScreen(
                onNavigateToMain = {
                    rootNavController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToMfa = { mfaToken, email ->
                    rootNavController.navigate(Screen.MfaVerify.createRoute(mfaToken, email))
                }
            )
        }

        composable(
            route = Screen.MfaVerify.route,
            arguments = listOf(
                navArgument("mfaToken") { type = NavType.StringType },
                navArgument("email") { type = NavType.StringType }
            )
        ) {
            MfaVerificationScreen(
                onNavigateToMain = {
                    rootNavController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    rootNavController.popBackStack()
                }
            )
        }

        composable(Screen.Main.route) {
            MainContainer(
                onLogout = {
                    rootNavController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}

@Composable
fun MainContainer(
    onLogout: () -> Unit
) {
    val bottomNavController = rememberNavController()
    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            // Only show bottom navigation on primary tabs
            val showBottomNav = currentRoute in listOf(
                Screen.Drive.route,
                Screen.Shared.route,
                Screen.Recent.route,
                Screen.Starred.route,
                Screen.Storage.route
            )
            if (showBottomNav) {
                AppBottomNavigation(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        bottomNavController.navigate(route) {
                            popUpTo(bottomNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = bottomNavController,
            startDestination = Screen.Drive.route,
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
            enterTransition = {
                NavigationTransitions.tabAwareSlideEnter(
                    initialRoute = initialState.destination.route,
                    targetRoute = targetState.destination.route
                )
            },
            exitTransition = {
                NavigationTransitions.tabAwareSlideExit(
                    initialRoute = initialState.destination.route,
                    targetRoute = targetState.destination.route
                )
            },
            popEnterTransition = { NavigationTransitions.defaultSlidePopEnter() },
            popExitTransition = { NavigationTransitions.defaultSlidePopExit() }
        ) {
            composable(
                route = Screen.Drive.route,
                arguments = listOf(
                    navArgument("folderId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) {
                DriveScreen(
                    onNavigateToSearch = { bottomNavController.navigate(Screen.Search.route) },
                    onNavigateToPreview = { fileId -> bottomNavController.navigate(Screen.Preview.createRoute(fileId)) },
                    onNavigateToShare = { _, _ -> bottomNavController.navigate(Screen.Shared.route) },
                    onNavigateToSettings = { bottomNavController.navigate(Screen.Settings.route) }
                )
            }

            composable(Screen.Shared.route) {
                SharedScreen(
                    onNavigateToPreview = { fileId -> bottomNavController.navigate(Screen.Preview.createRoute(fileId)) }
                )
            }

            composable(Screen.Recent.route) {
                RecentScreen(
                    onNavigateToPreview = { fileId -> bottomNavController.navigate(Screen.Preview.createRoute(fileId)) }
                )
            }

            composable(Screen.Starred.route) {
                StarredScreen(
                    onNavigateToPreview = { fileId -> bottomNavController.navigate(Screen.Preview.createRoute(fileId)) }
                )
            }

            composable(Screen.Storage.route) {
                StorageScreen(
                    onNavigateToSettings = { bottomNavController.navigate(Screen.Settings.route) }
                )
            }

            composable(Screen.Search.route) {
                SearchScreen(
                    onNavigateBack = { bottomNavController.popBackStack() },
                    onNavigateToPreview = { fileId -> bottomNavController.navigate(Screen.Preview.createRoute(fileId)) }
                )
            }

            composable(Screen.Trash.route) {
                TrashScreen(
                    onNavigateBack = { bottomNavController.popBackStack() }
                )
            }

            composable(Screen.Notifications.route) {
                NotificationScreen()
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateToLogin = onLogout,
                    onNavigateBack = { bottomNavController.popBackStack() }
                )
            }

            composable(
                route = Screen.Preview.route,
                arguments = listOf(navArgument("fileId") { type = NavType.StringType })
            ) {
                FilePreviewScreen(
                    onNavigateBack = { bottomNavController.popBackStack() }
                )
            }
        }
    }
}

