package com.DriveLay.JuanPerez.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.DriveLay.JuanPerez.firebase.FirebaseManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onBackClick: () -> Unit,
    onManageEmployees: () -> Unit,
    onManageFleet: () -> Unit,
    onViewUsageHistory: () -> Unit,
    companyIdArg: String? = null,
) {
    val scope = rememberCoroutineScope()
    val firebaseManager = remember { FirebaseManager() }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var employeesCount by remember { mutableStateOf(0) }
    var vehiclesCount by remember { mutableStateOf(0) }

    LaunchedEffect(companyIdArg) {
        scope.launch {
            if (companyIdArg != null) {
                val companyRes = firebaseManager.getCompanyData(companyIdArg)
                var membersCount = 0
                companyRes.fold(onSuccess = { data ->
                    membersCount = ((data?.get("members") as? List<*>)?.size) ?: 0
                }, onFailure = { /* mantener 0 */ })

                val empRes = firebaseManager.getEmployees(companyIdArg)
                val vehRes = firebaseManager.getVehicles(companyIdArg)
                empRes.fold(onSuccess = { employeesCount = maxOf(membersCount, it.size) }, onFailure = { employeesCount = membersCount })
                vehRes.fold(onSuccess = { vehiclesCount = it.size }, onFailure = { error = it.message })
                loading = false
            } else {
                val cidRes = firebaseManager.getCurrentCompanyId()
                cidRes.fold(onSuccess = { cid ->
                    if (cid == null) {
                        loading = false
                        error = "No perteneces a ninguna empresa"
                    } else {
                        val companyRes = firebaseManager.getCompanyData(cid)
                        var membersCount = 0
                        companyRes.fold(onSuccess = { data ->
                            membersCount = ((data?.get("members") as? List<*>)?.size) ?: 0
                        }, onFailure = { /* ignore, mantener 0 */ })

                        val empRes = firebaseManager.getEmployees(cid)
                        val vehRes = firebaseManager.getVehicles(cid)
                        empRes.fold(onSuccess = { employeesCount = maxOf(membersCount, it.size) }, onFailure = { employeesCount = membersCount })
                        vehRes.fold(onSuccess = { vehiclesCount = it.size }, onFailure = { error = it.message })
                        loading = false
                    }
                }, onFailure = {
                    loading = false
                    error = it.message
                })
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Empresa", fontWeight = FontWeight.SemiBold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) { Icon(Icons.Filled.ArrowBack, contentDescription = "Atrás", tint = Color.White) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF003499),
                    navigationIconContentColor = Color.White,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF003499))
                .padding(16.dp)
        ) {
            if (loading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else if (error != null) {
                Text(text = error ?: "Error", color = Color.White)
            } else {
                Text(text = "Bienvenido, Admin", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                DashboardCard(
                    title = "Gestionar Empleados",
                    subtitle = "$employeesCount Empleados Activos",
                    icon = { Icon(Icons.Filled.People, contentDescription = null) },
                    onClick = onManageEmployees
                )
                Spacer(modifier = Modifier.height(8.dp))
                DashboardCard(
                    title = "Gestionar Flota",
                    subtitle = "$vehiclesCount Vehículos Operativos",
                    icon = { Icon(Icons.Filled.DirectionsCar, contentDescription = null) },
                    onClick = onManageFleet
                )
                Spacer(modifier = Modifier.height(8.dp))
                DashboardCard(
                    title = "Historial de Usos",
                    subtitle = "Ver duración y distancia recorrida",
                    icon = { Icon(Icons.Filled.DirectionsCar, contentDescription = null) },
                    onClick = onViewUsageHistory
                )
            }
        }
    }
}

@Composable
private fun DashboardCard(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2196F3)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.White)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = subtitle, fontSize = 14.sp, color = Color.White)
            }
        }
    }
}