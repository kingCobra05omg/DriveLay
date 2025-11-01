package com.DriveLay.JuanPerez.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.DriveLay.JuanPerez.firebase.FirebaseManager
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBackClick: () -> Unit,
    companyIdArg: String? = null
) {
    val firebaseManager = remember { FirebaseManager() }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var companyId by remember { mutableStateOf<String?>(companyIdArg) }
    var notifications by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }

    LaunchedEffect(companyIdArg) {
        val cid = companyIdArg ?: run {
            val res = firebaseManager.getCurrentCompanyId()
            res.fold(onSuccess = { it }, onFailure = { error = it.message; null })
        }
        companyId = cid
        if (cid == null) {
            loading = false
            error = "No perteneces a ninguna empresa"
        } else {
            // Verificar que el usuario sea Administrador o Sub-administrador antes de cargar notificaciones
            val roleRes = firebaseManager.getUserRoleInCompany(cid)
            roleRes.fold(onSuccess = { role ->
                if (role == "Administrador" || role == "Sub-administrador") {
                    val nres = firebaseManager.getCompanyNotifications(cid)
                    nres.fold(onSuccess = {
                        notifications = it
                        loading = false
                    }, onFailure = {
                        error = it.message
                        loading = false
                    })
                } else {
                    loading = false
                    error = "Solo el administrador o sub-administrador puede ver las notificaciones"
                }
            }, onFailure = { e ->
                loading = false
                error = e.message
            })
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notificaciones", fontWeight = FontWeight.SemiBold, color = Color.White) },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.Filled.ArrowBack, contentDescription = "Atrás", tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF003499))
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
            } else if (notifications.isEmpty()) {
                Text(text = "Sin notificaciones", color = Color.White, fontSize = 16.sp)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(notifications) { notif ->
                        NotificationItem(notif)
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationItem(notif: Map<String, Any>) {
    val type = notif["type"] as? String ?: ""
    val timestamp = (notif["timestamp"] as? Number)?.toLong() ?: 0L
    val userName = notif["userName"] as? String
    val userEmail = notif["userEmail"] as? String
    val vehiclePlate = notif["vehiclePlate"] as? String
    val vehicleName = notif["vehicleName"] as? String
    val message = notif["message"] as? String

    val (title, bgColor, icon) = when (type) {
        "employee_join" -> Triple("Empleado se unió a la empresa", Color(0xFF4CAF50), Icons.Filled.People)
        "vehicle_start" -> Triple("Retiro de vehículo", Color(0xFF2196F3), Icons.Filled.DirectionsCar)
        "vehicle_finish" -> Triple("Devolución de vehículo", Color(0xFF1976D2), Icons.Filled.DirectionsCar)
        "help_request" -> Triple("Solicitud de ayuda", Color(0xFFFFA000), Icons.Filled.Warning)
        else -> Triple("Actividad", Color(0xFF546E7A), Icons.Filled.Warning)
    }

    val sdf = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val timeText = try { sdf.format(Date(timestamp)) } catch (_: Exception) { "" }

    Card(colors = CardDefaults.cardColors(containerColor = bgColor), shape = RoundedCornerShape(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                if (!timeText.isNullOrBlank()) {
                    Text(text = timeText, color = Color.White)
                }
                if (!userName.isNullOrBlank()) {
                    Text(text = "Empleado: ${userName}", color = Color.White)
                } else if (!userEmail.isNullOrBlank()) {
                    Text(text = "Empleado: ${userEmail}", color = Color.White)
                }
                val vehicleLabel = listOfNotNull(vehicleName, vehiclePlate).joinToString(" - ")
                if (vehicleLabel.isNotBlank()) {
                    Text(text = "Vehículo: ${vehicleLabel}", color = Color.White)
                }
                if (!message.isNullOrBlank()) {
                    Text(text = message ?: "", color = Color.White)
                }
            }
        }
    }
}