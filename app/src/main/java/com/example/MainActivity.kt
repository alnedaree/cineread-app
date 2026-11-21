package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.config.configureAppCheck
import com.example.ui.AtheerRadioApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure Firebase App Check with launch intent before any UI or Firebase calls
        configureAppCheck(this, intent)

        // STEP 2: Enable full-bleed edge-to-edge display and render Atheer Radio UI
        enableEdgeToEdge()
        setContent {
            AtheerRadioApp()
        }
    }
}
