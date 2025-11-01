package com.DriveLay.JuanPerez.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.DriveLay.JuanPerez.firebase.FirebaseManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleInUseScreen(
    onBackClick: () -> Unit,
    vehicleId: String,
    onFinishClick: (String) -> Unit
) {
    val firebaseManager = remember { FirebaseManager() }
    val scope = rememberCoroutineScope()
    var companyId by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    var vehicleName by remember { mutableStateOf("") }
    var vehiclePlate by remember { mutableStateOf("") }
    var startAt by remember { mutableStateOf<Long?>(null) }
    var startKm by remember { mutableStateOf<Int?>(null) }

    var elapsedMs by remember { mutableStateOf(0L) }
    var helpSending by remember { mutableStateOf(false) }
    var helpMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(vehicleId) {
        scope.launch {
            val cidRes = firebaseManager.getCurrentCompanyId()
            cidRes.fold(onSuccess = { cid ->
                if (cid == null) {
                    loading = false
                    error = "No perteneces a ninguna empresa"
                } else {
                    companyId = cid
                    val vehRes = firebaseManager.getVehicle(cid, vehicleId)
                    vehRes.fold(onSuccess = { veh ->
                        if (veh == null) {
                            error = "Vehículo no encontrado"
                        } else {
                            vehicleName = veh.name
                            vehiclePlate = veh.plate
                            startAt = veh.assignedAt ?: System.currentTimeMillis()
                            startKm = veh.startKm
                        }
                        loading = false
                    }, onFailure = {
                        error = it.message
                        loading = false
                    })
                }
            }, onFailure = {
                error = it.message
                loading = false
            })
        }
    }

    LaunchedEffect(startAt) {
        val s = startAt ?: return@LaunchedEffect
        while (true) {
            elapsedMs = System.currentTimeMillis() - s
            delay(1000)
        }
    }

    fun formatTwo(n: Long): String = n.toString().padStart(2, '0')
    val hours = formatTwo(elapsedMs / (1000 * 60 * 60))
    val minutes = formatTwo((elapsedMs / (1000 * 60)) % 60)
    val seconds = formatTwo((elapsedMs / 1000) % 60)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vehículo en Uso", color = Color.White, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Atrás", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF003499))
            )
        }
    ) { padding ->
        if (loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .background(Color(0xFFF8FAFC)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text("Tiempo en Uso", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.Black)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TimeBox(label = "Horas", value = hours)
                TimeBox(label = "Minutos", value = minutes)
                TimeBox(label = "Segundos", value = seconds)
            }

            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(vehicleName, fontWeight = FontWeight.SemiBold, color = Color.Black)
                    Text(vehiclePlate, color = Color.Black)
                    Divider()
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Hora de inicio", color = Color.Black)
                            val startText = startAt?.let { java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(java.util.Date(it)) } ?: "-"
                            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFF8FAFC)) {
                                Text(startText, modifier = Modifier.padding(8.dp), color = Color.Black)
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Km inicial", color = Color.Black)
                            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFF8FAFC)) {
                                Text(startKm?.toString() ?: "-", modifier = Modifier.padding(8.dp), color = Color.Black)
                            }
                        }
                    }
                }
            }

            if (!error.isNullOrBlank()) Text(error ?: "", color = Color(0xFFB00020))
            if (!helpMessage.isNullOrBlank()) Text(helpMessage ?: "", color = Color(0xFF166534))

            OutlinedButton(
                onClick = {
                    val cid = companyId ?: return@OutlinedButton
                    helpSending = true
                    scope.launch {
                        firebaseManager.reportAccidentAlert(cid, vehicleId, "Accidente reportado por el empleado").fold(
                            onSuccess = {
                                helpMessage = "Se notificó al administrador de la flota."
                                helpSending = false
                            },
                            onFailure = { e ->
                                error = e.message
                                helpSending = false
                            }
                        )
                    }
                },
                enabled = !helpSending,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF003499))
            ) {
                Text("Ayuda (Accidente)")
            }

            Button(
                onClick = { onFinishClick(vehicleId) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107), contentColor = Color(0xFF111827))
            ) {
                Text("Finalizar Uso")
            }
        }
    }
}

@Composable
private fun TimeBox(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shape = RoundedCornerShape(12.dp), color = Color.White) {
            Text(value, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.Black)
        }
        Spacer(Modifier.height(8.dp))
        Text(label, color = Color.Black)
    }
}