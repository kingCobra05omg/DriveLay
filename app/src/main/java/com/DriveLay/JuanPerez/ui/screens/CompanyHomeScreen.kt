package com.DriveLay.JuanPerez.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
    var isOwner by remember { mutableStateOf(false) }
    var companyId by remember { mutableStateOf<String?>(companyIdArg) }
    var logoUrl by remember { mutableStateOf<String?>(null) }
    val currentUserId = firebaseManager.getCurrentUser()?.uid

    LaunchedEffect(companyIdArg) {
        scope.launch {
            if (companyIdArg != null) {
                companyId = companyIdArg
                val dataRes = firebaseManager.getCompanyData(companyIdArg)
                dataRes.fold(onSuccess = { data ->
                    companyName = data?.get("name") as? String
                    val ownerId = data?.get("ownerId") as? String
                    logoUrl = data?.get("logoUrl") as? String
                    isOwner = ownerId == currentUserId
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
                            isOwner = ownerId == currentUserId
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
            SmallTopAppBar(
                title = { Text(text = "Mi Empresa", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (isOwner) {
                        TextButton(onClick = onGoToAdmin) { Text("Administrar") }
                    }
                }
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
                        if (logoUrl != null) {
                            AsyncImage(
                                model = logoUrl,
                                contentDescription = "Logo de la empresa",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                            )
                        } else {
                            Image(painter = painterResource(id = R.drawable.ic_launcher_foreground), contentDescription = null)
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(text = companyName ?: "Mi Empresa", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text(text = if (isOwner) "Rol: Administrador" else "Rol: Miembro")
                    }
                }
            }
        }
    }
}