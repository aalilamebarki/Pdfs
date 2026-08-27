package com.ali.docscanner.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ali.docscanner.presentation.camera.CameraScreen
import com.ali.docscanner.presentation.camera.ConfirmScreen
import com.ali.docscanner.presentation.home.HomeScreen

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Camera : Screen("camera")
    data object Confirm : Screen("confirm/{imagePath}") {
        fun createRoute(imagePath: String): String = "confirm/${Uri.encode(imagePath)}"
    }
}

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {

        composable(Screen.Home.route) {
            HomeScreen(
                onScanClick = { navController.navigate(Screen.Camera.route) }
            )
        }

        composable(Screen.Camera.route) {
            CameraScreen(
                onImageReady = { filePath ->
                    navController.navigate(Screen.Confirm.createRoute(filePath))
                },
                onClose = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Confirm.route,
            arguments = listOf(navArgument("imagePath") { type = NavType.StringType })
        ) {
            ConfirmScreen(
                onSaved = {
                    navController.popBackStack(Screen.Home.route, inclusive = false)
                },
                onRetake = { navController.popBackStack() }
            )
        }
    }
}
