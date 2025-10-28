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
import androidx.compose.runtime.Composable
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
    val appVersion = try {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        "v ${info.versionName}"
    } catch (e: Exception) {
        "v 0.0.5"
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
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF003499)
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

        NavigationBar {
            NavigationBarItem(
                selected = false,
                onClick = { onBottomNavSelected("perfil") },
                icon = { Icon(Icons.Filled.Person, contentDescription = "Perfil") },
                label = { Text("Perfil") }
            )
            NavigationBarItem(
                selected = true,
                onClick = { onBottomNavSelected("empresa") },
                icon = { Icon(Icons.Filled.Home, contentDescription = "Empresa") },
                label = { Text("Empresa") }
            )
            NavigationBarItem(
                selected = false,
                onClick = { onBottomNavSelected("vehiculos") },
                icon = { Icon(Icons.Filled.DirectionsCar, contentDescription = "Vehículos") },
                label = { Text("Vehículos") }
            )
            NavigationBarItem(
                selected = false,
                onClick = { onBottomNavSelected("alertas") },
                icon = { Icon(Icons.Filled.Notifications, contentDescription = "Notificaciones") },
                label = { Text("Notificaciones") }
            )
            NavigationBarItem(
                selected = false,
                onClick = { onBottomNavSelected("config") },
                icon = { Icon(Icons.Filled.Settings, contentDescription = "Config") },
                label = { Text("Config") }
            )
        }
    }
}