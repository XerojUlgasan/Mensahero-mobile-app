package com.example.mensahero_mobile_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.example.mensahero_mobile_app.navigation.AppNavGraph
import com.example.mensahero_mobile_app.ui.theme.MensaheromobileappTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MensaheromobileappTheme {
                val navController = rememberNavController()
                AppNavGraph(
                    navController = navController,
                    context = this
                )
            }
        }
    }
}