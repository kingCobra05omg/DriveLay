package com.DriveLay.JuanPerez.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.DriveLay.JuanPerez.firebase.FirebaseManager
import com.DriveLay.JuanPerez.model.Vehicle
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehiclesListScreen(
    onBackClick: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val firebaseManager = remember { FirebaseManager() }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var vehicles by remember { mutableStateOf<List<Vehicle>>(emptyList()) }
    var companyId by remember { mutableStateOf<String?>(null) }

    var showAddDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newPlate by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        scope.launch {
            val comp = firebaseManager.getCurrentCompanyId()
            comp.fold(onSuccess = { cid ->
                companyId = cid
                if (cid == null) {
                    loading = false
                    error = "No perteneces a ninguna empresa"
                } else {
                    val res = firebaseManager.getVehicles(cid)
                    res.fold(onSuccess = {
                        vehicles = it
                        loading = false
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
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Flota", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Añadir")
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
                        VehicleItem(vehicle)
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    val cid = companyId
                    if (!newName.isBlank() && !newPlate.isBlank() && cid != null) {
                        scope.launch {
                            val res = firebaseManager.addVehicle(cid, Vehicle(
                                name = newName,
                                plate = newPlate
                            ))
                            res.fold(onSuccess = { newId ->
                                vehicles = vehicles + Vehicle(
                                    id = newId,
                                    companyId = cid,
                                    name = newName,
                                    plate = newPlate
                                )
                                showAddDialog = false
                                newName = ""
                                newPlate = ""
                            }, onFailure = { e ->
                                error = e.message
                            })
                        }
                    }
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancelar") } },
            title = { Text("Añadir Vehículo") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Nombre") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPlate,
                        onValueChange = { newPlate = it },
                        label = { Text("Matrícula") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        )
    }
}

@Composable
private fun VehicleItem(vehicle: Vehicle) {
    ListItem(
        headlineContent = { Text(vehicle.name) },
        supportingContent = { Text(vehicle.plate) }
    )
    Divider()
}