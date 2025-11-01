package com.DriveLay.JuanPerez.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onCreateCompanyClick: () -> Unit = {},
    onJoinCompanyClick: () -> Unit = {},
    onBottomNavSelected: (String) -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    // Mostrar versión fija según solicitud
    val appVersion = "v 1.0"
    val firebaseManager = remember { com.DriveLay.JuanPerez.firebase.FirebaseManager() }
    var showBell by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val comp = firebaseManager.getCurrentCompanyId()
        comp.fold(onSuccess = { cid ->
            if (cid.isNullOrBlank()) {
                showBell = false
            } else {
                val roleRes = firebaseManager.getUserRoleInCompany(cid)
                roleRes.fold(onSuccess = { role ->
                    showBell = (role == "Administrador" || role == "Sub-administrador")
                }, onFailure = {
                    showBell = false
                })
            }
        }, onFailure = {
            showBell = false
        })
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF003499))
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Bienvenido",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            },
            actions = {
                if (showBell) {
                    IconButton(onClick = { onBottomNavSelected("notificaciones") }) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = "Notificaciones",
                            tint = Color.White
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF003499),
                actionIconContentColor = Color.White,
                titleContentColor = Color.White
            )
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "¿Qué te gustaría\nhacer?",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clickable { onCreateCompanyClick() },
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF4CAF50)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.AddBusiness,
                        contentDescription = "Crear Empresa",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Crear Empresa",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clickable { onJoinCompanyClick() },
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF2196F3)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.GroupAdd,
                        contentDescription = "Unirse a Empresa",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Unirse a una Empresa",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = appVersion,
                fontSize = 12.sp,
                color = Color.White
            )
        }

        NavigationBar(
            containerColor = Color(0xFF003499)
        ) {
            // Inicio
            NavigationBarItem(
                selected = true,
                onClick = { onBottomNavSelected("inicio") },
                icon = { Icon(Icons.Filled.Home, contentDescription = "Inicio") },
                label = { Text("Inicio") },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = Color.White,
                    unselectedIconColor = Color(0xFFBBDEFB),
                    unselectedTextColor = Color(0xFFBBDEFB),
                    indicatorColor = Color(0xFF1565C0)
                )
            )
            // Empresa
            NavigationBarItem(
                selected = false,
                onClick = { onBottomNavSelected("empresa") },
                icon = { Icon(Icons.Filled.Business, contentDescription = "Empresa") },
                label = { Text("Empresa") },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = Color.White,
                    unselectedIconColor = Color(0xFFBBDEFB),
                    unselectedTextColor = Color(0xFFBBDEFB),
                    indicatorColor = Color(0xFF1565C0)
                )
            )
            // Vehículos
            NavigationBarItem(
                selected = false,
                onClick = { onBottomNavSelected("vehiculos") },
                icon = { Icon(Icons.Filled.DirectionsCar, contentDescription = "Vehículos") },
                label = { Text("Vehículos") },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = Color.White,
                    unselectedIconColor = Color(0xFFBBDEFB),
                    unselectedTextColor = Color(0xFFBBDEFB),
                    indicatorColor = Color(0xFF1565C0)
                )
            )
            // (Notificaciones eliminado de la barra inferior)
            // Perfil
            NavigationBarItem(
                selected = false,
                onClick = { onBottomNavSelected("perfil") },
                icon = { Icon(Icons.Filled.Person, contentDescription = "Perfil") },
                label = { Text("Perfil") },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = Color.White,
                    unselectedIconColor = Color(0xFFBBDEFB),
                    unselectedTextColor = Color(0xFFBBDEFB),
                    indicatorColor = Color(0xFF1565C0)
                )
            )
        }
    }
}