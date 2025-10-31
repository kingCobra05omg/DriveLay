package com.DriveLay.JuanPerez.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.AirportShuttle
import androidx.compose.material3.*
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.shape.RoundedCornerShape
import com.DriveLay.JuanPerez.firebase.FirebaseManager
import com.DriveLay.JuanPerez.model.Vehicle
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleDetailScreen(
    onBackClick: () -> Unit,
    vehicleId: String
) {
    val firebaseManager = remember { FirebaseManager() }
    val scope = rememberCoroutineScope()
    val currentUserId = firebaseManager.getCurrentUser()?.uid
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var vehicle by remember { mutableStateOf<Vehicle?>(null) }
    var companyId by remember { mutableStateOf<String?>(null) }
    var isOwner by remember { mutableStateOf(false) }

    LaunchedEffect(vehicleId) {
        val comp = firebaseManager.getCurrentCompanyId()
        comp.fold(onSuccess = { cid ->
            companyId = cid
            if (cid == null) {
                loading = false
                error = "No perteneces a ninguna empresa"
            } else {
                // Determinar si el usuario es el owner (admin)
                val companyRes = firebaseManager.getCompanyData(cid)
                companyRes.fold(onSuccess = { data ->
                    val ownerId = data?.get("ownerId") as? String
                    isOwner = ownerId == currentUserId
                }, onFailure = {
                    isOwner = false
                })

                val res = firebaseManager.getVehicles(cid)
                res.fold(onSuccess = { list ->
                    vehicle = list.firstOrNull { it.id == vehicleId }
                    loading = false
                    if (vehicle == null) {
                        error = "Vehículo no encontrado"
                    }
                }, onFailure = {
                    loading = false
                    error = it.message
                })
            }
        }, onFailure = {
            loading = false
            error = it.message
        })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalles del Vehículo", color = Color.White, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Atrás", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF003499),
                    navigationIconContentColor = Color.White,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        if (loading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        if (error != null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(error ?: "Error")
            }
            return@Scaffold
        }

        val v = vehicle ?: return@Scaffold
        val desc = v.description ?: ""
        fun parseField(key: String): String? {
            val regex = Regex("$key:\\s*([^,]+)")
            return regex.find(desc)?.groupValues?.get(1)?.trim()
        }
        val tipo = parseField("Tipo") ?: ""
        val anio = parseField("Año") ?: ""
        val color = parseField("Color") ?: ""
        val nameParts = v.name.trim().split(" ")
        val marca = nameParts.firstOrNull() ?: v.name
        val modelo = nameParts.drop(1).joinToString(" ").ifBlank { v.name }

        // Estados de edición (inicializados desde los valores actuales)
        var showEdit by remember { mutableStateOf(false) }
        var editVehicleType by remember(v.id) { mutableStateOf(tipo.ifBlank { "Auto" }) }
        var editBrand by remember(v.id) { mutableStateOf(marca) }
        var editModel by remember(v.id) { mutableStateOf(modelo) }
        var editYear by remember(v.id) { mutableStateOf(anio) }
        var editColor by remember(v.id) { mutableStateOf(color) }
        var editPlate by remember(v.id) { mutableStateOf(v.plate ?: "") }
        var editError by remember { mutableStateOf<String?>(null) }
        var saving by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Encabezado con icono, nombre y placa
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Surface(shape = CircleShape, color = Color(0xFFE3F2FD)) {
                    Box(modifier = Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                        val icon = if ((tipo.lowercase()).contains("camión")) Icons.Filled.LocalShipping else Icons.Filled.DirectionsCar
                        Icon(icon, contentDescription = null, tint = Color(0xFF1565C0))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    v.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937)
                )
                Text(
                    v.plate ?: "",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                val statusText = v.status.ifBlank { "Activo" }
                val (chipBg, chipFg) = when (statusText) {
                    "En Uso" -> Color(0xFFDCFCE7) to Color(0xFF166534)
                    "Mantenimiento" -> Color(0xFFFEF3C7) to Color(0xFF92400E)
                    "Inactivo" -> Color(0xFFFEE2E2) to Color(0xFF991B1B)
                    else -> Color(0xFFDDEAFE) to Color(0xFF1E40AF)
                }
                AssistChip(
                    onClick = {},
                    label = { Text(statusText, color = chipFg, fontWeight = FontWeight.SemiBold) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = chipBg)
                )
            }

            // Información General
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Información General",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Tipo", color = Color(0xFF475569))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    tipo.ifBlank { "-" },
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Año", color = Color(0xFF475569))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    anio.ifBlank { "-" },
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Marca", color = Color(0xFF475569))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    marca.ifBlank { "-" },
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Modelo", color = Color(0xFF475569))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    modelo.ifBlank { "-" },
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Color", color = Color(0xFF475569))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    color.ifBlank { "-" },
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Ubicación Actual (placeholder)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Ubicación Actual",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "No disponible",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Botones de acción (sin funcionalidad por ahora)
            // Acciones según rol
            if (isOwner) {
                Button(
                    onClick = { showEdit = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF111827))
                ) { Text("Editar Vehículo") }
            } else {
                Button(
                    onClick = {
                        val cid = companyId
                        val vid = v.id
                        if (cid != null && vid != null) {
                            saving = true
                            val newStatus = "En Uso"
                            val newDesc = if (desc.contains("Asignado a:")) desc else listOf(desc, "Asignado a: ${currentUserId ?: ""}").filter { it.isNotBlank() }.joinToString(", ")
                            val updates = mutableMapOf<String, Any>(
                                "status" to newStatus,
                                "description" to newDesc
                            )
                            currentUserId?.let {
                                updates["assignedTo"] = it
                                updates["assignedAt"] = System.currentTimeMillis()
                            }
                            scope.launch {
                                firebaseManager.updateVehicle(cid, vid, updates).fold(
                                    onSuccess = {
                                        vehicle = v.copy(status = newStatus, description = newDesc, assignedTo = currentUserId, assignedAt = System.currentTimeMillis())
                                        saving = false
                                    },
                                    onFailure = {
                                        editError = it.message
                                        saving = false
                                    }
                                )
                            }
                        }
                    },
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Seleccionar para usar") }
            }
            OutlinedButton(onClick = { /* Navegar a historial en el futuro */ }, modifier = Modifier.fillMaxWidth()) { Text("Ver Historial de Mantenimiento") }
        }

        // Hoja inferior de edición (solo admin)
        if (showEdit) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(onDismissRequest = { showEdit = false }, sheetState = sheetState) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("Editar Vehículo", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        TextButton(onClick = { showEdit = false }) { Text("Cancelar") }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Tipo", color = Color(0xFF6B7280))
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = editVehicleType == "Auto", onClick = { editVehicleType = "Auto" }, label = { Text("Auto") }, leadingIcon = { Icon(Icons.Filled.DirectionsCar, contentDescription = null) })
                        FilterChip(selected = editVehicleType == "Camión", onClick = { editVehicleType = "Camión" }, label = { Text("Camión") }, leadingIcon = { Icon(Icons.Filled.LocalShipping, contentDescription = null) })
                        FilterChip(selected = editVehicleType == "Camioneta", onClick = { editVehicleType = "Camioneta" }, label = { Text("Camioneta") }, leadingIcon = { Icon(Icons.Filled.AirportShuttle, contentDescription = null) })
                    }
                    Spacer(Modifier.height(12.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))) {
                        Column(Modifier.fillMaxWidth().padding(12.dp)) {
                            OutlinedTextField(value = editPlate, onValueChange = { editPlate = it }, label = { Text("Matrícula") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp))
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(value = editBrand, onValueChange = { editBrand = it }, label = { Text("Marca") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp))
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(value = editModel, onValueChange = { editModel = it }, label = { Text("Modelo") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp))
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(value = editYear, onValueChange = { editYear = it }, label = { Text("Año") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(12.dp))
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(value = editColor, onValueChange = { editColor = it }, label = { Text("Color") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp))
                        }
                    }
                    if (editError != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(editError ?: "", color = Color(0xFFB00020))
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val cid = companyId
                            val vid = v.id
                            val yearInt = editYear.toIntOrNull()
                            if (cid != null && vid != null && editBrand.isNotBlank() && editModel.isNotBlank() && editPlate.isNotBlank() && editColor.isNotBlank() && yearInt != null) {
                                val newName = "${editBrand.trim()} ${editModel.trim()}"
                                val newDesc = "Tipo: ${editVehicleType.trim()}, Año: ${editYear.trim()}, Color: ${editColor.trim()}"
                                saving = true
                                scope.launch {
                                    firebaseManager.updateVehicle(cid, vid, mapOf("name" to newName, "plate" to editPlate.trim(), "description" to newDesc)).fold(
                                        onSuccess = {
                                            vehicle = v.copy(name = newName, plate = editPlate.trim(), description = newDesc)
                                            saving = false
                                            showEdit = false
                                        },
                                        onFailure = {
                                            editError = it.message
                                            saving = false
                                        }
                                    )
                                }
                            } else {
                                editError = "Completá todos los campos; Año debe ser número"
                            }
                        },
                        enabled = !saving,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Guardar Cambios") }
                }
            }
        }
    }
}