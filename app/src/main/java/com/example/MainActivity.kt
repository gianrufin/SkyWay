package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.RouteWeatherScreen
import com.example.ui.RouteWeatherViewModel
import com.example.ui.theme.SkySlateDeep
import com.example.ui.theme.SkyWayTheme

class MainActivity : ComponentActivity() {

    private val viewModel: RouteWeatherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent {
            SkyWayTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = SkySlateDeep,
                ) {
                    RouteWeatherScreen(viewModel = viewModel)
                }
            }
        }
    }
}
