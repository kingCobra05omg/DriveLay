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
    object AdminDashboardDetail : Screen("adminDashboard/{companyId}")
    object Employees : Screen("employees")
    object EmployeesDetail : Screen("employees/{companyId}")
    object VehiclesDetail : Screen("vehicles/{companyId}")
    object VehicleDetail : Screen("vehicleDetail/{vehicleId}")
    object VehicleCheckout : Screen("vehicleCheckout/{vehicleId}")
    object VehicleInUse : Screen("vehicleInUse/{vehicleId}")
    object VehicleReturn : Screen("vehicleReturn/{vehicleId}")
    object Notifications : Screen("notifications")
    object NotificationsDetail : Screen("notifications/{companyId}")
    object UsageHistory : Screen("usageHistory")
    object UsageHistoryDetail : Screen("usageHistory/{companyId}")
}

@Composable
fun AppNavigation(navController: NavHostController, startDestination: String) {
    NavHost(navController = navController, startDestination = startDestination) {
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
                        "inicio" -> Unit
                        "empresa" -> navController.navigate(Screen.CompanyHome.route)
                        "vehiculos" -> navController.navigate(Screen.Vehicles.route)
                        "notificaciones" -> navController.navigate(Screen.Notifications.route)
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
                onGoToAdmin = {
                    val cid = companyId ?: ""
                    navController.navigate("adminDashboard/$cid")
                },
                companyIdArg = companyId
            )
        }

        // Panel de Administración
        composable(Screen.AdminDashboard.route) {
            AdminDashboardScreen(
                onBackClick = { navController.popBackStack() },
                onManageEmployees = { navController.navigate(Screen.Employees.route) },
                onManageFleet = { navController.navigate(Screen.Vehicles.route) },
                onViewUsageHistory = { navController.navigate(Screen.UsageHistory.route) }
            )
        }

        // Panel de Administración por empresa seleccionada
        composable(
            route = "adminDashboard/{companyId}",
            arguments = listOf(navArgument("companyId") { type = NavType.StringType })
        ) { backStackEntry ->
            val companyId = backStackEntry.arguments?.getString("companyId") ?: ""
            AdminDashboardScreen(
                onBackClick = { navController.popBackStack() },
                onManageEmployees = { navController.navigate("employees/$companyId") },
                onManageFleet = { navController.navigate("vehicles/$companyId") },
                onViewUsageHistory = { navController.navigate("usageHistory/$companyId") },
                companyIdArg = companyId
            )
        }

        // Lista de Empleados
        composable(Screen.Employees.route) {
            EmployeesListScreen(onBackClick = { navController.popBackStack() })
        }
        composable(
            route = "employees/{companyId}",
            arguments = listOf(navArgument("companyId") { type = NavType.StringType })
        ) { backStackEntry ->
            val companyId = backStackEntry.arguments?.getString("companyId") ?: ""
            EmployeesListScreen(onBackClick = { navController.popBackStack() }, companyIdArg = companyId)
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
        composable(
            route = "vehicles/{companyId}",
            arguments = listOf(navArgument("companyId") { type = NavType.StringType })
        ) { backStackEntry ->
            val companyId = backStackEntry.arguments?.getString("companyId") ?: ""
            VehiclesListScreen(
                onBackClick = { navController.popBackStack() },
                onVehicleClick = { vehicleId -> navController.navigate("vehicleDetail/$vehicleId") },
                companyIdArg = companyId
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
                vehicleId = vehicleId,
                onSelectToUse = { id ->
                    navController.navigate("vehicleCheckout/$id")
                }
            )
        }

        // Registrar Salida
        composable(
            route = "vehicleCheckout/{vehicleId}",
            arguments = listOf(navArgument("vehicleId") { type = NavType.StringType })
        ) { backStackEntry ->
            val vehicleId = backStackEntry.arguments?.getString("vehicleId") ?: ""
            VehicleCheckoutScreen(
                onBackClick = { navController.popBackStack() },
                vehicleId = vehicleId,
                onCheckoutSuccess = { id ->
                    navController.navigate("vehicleInUse/$id")
                }
            )
        }

        // Vehículo en uso (temporizador y ayuda)
        composable(
            route = "vehicleInUse/{vehicleId}",
            arguments = listOf(navArgument("vehicleId") { type = NavType.StringType })
        ) { backStackEntry ->
            val vehicleId = backStackEntry.arguments?.getString("vehicleId") ?: ""
            VehicleInUseScreen(
                onBackClick = { navController.popBackStack() },
                vehicleId = vehicleId,
                onFinishClick = { id -> navController.navigate("vehicleReturn/$id") }
            )
        }

        // Registrar Devolución
        composable(
            route = "vehicleReturn/{vehicleId}",
            arguments = listOf(navArgument("vehicleId") { type = NavType.StringType })
        ) { backStackEntry ->
            val vehicleId = backStackEntry.arguments?.getString("vehicleId") ?: ""
            VehicleReturnScreen(
                onBackClick = { navController.popBackStack() },
                vehicleId = vehicleId,
                onReturnCompleted = {
                    navController.navigate(Screen.Vehicles.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }

        // Notificaciones
        composable(Screen.Notifications.route) {
            NotificationsScreen(onBackClick = { navController.popBackStack() })
        }
        composable(
            route = "notifications/{companyId}",
            arguments = listOf(navArgument("companyId") { type = NavType.StringType })
        ) { backStackEntry ->
            val companyId = backStackEntry.arguments?.getString("companyId") ?: ""
            NotificationsScreen(onBackClick = { navController.popBackStack() }, companyIdArg = companyId)
        }

        // Historial de Usos
        composable(Screen.UsageHistory.route) {
            UsageHistoryScreen(onBackClick = { navController.popBackStack() })
        }
        composable(
            route = "usageHistory/{companyId}",
            arguments = listOf(navArgument("companyId") { type = NavType.StringType })
        ) { backStackEntry ->
            val companyId = backStackEntry.arguments?.getString("companyId") ?: ""
            UsageHistoryScreen(onBackClick = { navController.popBackStack() }, companyIdArg = companyId)
        }
    }
}