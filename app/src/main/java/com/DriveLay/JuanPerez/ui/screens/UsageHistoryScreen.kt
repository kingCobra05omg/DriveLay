package com.DriveLay.JuanPerez.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
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
fun UsageHistoryScreen(
    onBackClick: () -> Unit,
    companyIdArg: String? = null
) {
    val firebaseManager = remember { FirebaseManager() }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var companyId by remember { mutableStateOf<String?>(companyIdArg) }
    var usages by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }

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
            val ures = firebaseManager.getCompanyUsageHistory(cid)
            ures.fold(onSuccess = {
                usages = it
                loading = false
            }, onFailure = {
                error = it.message
                loading = false
            })
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial de Usos", fontWeight = FontWeight.SemiBold, color = Color.White) },
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
            } else if (usages.isEmpty()) {
                Text(text = "Sin registros", color = Color.White, fontSize = 16.sp)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(usages) { usage ->
                        UsageItem(usage)
                    }
                }
            }
        }
    }
}

@Composable
private fun UsageItem(usage: Map<String, Any>) {
    val userName = usage["userName"] as? String
    val userEmail = usage["userEmail"] as? String
    val vehiclePlate = usage["vehiclePlate"] as? String
    val vehicleName = usage["vehicleName"] as? String
    val startAt = (usage["startAt"] as? Number)?.toLong()
    val endAt = (usage["endAt"] as? Number)?.toLong()
    val durationMs = (usage["durationMs"] as? Number)?.toLong() ?: 0L
    val distanceKm = (usage["distanceKm"] as? Number)?.toInt() ?: 0

    val sdf = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val startText = startAt?.let { runCatching { sdf.format(Date(it)) }.getOrNull() } ?: ""
    val endText = endAt?.let { runCatching { sdf.format(Date(it)) }.getOrNull() } ?: ""
    val durationText = formatDuration(durationMs)
    val vehicleLabel = listOfNotNull(vehicleName, vehiclePlate).joinToString(" - ")

    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF2196F3)), shape = RoundedCornerShape(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (vehicleLabel.isNotBlank()) {
                    Text(text = vehicleLabel, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
                if (!userName.isNullOrBlank()) {
                    Text(text = "Empleado: ${userName}", color = Color.White)
                } else if (!userEmail.isNullOrBlank()) {
                    Text(text = "Empleado: ${userEmail}", color = Color.White)
                }
                if (startText.isNotBlank()) {
                    Text(text = "Inicio: ${startText}", color = Color.White)
                }
                if (endText.isNotBlank()) {
                    Text(text = "Fin: ${endText}", color = Color.White)
                }
                Text(text = "Duración: ${durationText}", color = Color.White)
                Text(text = "Distancia: ${distanceKm} km", color = Color.White)
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return String.format("%02d:%02d:%02d", h, m, s)
}