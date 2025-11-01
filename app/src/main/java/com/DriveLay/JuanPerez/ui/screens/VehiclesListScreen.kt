package com.DriveLay.JuanPerez.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.AirportShuttle
import androidx.compose.material3.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.DriveLay.JuanPerez.firebase.FirebaseManager
import com.DriveLay.JuanPerez.model.Vehicle
import kotlinx.coroutines.launch
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehiclesListScreen(
    onBackClick: () -> Unit,
    onVehicleClick: (String) -> Unit = {},
    companyIdArg: String? = null
) {
    val scope = rememberCoroutineScope()
    val firebaseManager = remember { FirebaseManager() }
    val currentUserId = firebaseManager.getCurrentUser()?.uid
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var vehicles by remember { mutableStateOf<List<Vehicle>>(emptyList()) }
    var companyId by remember { mutableStateOf<String?>(null) }
    var isOwner by remember { mutableStateOf(false) }
    var isSubAdmin by remember { mutableStateOf(false) }

    var showAddDialog by remember { mutableStateOf(false) }
    var vehicleType by remember { mutableStateOf("Auto") }
    var newBrand by remember { mutableStateOf("") }
    var newModel by remember { mutableStateOf("") }
    var newYear by remember { mutableStateOf("") }
    var newColor by remember { mutableStateOf("") }
    var newPlate by remember { mutableStateOf("") }

    LaunchedEffect(companyIdArg) {
        scope.launch {
            val loadForCompany: suspend (String) -> Unit = { cid ->
                companyId = cid
                val companyRes = firebaseManager.getCompanyData(cid)
                companyRes.fold(onSuccess = { company ->
                    val ownerId = company?.get("ownerId") as? String
                    isOwner = ownerId == currentUserId
                }, onFailure = {
                    isOwner = false
                })
                // Resolver rol del usuario dentro de la empresa
                val roleRes = firebaseManager.getUserRoleInCompany(cid)
                roleRes.fold(onSuccess = { role ->
                    isSubAdmin = (role == "Sub-administrador")
                }, onFailure = {
                    isSubAdmin = false
                })
                val res = firebaseManager.getVehicles(cid)
                res.fold(onSuccess = {
                    vehicles = it
                    loading = false
                }, onFailure = {
                    loading = false
                    error = it.message
                })
            }

            if (companyIdArg != null) {
                loadForCompany(companyIdArg)
            } else {
                val comp = firebaseManager.getCurrentCompanyId()
                comp.fold(onSuccess = { cid ->
                    if (cid == null) {
                        loading = false
                        error = "No perteneces a ninguna empresa"
                    } else {
                        loadForCompany(cid)
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
                title = { Text("Flota", fontWeight = FontWeight.SemiBold, color = Color.White) },
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
        },
        floatingActionButton = {
            if (isOwner || isSubAdmin) {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Añadir")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (loading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (error != null) {
                Text(text = error ?: "Error")
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(vehicles) { vehicle ->
                        VehicleItem(vehicle = vehicle, onClick = { onVehicleClick(vehicle.id) })
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showAddDialog = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showAddDialog = false }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                    Text(
                        text = "Añadir Vehículo",
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { showAddDialog = false }) { Text("Cancelar") }
                }

                Spacer(Modifier.height(8.dp))
                Text("Tipo de Vehículo", color = Color(0xFF6B7280))
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = vehicleType == "Auto",
                        onClick = { vehicleType = "Auto" },
                        label = { Text("Auto") },
                        leadingIcon = { Icon(Icons.Filled.DirectionsCar, contentDescription = null) }
                    )
                    FilterChip(
                        selected = vehicleType == "Camión",
                        onClick = { vehicleType = "Camión" },
                        label = { Text("Camión") },
                        leadingIcon = { Icon(Icons.Filled.LocalShipping, contentDescription = null) }
                    )
                    FilterChip(
                        selected = vehicleType == "Camioneta",
                        onClick = { vehicleType = "Camioneta" },
                        label = { Text("Camioneta") },
                        leadingIcon = { Icon(Icons.Filled.AirportShuttle, contentDescription = null) }
                    )
                }

                Spacer(Modifier.height(16.dp))
                Text("Detalles del Vehículo", color = Color(0xFF6B7280))
                Spacer(Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = newPlate,
                            onValueChange = { newPlate = it },
                            label = { Text("Matrícula", color = Color(0xFF212121)) },
                            placeholder = { Text("ABC-123", color = Color(0xFF9E9E9E)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF212121),
                                unfocusedTextColor = Color(0xFF212121),
                                focusedLabelColor = Color(0xFF616161),
                                unfocusedLabelColor = Color(0xFF9E9E9E),
                                focusedContainerColor = Color(0xFFF1F5F9),
                                unfocusedContainerColor = Color(0xFFF1F5F9),
                                focusedBorderColor = Color(0xFFCBD5E1),
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            )
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = newBrand,
                            onValueChange = { newBrand = it },
                            label = { Text("Marca", color = Color(0xFF212121)) },
                            placeholder = { Text("Ej: Ford", color = Color(0xFF9E9E9E)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF212121),
                                unfocusedTextColor = Color(0xFF212121),
                                focusedLabelColor = Color(0xFF616161),
                                unfocusedLabelColor = Color(0xFF9E9E9E),
                                focusedContainerColor = Color(0xFFF1F5F9),
                                unfocusedContainerColor = Color(0xFFF1F5F9),
                                focusedBorderColor = Color(0xFFCBD5E1),
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            )
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = newModel,
                            onValueChange = { newModel = it },
                            label = { Text("Modelo", color = Color(0xFF212121)) },
                            placeholder = { Text("Ej: Ranger", color = Color(0xFF9E9E9E)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF212121),
                                unfocusedTextColor = Color(0xFF212121),
                                focusedLabelColor = Color(0xFF616161),
                                unfocusedLabelColor = Color(0xFF9E9E9E),
                                focusedContainerColor = Color(0xFFF1F5F9),
                                unfocusedContainerColor = Color(0xFFF1F5F9),
                                focusedBorderColor = Color(0xFFCBD5E1),
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            )
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = newYear,
                            onValueChange = { newYear = it },
                            label = { Text("Año", color = Color(0xFF212121)) },
                            placeholder = { Text("Ej: 2023", color = Color(0xFF9E9E9E)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF212121),
                                unfocusedTextColor = Color(0xFF212121),
                                focusedLabelColor = Color(0xFF616161),
                                unfocusedLabelColor = Color(0xFF9E9E9E),
                                focusedContainerColor = Color(0xFFF1F5F9),
                                unfocusedContainerColor = Color(0xFFF1F5F9),
                                focusedBorderColor = Color(0xFFCBD5E1),
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            )
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = newColor,
                            onValueChange = { newColor = it },
                            label = { Text("Color", color = Color(0xFF212121)) },
                            placeholder = { Text("Ej: Azul", color = Color(0xFF9E9E9E)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF212121),
                                unfocusedTextColor = Color(0xFF212121),
                                focusedLabelColor = Color(0xFF616161),
                                unfocusedLabelColor = Color(0xFF9E9E9E),
                                focusedContainerColor = Color(0xFFF1F5F9),
                                unfocusedContainerColor = Color(0xFFF1F5F9),
                                focusedBorderColor = Color(0xFFCBD5E1),
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            )
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        val cid = companyId
                        if (!isOwner && !isSubAdmin) {
                            error = "Solo sub-administrador o administrador puede añadir vehículos"
                            return@Button
                        }
                        val yearInt = newYear.toIntOrNull()
                        if (cid != null &&
                            newBrand.isNotBlank() &&
                            newModel.isNotBlank() &&
                            yearInt != null &&
                            newColor.isNotBlank() &&
                            newPlate.isNotBlank()
                        ) {
                            val composedName = "${newBrand.trim()} ${newModel.trim()}"
                            val desc = "Tipo: ${vehicleType.trim()}, Año: ${newYear.trim()}, Color: ${newColor.trim()}"
                            scope.launch {
                                val res = firebaseManager.addVehicle(
                                    cid,
                                    Vehicle(
                                        companyId = cid,
                                        name = composedName,
                                        plate = newPlate.trim(),
                                        description = desc
                                    )
                                )
                                res.fold(onSuccess = { newId ->
                                    vehicles = vehicles + Vehicle(
                                        id = newId,
                                        companyId = cid,
                                        name = composedName,
                                        plate = newPlate.trim(),
                                        description = desc
                                    )
                                    showAddDialog = false
                                    vehicleType = "Auto"
                                    newBrand = ""
                                    newModel = ""
                                    newYear = ""
                                    newColor = ""
                                    newPlate = ""
                                }, onFailure = { e ->
                                    error = e.message
                                })
                            }
                        } else {
                            error = "Completá matrícula, marca, modelo, año (número) y color"
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Agregar Vehículo")
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun VehicleItem(vehicle: Vehicle, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(vehicle.name) },
        supportingContent = { Text(vehicle.plate) },
        trailingContent = { StatusChip(status = vehicle.status) },
        modifier = Modifier.clickable(onClick = onClick)
    )
    Divider()
}

@Composable
private fun StatusChip(status: String) {
    val (bg, fg) = when (status) {
        "En Uso" -> Color(0xFFDCFCE7) to Color(0xFF166534) // verde
        "Mantenimiento" -> Color(0xFFFEF3C7) to Color(0xFF92400E) // amarillo
        "Inactivo" -> Color(0xFFFEE2E2) to Color(0xFF991B1B) // rojo
        else -> Color(0xFFDDEAFE) to Color(0xFF1E40AF) // azul por defecto (Activo)
    }
    AssistChip(
        onClick = {},
        label = { Text(status, color = fg) },
        colors = AssistChipDefaults.assistChipColors(containerColor = bg)
    )
}