package com.DriveLay.JuanPerez.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.DriveLay.JuanPerez.ui.screens.LoginScreen
import com.DriveLay.JuanPerez.ui.screens.SignUpScreen
import com.DriveLay.JuanPerez.ui.screens.WelcomeScreen
import com.DriveLay.JuanPerez.ui.screens.HomeScreen
import com.DriveLay.JuanPerez.ui.screens.CreateCompanyScreen
import com.DriveLay.JuanPerez.ui.screens.JoinCompanyScreen
import com.DriveLay.JuanPerez.ui.screens.ProfileScreen
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object Home : Screen("home")
    object CreateCompany : Screen("createCompany")
    object JoinCompany : Screen("joinCompany")
    object Profile : Screen("profile")
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Welcome.route
    ) {
        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onLoginClick = {
                    navController.navigate(Screen.Login.route)
                },
                onSignUpClick = {
                    navController.navigate(Screen.SignUp.route)
                }
            )
        }
        
        composable(Screen.Login.route) {
            LoginScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onLoginSuccess = {
                    // Navegar a Home después del login exitoso
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onForgotPasswordClick = {
                    // Aquí se implementaría la navegación a recuperar contraseña
                }
            )
        }
        
        composable(Screen.SignUp.route) {
            SignUpScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onSignUpSuccess = {
                    // Navegar a Home después del registro exitoso
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onLoginClick = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Welcome.route)
                    }
                }
            )
        }

        // Home post-login
        composable(Screen.Home.route) {
            HomeScreen(
                onCreateCompanyClick = {
                    navController.navigate(Screen.CreateCompany.route)
                },
                onJoinCompanyClick = {
                    navController.navigate(Screen.JoinCompany.route)
                },
                onBottomNavSelected = { dest ->
                    when (dest) {
                        "home" -> Unit // ya estamos en Home
                        "perfil" -> navController.navigate(Screen.Profile.route)
                        // Rutas futuras: "viajes", "mensajes", "alertas", "perfil"
                        else -> Unit
                    }
                }
            )
        }

        // Crear Empresa
        composable(Screen.CreateCompany.route) {
            val scope = androidx.compose.runtime.rememberCoroutineScope()
            val firebaseManager = androidx.compose.runtime.remember { com.DriveLay.JuanPerez.firebase.FirebaseManager() }
            CreateCompanyScreen(
                onBackClick = { navController.popBackStack() },
                onSubmit = { name, employees, vehicles ->
                    scope.launch {
                        val result = firebaseManager.createCompany(name, employees, vehicles)
                        result.fold(
                            onSuccess = {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Home.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            onFailure = {
                                navController.popBackStack()
                            }
                        )
                    }
                }
            )
        }

        // Unirse a Empresa
        composable(Screen.JoinCompany.route) {
            val scope = androidx.compose.runtime.rememberCoroutineScope()
            val firebaseManager = androidx.compose.runtime.remember { com.DriveLay.JuanPerez.firebase.FirebaseManager() }
            JoinCompanyScreen(
                onBackClick = { navController.popBackStack() },
                onJoin = { code, _ ->
                    scope.launch {
                        val result = firebaseManager.joinCompany(code)
                        result.fold(
                            onSuccess = {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Home.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            onFailure = {
                                navController.popBackStack()
                            }
                        )
                    }
                }
            )
        }

        // Perfil
        composable(Screen.Profile.route) {
            val firebaseManager = androidx.compose.runtime.remember { com.DriveLay.JuanPerez.firebase.FirebaseManager() }
            ProfileScreen(
                onBackClick = { navController.popBackStack() },
                onSignOut = {
                    // Al cerrar sesión, ir a Welcome y limpiar Home
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}