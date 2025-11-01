package com.DriveLay.JuanPerez

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.DriveLay.JuanPerez.navigation.AppNavigation
import com.DriveLay.JuanPerez.ui.theme.JuanPerezTheme
import com.google.firebase.auth.FirebaseAuth


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        setContent {
            JuanPerezTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val isLoggedIn = FirebaseAuth.getInstance().currentUser != null
                    val startDestination = if (isLoggedIn) {
                        com.DriveLay.JuanPerez.navigation.Screen.Home.route
                    } else {
                        com.DriveLay.JuanPerez.navigation.Screen.Welcome.route
                    }
                    AppNavigation(navController = navController, startDestination = startDestination)
                }
            }
        }
    }
}