package com.DriveLay.JuanPerez.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.DriveLay.JuanPerez.ui.screens.*

import com.DriveLay.JuanPerez.ui.screens.LoginScreen
import com.DriveLay.JuanPerez.ui.screens.SignUpScreen
import com.DriveLay.JuanPerez.ui.screens.WelcomeScreen
import com.DriveLay.JuanPerez.ui.screens.HomeScreen
import com.DriveLay.JuanPerez.ui.screens.CreateCompanyScreen
import com.DriveLay.JuanPerez.ui.screens.JoinCompanyScreen
import com.DriveLay.JuanPerez.ui.screens.ProfileScreen
import com.DriveLay.JuanPerez.ui.screens.CompanyHomeScreen
import com.DriveLay.JuanPerez.ui.screens.VehiclesListScreen
import com.DriveLay.JuanPerez.ui.screens.AdminDashboardScreen
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object Home : Screen("home")
    object CreateCompany : Screen("createCompany")
    object JoinCompany : Screen("joinCompany")
    object Profile : Screen("profile")
    object CompanyHome : Screen("companyHome")
    object Vehicles : Screen("vehicles")
    object AdminDashboard : Screen("adminDashboard")
    object Companies : Screen("companies")
    object CompanyHomeDetail : Screen("companyHome/{companyId}")
    object Employees : Screen("employees")
    object VehicleDetail : Screen("vehicleDetail/{vehicleId}")
}

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Welcome.route) {
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
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onForgotPasswordClick = {
                    // Recuperar contraseña (pendiente)
                }
            )
        }
        
        composable(Screen.SignUp.route) {
            SignUpScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onSignUpSuccess = {
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
                        "home" -> Unit
                        "empresa" -> navController.navigate(Screen.CompanyHome.route)
                        "vehiculos" -> navController.navigate(Screen.Vehicles.route)
                        "perfil" -> navController.navigate(Screen.Profile.route)
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
                                // Redirigir directamente al inicio de empresa
                                navController.navigate(Screen.CompanyHome.route) {
                                    popUpTo(Screen.Home.route) { inclusive = false }
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
                                // Redirigir directamente al inicio de empresa
                                navController.navigate(Screen.CompanyHome.route) {
                                    popUpTo(Screen.Home.route) { inclusive = false }
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
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        // Lista de Empresas
        composable(Screen.Companies.route) {
            CompaniesListScreen(
                onBackClick = { navController.popBackStack() },
                onSelectCompany = { companyId ->
                    navController.navigate("companyHome/$companyId")
                }
            )
        }

        // Inicio de Empresa (por defecto: empresa actual)
        composable(Screen.CompanyHome.route) {
            CompanyHomeScreen(
                onBackClick = { navController.popBackStack() },
                onGoToAdmin = { navController.navigate(Screen.AdminDashboard.route) }
            )
        }

        // Inicio de Empresa por ID explícito
        composable(
            route = "companyHome/{companyId}",
            arguments = listOf(navArgument("companyId") { type = NavType.StringType; nullable = true })
        ) { backStackEntry ->
            val companyId = backStackEntry.arguments?.getString("companyId")
            CompanyHomeScreen(
                onBackClick = { navController.popBackStack() },
                onGoToAdmin = { navController.navigate(Screen.AdminDashboard.route) },
                companyIdArg = companyId
            )
        }

        // Panel de Administración
        composable(Screen.AdminDashboard.route) {
            AdminDashboardScreen(
                onBackClick = { navController.popBackStack() },
                onManageEmployees = { navController.navigate(Screen.Employees.route) },
                onManageFleet = { navController.navigate(Screen.Vehicles.route) }
            )
        }

        // Lista de Empleados
        composable(Screen.Employees.route) {
            EmployeesListScreen(onBackClick = { navController.popBackStack() })
        }
        
        // Lista de Vehículos
        composable(Screen.Vehicles.route) {
            VehiclesListScreen(
                onBackClick = { navController.popBackStack() },
                onVehicleClick = { vehicleId ->
                    navController.navigate("vehicleDetail/$vehicleId")
                }
            )
        }

        // Detalle de Vehículo
        composable(
            route = "vehicleDetail/{vehicleId}",
            arguments = listOf(navArgument("vehicleId") { type = NavType.StringType })
        ) { backStackEntry ->
            val vehicleId = backStackEntry.arguments?.getString("vehicleId") ?: ""
            VehicleDetailScreen(
                onBackClick = { navController.popBackStack() },
                vehicleId = vehicleId
            )
        }
    }
}