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
import com.ali.docscanner.presentation.crop.CropScreen
import com.ali.docscanner.presentation.documentpreview.DocumentPreviewScreen
import com.ali.docscanner.presentation.documentslist.DocumentsListScreen
import com.ali.docscanner.presentation.filter.FilterScreen
import com.ali.docscanner.presentation.home.HomeScreen
import com.ali.docscanner.presentation.pdfexport.PdfExportScreen

/**
 * documentId sentinel: "0" means "no document created yet — the first confirmed page
 * will create one". A real Room-generated id (as a String) is threaded through once it
 * exists, so Camera/Crop/Filter can add subsequent pages to the same document.
 */
private const val NEW_DOCUMENT_ID = "0"

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object DocumentsList : Screen("documentsList")

    data object Camera : Screen("camera/{documentId}") {
        fun createRoute(documentId: Long): String = "camera/$documentId"
    }

    data object Crop : Screen("crop/{imagePath}/{documentId}") {
        fun createRoute(imagePath: String, documentId: Long): String =
            "crop/${Uri.encode(imagePath)}/$documentId"
    }

    data object Filter : Screen("filter/{imagePath}/{documentId}") {
        fun createRoute(imagePath: String, documentId: Long): String =
            "filter/${Uri.encode(imagePath)}/$documentId"
    }

    data object DocumentPreview : Screen("documentPreview/{documentId}") {
        fun createRoute(documentId: Long): String = "documentPreview/$documentId"
    }

    data object PdfExport : Screen("pdfExport/{documentId}") {
        fun createRoute(documentId: Long): String = "pdfExport/$documentId"
    }
}

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {

        composable(Screen.Home.route) {
            HomeScreen(
                onScanClick = { navController.navigate(Screen.Camera.createRoute(0L)) },
                onSeeAllClick = { navController.navigate(Screen.DocumentsList.route) },
                onDocumentClick = { documentId ->
                    navController.navigate(Screen.PdfExport.createRoute(documentId))
                }
            )
        }

        composable(Screen.DocumentsList.route) {
            DocumentsListScreen(
                onDocumentClick = { documentId ->
                    navController.navigate(Screen.PdfExport.createRoute(documentId))
                }
            )
        }

        composable(
            route = Screen.Camera.route,
            arguments = listOf(navArgument("documentId") { type = NavType.StringType; defaultValue = NEW_DOCUMENT_ID })
        ) {
            CameraScreen(
                onImageReady = { filePath, documentId ->
                    navController.navigate(Screen.Crop.createRoute(filePath, documentId))
                },
                onClose = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Crop.route,
            arguments = listOf(
                navArgument("imagePath") { type = NavType.StringType },
                navArgument("documentId") { type = NavType.StringType }
            )
        ) {
            CropScreen(
                onCropped = { croppedPath, documentId ->
                    navController.navigate(Screen.Filter.createRoute(croppedPath, documentId))
                },
                onClose = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Filter.route,
            arguments = listOf(
                navArgument("imagePath") { type = NavType.StringType },
                navArgument("documentId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val incomingDocumentId = backStackEntry.arguments?.getString("documentId")?.toLongOrNull() ?: 0L
            FilterScreen(
                onSaved = { savedDocumentId ->
                    navController.navigate(Screen.DocumentPreview.createRoute(savedDocumentId)) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                },
                onClose = {
                    if (incomingDocumentId == 0L) {
                        // First page of a brand-new scan — nothing to return to but Home.
                        navController.popBackStack(Screen.Home.route, inclusive = false)
                    } else {
                        // Adding a page to an existing document — return to that
                        // document's preview, not all the way back to Home.
                        navController.popBackStack(
                            Screen.DocumentPreview.createRoute(incomingDocumentId),
                            inclusive = false
                        )
                    }
                }
            )
        }

        composable(
            route = Screen.DocumentPreview.route,
            arguments = listOf(navArgument("documentId") { type = NavType.StringType })
        ) {
            DocumentPreviewScreen(
                onAddPage = { documentId ->
                    navController.navigate(Screen.Camera.createRoute(documentId))
                },
                onExport = { documentId ->
                    navController.navigate(Screen.PdfExport.createRoute(documentId))
                },
                onDocumentDeleted = {
                    navController.popBackStack(Screen.Home.route, inclusive = false)
                }
            )
        }

        composable(
            route = Screen.PdfExport.route,
            arguments = listOf(navArgument("documentId") { type = NavType.StringType })
        ) {
            PdfExportScreen(
                onDone = {
                    navController.popBackStack(Screen.Home.route, inclusive = false)
                }
            )
        }
    }
}
