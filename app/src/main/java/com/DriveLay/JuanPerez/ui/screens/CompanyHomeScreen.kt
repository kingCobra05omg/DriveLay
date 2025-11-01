package com.DriveLay.JuanPerez.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.DriveLay.JuanPerez.firebase.FirebaseManager
import com.DriveLay.JuanPerez.R
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyHomeScreen(
    onBackClick: () -> Unit,
    onGoToAdmin: () -> Unit,
    companyIdArg: String? = null
) {
    val scope = rememberCoroutineScope()
    val firebaseManager = remember { FirebaseManager() }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var companyName by remember { mutableStateOf<String?>(null) }
    var employeesCount by remember { mutableStateOf<Int?>(null) }
    var vehiclesCount by remember { mutableStateOf<Int?>(null) }
    // Conteo actual (activos) obtenido desde colecciones
    var employeesActive by remember { mutableStateOf(0) }
    var vehiclesActive by remember { mutableStateOf(0) }
    var isOwner by remember { mutableStateOf(false) }
    var companyId by remember { mutableStateOf<String?>(companyIdArg) }
    var logoUrl by remember { mutableStateOf<String?>(null) }
    val currentUserId = firebaseManager.getCurrentUser()?.uid
    var showEditDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newEmployees by remember { mutableStateOf("") }
    var newVehicles by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var isSavingLogo by remember { mutableStateOf(false) }
    var attemptedDefaultUpload by remember { mutableStateOf(false) }
    // Imagen por defecto servida desde entorno local de desarrollo
    val defaultCompanyImageUrlDev = "http://10.0.2.2:5501/Imagenes/FondoBody.jpg"

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null && companyId != null && isOwner) {
            scope.launch {
                isSavingLogo = true
                firebaseManager.uploadCompanyImage(companyId!!, uri).fold(
                    onSuccess = { url ->
                        firebaseManager.updateCompany(companyId!!, mapOf("logoUrl" to url)).fold(
                            onSuccess = {
                                logoUrl = url
                            },
                            onFailure = { e ->
                                error = e.message
                            }
                        )
                    },
                    onFailure = { e ->
                        error = e.message
                    }
                )
                isSavingLogo = false
            }
        }
    }

    LaunchedEffect(companyIdArg) {
        scope.launch {
            if (companyIdArg != null) {
                companyId = companyIdArg
                val dataRes = firebaseManager.getCompanyData(companyIdArg)
                dataRes.fold(onSuccess = { data ->
                    companyName = data?.get("name") as? String
                    val ownerId = data?.get("ownerId") as? String
                    logoUrl = data?.get("logoUrl") as? String
                    employeesCount = (data?.get("employees") as? Number)?.toInt()
                    vehiclesCount = (data?.get("vehicles") as? Number)?.toInt()
                    isOwner = ownerId == currentUserId
                    // Conteo base por miembros
                    val membersCount = ((data?.get("members") as? List<*>)?.size) ?: 0
                    employeesActive = membersCount
                    // Obtener conteos actuales (activos) desde colección empleados y tomar el mayor
                    val empRes = firebaseManager.getEmployees(companyIdArg)
                    empRes.fold(
                        onSuccess = { employeesActive = maxOf(membersCount, it.size) },
                        onFailure = { employeesActive = membersCount }
                    )
                    val vehRes = firebaseManager.getVehicles(companyIdArg)
                    vehRes.fold(onSuccess = { vehiclesActive = it.size }, onFailure = { /* mantener default */ })

                    // Subir y fijar imagen por defecto si no hay logo
                    if (!attemptedDefaultUpload && (logoUrl == null || logoUrl!!.isBlank()) && isOwner) {
                        attemptedDefaultUpload = true
                        isSavingLogo = true
                        scope.launch {
                            firebaseManager.uploadDefaultCompanyImageFromUrl(companyIdArg, defaultCompanyImageUrlDev).fold(
                                onSuccess = { url ->
                                    firebaseManager.updateCompany(companyIdArg, mapOf("logoUrl" to url)).fold(
                                        onSuccess = {
                                            logoUrl = url
                                        },
                                        onFailure = { e ->
                                            error = e.message
                                        }
                                    )
                                },
                                onFailure = { e ->
                                    error = e.message
                                }
                            )
                            isSavingLogo = false
                        }
                    }
                    loading = false
                }, onFailure = {
                    error = it.message
                    loading = false
                })
            } else {
                val cidRes = firebaseManager.getCurrentCompanyId()
                cidRes.fold(onSuccess = { cid ->
                    if (cid == null) {
                        error = "No perteneces a ninguna empresa"
                        loading = false
                    } else {
                        companyId = cid
                        val dataRes = firebaseManager.getCompanyData(cid)
                        dataRes.fold(onSuccess = { data ->
                            companyName = data?.get("name") as? String
                            val ownerId = data?.get("ownerId") as? String
                            logoUrl = data?.get("logoUrl") as? String
                            employeesCount = (data?.get("employees") as? Number)?.toInt()
                            vehiclesCount = (data?.get("vehicles") as? Number)?.toInt()
                            isOwner = ownerId == currentUserId
                            // Conteo base por miembros
                            val membersCount = ((data?.get("members") as? List<*>)?.size) ?: 0
                            employeesActive = membersCount
                            // Obtener conteos actuales (activos) desde colección empleados y tomar el mayor
                            val empRes = firebaseManager.getEmployees(cid)
                            empRes.fold(
                                onSuccess = { employeesActive = maxOf(membersCount, it.size) },
                                onFailure = { employeesActive = membersCount }
                            )
                            val vehRes = firebaseManager.getVehicles(cid)
                            vehRes.fold(onSuccess = { vehiclesActive = it.size }, onFailure = { /* mantener default */ })

                            // Subir y fijar imagen por defecto si no hay logo
                            if (!attemptedDefaultUpload && (logoUrl == null || logoUrl!!.isBlank()) && isOwner && cid != null) {
                                attemptedDefaultUpload = true
                                isSavingLogo = true
                                scope.launch {
                                    firebaseManager.uploadDefaultCompanyImageFromUrl(cid, defaultCompanyImageUrlDev).fold(
                                        onSuccess = { url ->
                                            firebaseManager.updateCompany(cid, mapOf("logoUrl" to url)).fold(
                                                onSuccess = {
                                                    logoUrl = url
                                                },
                                                onFailure = { e ->
                                                    error = e.message
                                                }
                                            )
                                        },
                                        onFailure = { e ->
                                            error = e.message
                                        }
                                    )
                                    isSavingLogo = false
                                }
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
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Inicio", fontWeight = FontWeight.SemiBold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                },
                actions = {
                    if (isOwner) {
                        TextButton(onClick = onGoToAdmin, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) { Text("Administrar") }
                        TextButton(
                            onClick = {
                                // Prefill values
                                newName = companyName ?: ""
                                newEmployees = employeesCount?.toString() ?: ""
                                newVehicles = vehiclesCount?.toString() ?: ""
                                showEditDialog = true
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                        ) { Text("Editar") }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF003499),
                    navigationIconContentColor = Color.White,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier
            .padding(padding)
            .fillMaxSize()) {
            when {
                loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                error != null -> Text(text = error ?: "Error", modifier = Modifier.align(Alignment.Center))
                else -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(Modifier.height(24.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .padding(horizontal = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = (logoUrl ?: defaultCompanyImageUrlDev),
                                    contentDescription = "Foto de la empresa",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(12.dp)
                                ) {
                                    Text(text = companyName ?: "Mi Empresa", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF212121))
                                    Spacer(Modifier.height(4.dp))
                                    Text(text = if (isOwner) "Rol: Administrador" else "Rol: Miembro")
                                }

                                if (isOwner) {
                                    // Botón para cambiar imagen del logo
                                    FilledTonalButton(
                                        onClick = { imagePicker.launch("image/*") },
                                        enabled = !isSavingLogo,
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(8.dp)
                                    ) {
                                        Text(if (isSavingLogo) "Subiendo..." else "Cambiar imagen")
                                    }
                                }
                            }
                        }
                        
                        Spacer(Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(90.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text("Empleados", fontSize = 14.sp, color = Color(0xFF003499))
                                    Spacer(Modifier.height(4.dp))
                                    Text("${employeesActive}/${employeesCount ?: 0}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF212121))
                                }
                            }

                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(90.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text("Vehículos", fontSize = 14.sp, color = Color(0xFF003499))
                                    Spacer(Modifier.height(4.dp))
                                    Text("${vehiclesActive}/${vehiclesCount ?: 0}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF212121))
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showEditDialog) {
            AlertDialog(
                onDismissRequest = { if (!saving) showEditDialog = false },
                title = { Text("Editar Empresa") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text("Nombre") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newEmployees,
                            onValueChange = { newEmployees = it },
                            label = { Text("Cantidad de Empleados") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newVehicles,
                            onValueChange = { newVehicles = it },
                            label = { Text("Cantidad de Vehículos") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(enabled = !saving, onClick = {
                        if (!isOwner) return@TextButton
                        val cid = companyId ?: return@TextButton
                        val updates = mutableMapOf<String, Any?>()
                        updates["name"] = newName
                        val emp = newEmployees.toIntOrNull()
                        val veh = newVehicles.toIntOrNull()
                        if (emp != null) updates["employees"] = emp
                        if (veh != null) updates["vehicles"] = veh
                        saving = true
                        scope.launch {
                            firebaseManager.updateCompany(cid, updates).fold(
                                onSuccess = {
                                    companyName = newName
                                    if (emp != null) employeesCount = emp
                                    if (veh != null) vehiclesCount = veh
                                    saving = false
                                    showEditDialog = false
                                },
                                onFailure = {
                                    error = it.message
                                    saving = false
                                }
                            )
                        }
                    }) {
                        Text("Guardar")
                    }
                },
                dismissButton = {
                    TextButton(enabled = !saving, onClick = { showEditDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}