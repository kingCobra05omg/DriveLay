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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.util.Patterns
import android.content.Intent
import android.net.Uri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeesListScreen(
    onBackClick: () -> Unit,
    companyIdArg: String? = null
) {
    val firebaseManager = remember { FirebaseManager() }
    val context = LocalContext.current
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var companyCode by remember { mutableStateOf<String?>(null) }
    var companyName by remember { mutableStateOf<String?>(null) }
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
    var invitationToDelete by remember { mutableStateOf<Invitation?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

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
        val loadForCompany: suspend (String) -> Unit = { cid ->
            companyId = cid
            // Datos de la empresa: código, logo, owner y miembros
            var ownerIdLocal: String? = null
            var membersIdsLocal: List<String> = emptyList()
            val companyRes = firebaseManager.getCompanyData(cid)
            companyRes.fold(onSuccess = { data ->
                companyCode = data?.get("code") as? String
                companyName = data?.get("name") as? String
                logoUrl = data?.get("logoUrl") as? String
                ownerIdLocal = data?.get("ownerId") as? String
                isOwner = ownerIdLocal == firebaseManager.getCurrentUser()?.uid
                membersIdsLocal = ((data?.get("members") as? List<*>)?.mapNotNull { it as? String }) ?: emptyList()
            }, onFailure = {
                error = it.message
            })

            // Empleados desde la subcolección; si no hay, mostrar miembros como fallback
            val empRes = firebaseManager.getEmployees(cid)
            empRes.fold(onSuccess = { listFromCollection ->
                var combined = listFromCollection
                if (combined.isEmpty() && membersIdsLocal.isNotEmpty()) {
                    val membersEmployees = mutableListOf<Employee>()
                    for (uid in membersIdsLocal) {
                        val userRes = firebaseManager.getUserData(uid)
                        userRes.fold(onSuccess = { udata ->
                            val nombre = (udata?.get("nombre") as? String)?.trim().orEmpty()
                            val apellido = (udata?.get("apellido") as? String)?.trim().orEmpty()
                            val email = (udata?.get("email") as? String)?.trim().orEmpty()
                            val fullName = listOf(nombre, apellido).filter { it.isNotBlank() }.joinToString(" ").ifBlank { email.ifBlank { "Miembro" } }
                            val role = if (uid == ownerIdLocal) "Administrador" else "Miembro"
                            membersEmployees.add(
                                Employee(
                                    id = uid,
                                    companyId = cid,
                                    name = fullName,
                                    role = role,
                                    active = true,
                                    email = email
                                )
                            )
                        }, onFailure = { /* ignorar fallos individuales */ })
                    }
                    combined = membersEmployees
                }
                employees = combined
                loading = false
            }, onFailure = {
                // Si falla la colección, intentar al menos mostrar miembros
                if (membersIdsLocal.isNotEmpty()) {
                    val membersEmployees = mutableListOf<Employee>()
                    for (uid in membersIdsLocal) {
                        val userRes = firebaseManager.getUserData(uid)
                        userRes.fold(onSuccess = { udata ->
                            val nombre = (udata?.get("nombre") as? String)?.trim().orEmpty()
                            val apellido = (udata?.get("apellido") as? String)?.trim().orEmpty()
                            val email = (udata?.get("email") as? String)?.trim().orEmpty()
                            val fullName = listOf(nombre, apellido).filter { it.isNotBlank() }.joinToString(" ").ifBlank { email.ifBlank { "Miembro" } }
                            val role = if (uid == ownerIdLocal) "Administrador" else "Miembro"
                            membersEmployees.add(
                                Employee(
                                    id = uid,
                                    companyId = cid,
                                    name = fullName,
                                    role = role,
                                    active = true,
                                    email = email
                                )
                            )
                        }, onFailure = { /* ignorar */ })
                    }
                    employees = membersEmployees
                } else {
                    error = it.message
                }
                loading = false
            })

            // Invitaciones de la empresa (solo para owner)
            val invRes = firebaseManager.getInvitations(cid)
            invRes.fold(onSuccess = { invitations = it }, onFailure = { /* no bloquear */ })
        }

        if (companyIdArg != null) {
            loadForCompany(companyIdArg)
        } else {
            val cidRes = firebaseManager.getCurrentCompanyId()
            cidRes.fold(onSuccess = { cid ->
                if (cid == null) {
                    error = "No perteneces a ninguna empresa"
                    loading = false
                } else {
                    loadForCompany(cid)
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
                .verticalScroll(rememberScrollState())
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
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.People, contentDescription = null, tint = Color(0xFF475569))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Código de la empresa: ${companyCode ?: "-"}",
                                    color = Color(0xFF0F172A),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
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
                                        Icon(
                                            Icons.Filled.ContentCopy,
                                            contentDescription = "Copiar código",
                                            tint = Color(0xFF475569)
                                        )
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
                                Text(
                                    text = "Sin imagen de empresa",
                                    color = Color(0xFF374151),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
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
                                            val emailTo = inviteEmail.trim()
                                            firebaseManager.addInvitation(companyId!!, emailTo, inviteRole).fold(
                                                onSuccess = { id ->
                                                    invitations = invitations + Invitation(
                                                        id = id,
                                                        companyId = companyId!!,
                                                        email = emailTo,
                                                        role = inviteRole,
                                                        status = "Pendiente"
                                                    )
                                                    // Abrir cliente de correo con el mensaje prellenado
                                                    val subject = "Invitación a unirse a la empresa ${companyName ?: ""}"
                                                    val body = buildString {
                                                        appendLine("Has sido invitado a unirte a la empresa ${companyName ?: "(sin nombre)"}.")
                                                        if (!companyCode.isNullOrBlank()) {
                                                            appendLine("Código de empresa: ${companyCode}.")
                                                        }
                                                        appendLine("Rol asignado: ${inviteRole}.")
                                                        appendLine()
                                                        appendLine("Para unirte: abre la app DriveLay, ve a 'Unirse a Empresa' e ingresa el código.")
                                                    }
                                                    try {
                                                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                                                            data = Uri.parse("mailto:")
                                                            putExtra(Intent.EXTRA_EMAIL, arrayOf(emailTo))
                                                            putExtra(Intent.EXTRA_SUBJECT, subject)
                                                            putExtra(Intent.EXTRA_TEXT, body)
                                                        }
                                                        context.startActivity(Intent.createChooser(intent, "Enviar invitación"))
                                                    } catch (e: Exception) {
                                                        error = "No se encontró una app de correo para enviar la invitación"
                                                    }
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
                                        IconButton(onClick = {
                                            invitationToDelete = inv
                                            showDeleteDialog = true
                                        }) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Eliminar")
                                        }
                                    }
                                }
                            )
                            Divider()
                        }
                    }
                }

                // Diálogo de confirmación para eliminar invitación
                if (showDeleteDialog && invitationToDelete != null && companyId != null) {
                    AlertDialog(
                        onDismissRequest = { showDeleteDialog = false; invitationToDelete = null },
                        title = { Text("Eliminar invitación") },
                        text = { Text("¿Seguro que deseas eliminar esta invitación pendiente? Esta acción no se puede deshacer.") },
                        confirmButton = {
                            TextButton(onClick = {
                                val toDelete = invitationToDelete
                                if (toDelete != null) {
                                    scope.launch {
                                        firebaseManager.deleteInvitation(companyId!!, toDelete.id).fold(
                                            onSuccess = {
                                                invitations = invitations.filter { it.id != toDelete.id }
                                            },
                                            onFailure = { /* opcional: mostrar error */ }
                                        )
                                        showDeleteDialog = false
                                        invitationToDelete = null
                                    }
                                } else {
                                    showDeleteDialog = false
                                }
                            }) {
                                Text("Eliminar", color = Color(0xFFD32F2F))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteDialog = false; invitationToDelete = null }) {
                                Text("Cancelar")
                            }
                        }
                    )
                }
            }
        }
    }
}