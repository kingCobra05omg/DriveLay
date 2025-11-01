package com.DriveLay.JuanPerez.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.DriveLay.JuanPerez.R
import com.DriveLay.JuanPerez.firebase.FirebaseManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleReturnScreen(
    onBackClick: () -> Unit,
    vehicleId: String,
    onReturnCompleted: () -> Unit
) {
    val firebaseManager = remember { FirebaseManager() }
    val scope = rememberCoroutineScope()
    var companyId by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    var vehicleName by remember { mutableStateOf("") }
    var vehiclePlate by remember { mutableStateOf("") }
    var startAt by remember { mutableStateOf<Long?>(null) }

    val now = remember { Date() }
    val timeStr = remember(now) { SimpleDateFormat("hh:mm a", Locale.getDefault()).format(now) }

    var endKm by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }

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
                            startAt = veh.assignedAt
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registrar Devolución", color = Color.White, fontWeight = FontWeight.SemiBold) },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Imagen genérica del vehículo (placeholder)
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(painter = painterResource(id = R.drawable.welcome_image), contentDescription = null, modifier = Modifier.height(140.dp).fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                    Text(vehicleName, fontWeight = FontWeight.SemiBold, color = Color.Black)
                    Text("Matrícula ${vehiclePlate}", color = Color.Black)
                }
            }

            OutlinedTextField(
                value = timeStr,
                onValueChange = {},
                label = { Text("Hora de Devolución", color = Color.Black) },
                trailingIcon = { Icon(Icons.Filled.Schedule, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    disabledTextColor = Color.Black,
                    focusedLabelColor = Color.Black,
                    unfocusedLabelColor = Color.Black,
                    disabledLabelColor = Color.Black,
                    focusedTrailingIconColor = Color.Black,
                    unfocusedTrailingIconColor = Color.Black,
                    disabledTrailingIconColor = Color.Black,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    disabledContainerColor = Color.White
                )
            )

            // Resumen de inicio y duración total
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val startText = startAt?.let { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(it)) } ?: "-"
                    Text("Hora de inicio: $startText", color = Color.Black)
                    val endText = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(now)
                    Text("Hora de fin: $endText", color = Color.Black)
                    val durationMs = startAt?.let { now.time - it } ?: 0L
                    fun two(n: Long) = n.toString().padStart(2, '0')
                    val h = two(durationMs / (1000 * 60 * 60))
                    val m = two((durationMs / (1000 * 60)) % 60)
                    val s = two((durationMs / 1000) % 60)
                    Text("Tiempo total: $h:$m:$s", fontWeight = FontWeight.SemiBold, color = Color.Black)
                }
            }

            OutlinedTextField(
                value = endKm,
                onValueChange = { endKm = it.filter { ch -> ch.isDigit() } },
                label = { Text("Kilometraje Final", color = Color.Black) },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedLabelColor = Color.Black,
                    unfocusedLabelColor = Color.Black,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notas / Observaciones", color = Color.Black) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedLabelColor = Color.Black,
                    unfocusedLabelColor = Color.Black,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            if (!error.isNullOrBlank()) Text(error ?: "", color = Color(0xFFB00020))

            Button(
                onClick = {
                    val cid = companyId
                    val km = endKm.toIntOrNull()
                    if (cid != null && km != null) {
                        saving = true
                        scope.launch {
                            firebaseManager.finishVehicleUsage(cid, vehicleId, km, notes.ifBlank { null }).fold(
                                onSuccess = {
                                    saving = false
                                    onReturnCompleted()
                                },
                                onFailure = { e ->
                                    error = e.message
                                    saving = false
                                }
                            )
                        }
                    } else {
                        error = "Ingresá un kilometraje final válido"
                    }
                },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107), contentColor = Color(0xFF111827))
            ) {
                Text("Confirmar Devolución")
            }
        }
    }
}