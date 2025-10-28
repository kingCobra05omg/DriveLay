package com.DriveLay.JuanPerez.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import com.DriveLay.JuanPerez.firebase.FirebaseManager
import com.DriveLay.JuanPerez.model.Employee
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeesListScreen(
    onBackClick: () -> Unit
) {
    val firebaseManager = remember { FirebaseManager() }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var companyCode by remember { mutableStateOf<String?>(null) }
    var employees by remember { mutableStateOf<List<Employee>>(emptyList()) }
    var companyId by remember { mutableStateOf<String?>(null) }
    var logoUrl by remember { mutableStateOf<String?>(null) }
    var isOwner by remember { mutableStateOf(false) }
    var isSavingLogo by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var copied by remember { mutableStateOf(false) }

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

    LaunchedEffect(Unit) {
        val cidRes = firebaseManager.getCurrentCompanyId()
        cidRes.fold(onSuccess = { cid ->
            companyId = cid
            if (cid == null) {
                error = "No perteneces a ninguna empresa"
                loading = false
            } else {
                val companyRes = firebaseManager.getCompanyData(cid)
                companyRes.fold(onSuccess = { data ->
                    companyCode = data?.get("code") as? String
                    logoUrl = data?.get("logoUrl") as? String
                    val ownerId = data?.get("ownerId") as? String
                    isOwner = ownerId == firebaseManager.getCurrentUser()?.uid
                }, onFailure = {
                    error = it.message
                })

                val empRes = firebaseManager.getEmployees(cid)
                empRes.fold(onSuccess = {
                    employees = it
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

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Empleados", fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (loading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (error != null) {
                Text(text = error ?: "Error", color = Color.Red)
            } else {
                // Código de la empresa visible
                if (companyCode != null || logoUrl != null) {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9))) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.People, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Código de la empresa: ${companyCode ?: "-"}")
                                Spacer(modifier = Modifier.width(8.dp))
                                val clipboardManager = LocalClipboardManager.current
                                if (companyCode != null) {
                                    IconButton(onClick = {
                                        clipboardManager.setText(AnnotatedString(companyCode!!))
                                        scope.launch {
                                            copied = true
                                            delay(1500)
                                            copied = false
                                        }
                                    }) {
                                        Icon(Icons.Filled.ContentCopy, contentDescription = "Copiar código")
                                    }
                                }
                            }
                            if (copied) {
                                Text(text = "Código copiado", color = Color(0xFF16A34A), fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            if (logoUrl != null) {
                                AsyncImage(
                                    model = logoUrl,
                                    contentDescription = "Logo de la empresa",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                )
                            } else {
                                Text(text = "Sin imagen de empresa")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            if (isOwner) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Button(
                                        onClick = { imagePicker.launch("image/*") },
                                        enabled = !isSavingLogo
                                    ) {
                                        Text(if (isSavingLogo) "Subiendo..." else "Cambiar imagen")
                                    }
                                    if (isSavingLogo) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (employees.isEmpty()) {
                    Text(text = "No hay empleados registrados")
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        employees.forEach { emp ->
                            ListItem(
                                headlineContent = { Text(emp.name) },
                                supportingContent = { Text(emp.role) }
                            )
                            Divider()
                        }
                    }
                }
            }
        }
    }
}