package com.ali.docscanner.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/**
 * Camera permission lifecycle, expressed explicitly so the UI can react to each case
 * distinctly (as required): first-time ask, user denied but can be asked again, and
 * "don't ask again" (permanently denied), which requires sending the user to Settings.
 */
enum class PermissionStatus {
    NOT_REQUESTED,
    GRANTED,
    DENIED,
    PERMANENTLY_DENIED
}

data class CameraPermissionState(
    val status: PermissionStatus,
    val requestPermission: () -> Unit
)

/**
 * Wraps [androidx.activity.result.contract.ActivityResultContracts.RequestPermission]
 * (part of the standard AndroidX Activity library) — no Accompanist dependency needed.
 */
@Composable
fun rememberCameraPermissionState(): CameraPermissionState {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasRequestedBefore by rememberSaveable { mutableStateOf(false) }
    var permissionGranted by remember { mutableStateOf(isCameraPermissionGranted(context)) }
    var canShowRationale by remember { mutableStateOf(true) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        hasRequestedBefore = true
        if (!granted && activity != null) {
            canShowRationale = activity.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
        }
    }

    // Re-check permission whenever the user returns to this screen — e.g. after
    // granting it from the system Settings screen.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionGranted = isCameraPermissionGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val status = when {
        permissionGranted -> PermissionStatus.GRANTED
        !hasRequestedBefore -> PermissionStatus.NOT_REQUESTED
        canShowRationale -> PermissionStatus.DENIED
        else -> PermissionStatus.PERMANENTLY_DENIED
    }

    return remember(status) {
        CameraPermissionState(
            status = status,
            requestPermission = { launcher.launch(Manifest.permission.CAMERA) }
        )
    }
}

fun isCameraPermissionGranted(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
}

fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
