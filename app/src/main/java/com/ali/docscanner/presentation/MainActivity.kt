package com.ali.docscanner.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.ali.docscanner.navigation.AppNavigation
import com.ali.docscanner.presentation.theme.DocScannerTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Phase 1's temporary SmokeTestScreen/SmokeTestViewModel (which verified Hilt + Room +
 * Coroutines wiring) has been removed now that the real navigation graph exists —
 * HomeScreen -> CameraScreen now serves as the end-to-end verification instead.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DocScannerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}
