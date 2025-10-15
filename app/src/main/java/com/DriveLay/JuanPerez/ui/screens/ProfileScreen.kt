package com.DriveLay.JuanPerez.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import coil.compose.AsyncImage
import com.DriveLay.JuanPerez.firebase.FirebaseManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBackClick: () -> Unit = {},
    onSignOut: () -> Unit = {}
) {
    val firebaseManager = remember { FirebaseManager() }
    val user = firebaseManager.getCurrentUser()
    val scope = rememberCoroutineScope()
    var apodo by remember { mutableStateOf("") }
    var fotoUri by remember { mutableStateOf<Uri?>(null) }
    var fotoUrl by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var numeroEmpleado by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }

    // Cargar datos actuales del usuario
    LaunchedEffect(user?.uid) {
        user?.uid?.let { uid ->
            firebaseManager.getUserData(uid).fold(
                onSuccess = { data ->
                    apodo = (data?.get("apodo") as? String) ?: ""
                    fotoUrl = (data?.get("fotoUrl") as? String)
                    numeroEmpleado = (data?.get("numeroEmpleado") as? String) ?: ""
                    telefono = (data?.get("telefono") as? String) ?: ""
                },
                onFailure = { }
            )
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        fotoUri = uri
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF003499))
    ) {
        CenterAlignedTopAppBar(
            title = { Text("Perfil", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Color.White) },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color(0xFF003499),
                navigationIconContentColor = Color.White,
                titleContentColor = Color.White
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar con botón de edición
                    Box(
                        modifier = Modifier.size(96.dp),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        AsyncImage(
                            model = fotoUri ?: (fotoUrl ?: ""),
                            contentDescription = "Foto de perfil",
                            modifier = Modifier
                                .matchParentSize()
                                .clip(CircleShape)
                                .background(Color(0xFFF1F5F9))
                        )
                        IconButton(
                            onClick = { imagePicker.launch("image/*") },
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2196F3))
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Cambiar foto",
                                tint = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = user?.email ?: "-",
                        color = Color(0xFF212121),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Apodo
                    OutlinedTextField(
                        value = apodo,
                        onValueChange = { apodo = it },
                        label = { Text("Apodo", color = Color(0xFF212121)) },
                        placeholder = { Text("Ingresa tu apodo", color = Color(0xFF9E9E9E)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF212121),
                            unfocusedTextColor = Color(0xFF212121),
                            focusedLabelColor = Color(0xFF616161),
                            unfocusedLabelColor = Color(0xFF9E9E9E),
                            focusedContainerColor = Color(0xFFF1F5F9),
                            unfocusedContainerColor = Color(0xFFF1F5F9)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Número de empleado
                    OutlinedTextField(
                        value = numeroEmpleado,
                        onValueChange = { numeroEmpleado = it },
                        label = { Text("Número de empleado", color = Color(0xFF212121)) },
                        placeholder = { Text("734591", color = Color(0xFF9E9E9E)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF1F5F9),
                            unfocusedContainerColor = Color(0xFFF1F5F9)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Número de teléfono
                    OutlinedTextField(
                        value = telefono,
                        onValueChange = { telefono = it },
                        label = { Text("Número de teléfono", color = Color(0xFF212121)) },
                        placeholder = { Text("+34 600 123 456", color = Color(0xFF9E9E9E)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF1F5F9),
                            unfocusedContainerColor = Color(0xFFF1F5F9)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                isSaving = true
                                var uploadedUrl: String? = fotoUrl
                                fotoUri?.let { uri ->
                                    firebaseManager.uploadProfileImage(uri).fold(
                                        onSuccess = { url -> uploadedUrl = url },
                                        onFailure = { }
                                    )
                                }
                                firebaseManager.updateUserProfile(
                                    nickname = apodo,
                                    photoUrl = uploadedUrl,
                                    employeeNumber = numeroEmpleado,
                                    phoneNumber = telefono
                                ).fold(
                                    onSuccess = { },
                                    onFailure = { }
                                )
                                fotoUrl = uploadedUrl
                                isSaving = false
                            }
                        },
                        enabled = !isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isSaving) "Guardando..." else "Guardar Cambios", color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            firebaseManager.signOut()
                            onSignOut()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0E0E0), contentColor = Color(0xFF212121)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cerrar Sesión")
                    }
                }
            }
        }
    }
}