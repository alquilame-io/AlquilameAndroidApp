package com.io.alquilame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import com.io.alquilame.ui.AppRoot
import com.io.alquilame.ui.common.LocalBackend
import com.io.alquilame.ui.theme.AlquilameTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val backend = (application as AlquilameApp).backend
        setContent {
            AlquilameTheme {
                CompositionLocalProvider(LocalBackend provides backend) {
                    AppRoot()
                }
            }
        }
    }
}
