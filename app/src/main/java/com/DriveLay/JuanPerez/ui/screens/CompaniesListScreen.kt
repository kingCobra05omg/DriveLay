package com.DriveLay.JuanPerez.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Business
import com.DriveLay.JuanPerez.firebase.FirebaseManager
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompaniesListScreen(
    onBackClick: () -> Unit,
    onSelectCompany: (String) -> Unit,
) {
    val firebaseManager = remember { FirebaseManager() }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var companies by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }

    LaunchedEffect(Unit) {
        val res = firebaseManager.getUserCompanies()
        res.fold(onSuccess = {
            companies = it
            loading = false
        }, onFailure = {
            error = it.message
            loading = false
        })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Empresas", fontSize = 18.sp, color = Color.White) },
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
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            when {
                loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                error != null -> {
                    Text(
                        text = error ?: "Error",
                        color = Color.Red,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    if (companies.isEmpty()) {
                        Text(
                            text = "No perteneces a ninguna empresa",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            companies.forEach { company ->
                                val id = company["id"] as? String ?: return@forEach
                                val name = company["name"] as? String ?: "Empresa"
                                val logoUrl = company["logoUrl"] as? String
                                ListItem(
                                    leadingContent = {
                                        if (logoUrl != null) {
                                            AsyncImage(
                                                model = logoUrl,
                                                contentDescription = "Logo",
                                                modifier = Modifier.size(40.dp)
                                            )
                                        } else {
                                            Icon(Icons.Filled.Business, contentDescription = null)
                                        }
                                    },
                                    headlineContent = { Text(name) },
                                    trailingContent = { Icon(Icons.Filled.ArrowForward, contentDescription = null) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectCompany(id) }
                                        .padding(horizontal = 8.dp)
                                )
                                Divider()
                            }

                        }
                    }
                }
            }
        }
    }
}