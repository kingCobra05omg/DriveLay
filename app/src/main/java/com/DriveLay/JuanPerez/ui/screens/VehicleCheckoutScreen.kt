package com.DriveLay.JuanPerez.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.DriveLay.JuanPerez.firebase.FirebaseManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleCheckoutScreen(
    onBackClick: () -> Unit,
    vehicleId: String,
    onCheckoutSuccess: (String) -> Unit
) {
    val firebaseManager = remember { FirebaseManager() }
    val scope = rememberCoroutineScope()
    var companyId by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    var vehicleName by remember { mutableStateOf("") }
    var vehiclePlate by remember { mutableStateOf("") }

    val now = remember { Date() }
    val dateStr = remember(now) { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(now) }
    val timeStr = remember(now) { SimpleDateFormat("hh:mm a", Locale.getDefault()).format(now) }

    var currentKm by remember { mutableStateOf("") }
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
                title = { Text("Registrar Salida", color = Color.White, fontWeight = FontWeight.SemiBold) },
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
                .background(Color.White),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "Vehículo: ${vehiclePlate.ifBlank { "-" }}", fontWeight = FontWeight.Medium, color = Color.Black)
            Text(text = vehicleName, color = Color.Black)

            OutlinedTextField(
                value = dateStr,
                onValueChange = {},
                label = { Text("Fecha de Salida", color = Color.Black) },
                trailingIcon = { Icon(Icons.Filled.CalendarToday, contentDescription = null) },
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

            OutlinedTextField(
                value = timeStr,
                onValueChange = {},
                label = { Text("Hora de Salida", color = Color.Black) },
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

            OutlinedTextField(
                value = currentKm,
                onValueChange = { currentKm = it.filter { ch -> ch.isDigit() } },
                label = { Text("Kilometraje Actual (km)", color = Color.Black) },
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

            if (!error.isNullOrBlank()) {
                Text(error ?: "", color = Color(0xFFB00020))
            }

            Button(
                onClick = {
                    val cid = companyId
                    val km = currentKm.toIntOrNull()
                    if (cid != null && km != null) {
                        saving = true
                        scope.launch {
                            firebaseManager.startVehicleUsage(cid, vehicleId, km).fold(
                                onSuccess = {
                                    saving = false
                                    onCheckoutSuccess(vehicleId)
                                },
                                onFailure = { e ->
                                    error = e.message
                                    saving = false
                                }
                            )
                        }
                    } else {
                        error = "Ingresá un kilometraje válido"
                    }
                },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107), contentColor = Color(0xFF111827))
            ) {
                Text("Confirmar Salida")
            }
        }
    }
}