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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import com.DriveLay.JuanPerez.firebase.FirebaseManager
import com.DriveLay.JuanPerez.model.Employee
import com.DriveLay.JuanPerez.model.Invitation
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import android.util.Patterns

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
    var inviteEmail by remember { mutableStateOf("") }
    var inviteRole by remember { mutableStateOf("Empleado") }
    var isSending by remember { mutableStateOf(false) }
    var invitations by remember { mutableStateOf<List<Invitation>>(emptyList()) }

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

                val invRes = firebaseManager.getInvitations(cid)
                invRes.fold(onSuccess = {
                    invitations = it
                }, onFailure = {
                    // no bloquear por error de invitaciones
                })
            }
        }, onFailure = {
            error = it.message
            loading = false
        })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Empleados", fontSize = 18.sp, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
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
                // Código de la empresa visible y edición de imagen
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

                // Invitación de nuevos empleados
                if (isOwner && companyId != null) {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "Invitar Nuevo Empleado", color = Color(0xFF212121), fontSize = 18.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = inviteEmail,
                                onValueChange = { inviteEmail = it },
                                label = { Text("Correo Electrónico del Empleado", color = Color(0xFF212121)) },
                                placeholder = { Text("ejemplo@empresa.com", color = Color(0xFF9E9E9E)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color(0xFF212121),
                                    unfocusedTextColor = Color(0xFF212121),
                                    focusedContainerColor = Color(0xFFF1F5F9),
                                    unfocusedContainerColor = Color(0xFFF1F5F9),
                                    focusedBorderColor = Color(0xFF90CAF9),
                                    unfocusedBorderColor = Color(0xFFB0BEC5),
                                    cursorColor = Color(0xFF003499)
                                )
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val selectedColor = Color(0xFF2196F3)
                                val unselectedColor = Color(0xFFE0E0E0)
                                Button(
                                    onClick = { inviteRole = "Empleado" },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (inviteRole == "Empleado") selectedColor else unselectedColor, contentColor = Color.White)
                                ) { Text("Empleado") }
                                Button(
                                    onClick = { inviteRole = "Sub-administrador" },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (inviteRole == "Sub-administrador") selectedColor else unselectedColor, contentColor = Color.White)
                                ) { Text("Sub-administrador") }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    if (!isSending && Patterns.EMAIL_ADDRESS.matcher(inviteEmail).matches()) {
                                        scope.launch {
                                            isSending = true
                                            firebaseManager.addInvitation(companyId!!, inviteEmail.trim(), inviteRole).fold(
                                                onSuccess = { id ->
                                                    invitations = invitations + Invitation(
                                                        id = id,
                                                        companyId = companyId!!,
                                                        email = inviteEmail.trim(),
                                                        role = inviteRole,
                                                        status = "Pendiente"
                                                    )
                                                    inviteEmail = ""
                                                },
                                                onFailure = { e ->
                                                    error = e.message
                                                }
                                            )
                                            isSending = false
                                        }
                                    }
                                },
                                enabled = !isSending && Patterns.EMAIL_ADDRESS.matcher(inviteEmail).matches(),
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Send, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (isSending) "Enviando..." else "Enviar Invitación", color = Color.White)
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

                // Invitaciones pendientes
                if (isOwner && invitations.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Invitaciones Pendientes", color = Color(0xFF212121), fontSize = 16.sp)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        invitations.forEach { inv ->
                            ListItem(
                                headlineContent = { Text(inv.email) },
                                supportingContent = { Text(inv.status) },
                                trailingContent = {
                                    Row {
                                        IconButton(onClick = {
                                            scope.launch {
                                                firebaseManager.updateInvitationStatus(companyId!!, inv.id, "Cancelada").fold(
                                                    onSuccess = {
                                                        invitations = invitations.map { if (it.id == inv.id) it.copy(status = "Cancelada") else it }
                                                    },
                                                    onFailure = { }
                                                )
                                            }
                                        }) {
                                            Icon(Icons.Filled.Close, contentDescription = "Cancelar")
                                        }
                                    }
                                }
                            )
                            Divider()
                        }
                    }
                }
            }
        }
    }
}