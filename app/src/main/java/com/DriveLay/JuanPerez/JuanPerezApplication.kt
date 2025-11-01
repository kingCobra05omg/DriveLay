package com.DriveLay.JuanPerez

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.content.pm.ApplicationInfo

class JuanPerezApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        try {
            // Inicializar Firebase de manera más explícita
            if (FirebaseApp.getApps(this).isEmpty()) {
                val app = FirebaseApp.initializeApp(this)
                Log.d("Firebase", "Firebase inicializado correctamente: ${app?.name}")
            } else {
                Log.d("Firebase", "Firebase ya estaba inicializado")
            }
            // Habilitar persistencia en Realtime Database (opcional)
            try {
                FirebaseDatabase.getInstance().setPersistenceEnabled(true)
                Log.d("Firebase", "Persistencia de Realtime Database habilitada")
            } catch (e: Exception) {
                Log.w("Firebase", "No se pudo habilitar persistencia (posible inicialización múltiple)")
            }
        } catch (e: Exception) {
            Log.e("Firebase", "Error al inicializar Firebase", e)
            
            // Intentar inicialización manual como respaldo
            try {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:690921397333:android:043681eb88d4c3b04708dc")
                    .setApiKey("AIzaSyBnaWlQfZyIraAPKhXawuH0eGtmTI4ncvM")
                    .setProjectId("juanperez-2e025")
                    .setStorageBucket("juanperez-2e025.firebasestorage.app")
                    .build()
                
                FirebaseApp.initializeApp(this, options)
                Log.d("Firebase", "Firebase inicializado manualmente como respaldo")
            } catch (fallbackException: Exception) {
                Log.e("Firebase", "Error en inicialización de respaldo", fallbackException)
            }
        }

        // Ejecutar diagnóstico y smoke test solo en debug
        if ((applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            runFirebaseDiagnostics()
        }
    }

    private fun runFirebaseDiagnostics() {
        // Log de opciones activas
        try {
            val app = FirebaseApp.getInstance()
            val opts = app.options
            Log.i("FirebaseDiag", "projectId=${opts.projectId}, appId=${opts.applicationId}, bucket=${opts.storageBucket}")
        } catch (e: Exception) {
            Log.w("FirebaseDiag", "FirebaseApp no inicializado aún", e)
        }

        // Pruebas simples de conectividad en background
        CoroutineScope(Dispatchers.IO).launch {
            // Auth
            try {
                val user = FirebaseAuth.getInstance().currentUser
                Log.i("FirebaseDiag", "authUser=${user?.uid ?: "null"}")
            } catch (e: Exception) {
                Log.e("FirebaseDiag", "Auth error", e)
            }

            // Firestore: lectura simple
            try {
                val fs = FirebaseFirestore.getInstance()
                val snap = fs.collection("companies").limit(1).get().await()
                Log.i("FirebaseDiag", "Firestore OK, companiesCount=${snap.size()}")
            } catch (e: Exception) {
                Log.e("FirebaseDiag", "Firestore error", e)
            }

            // Storage: listar carpeta 'smoke'
            try {
                val storage = FirebaseStorage.getInstance()
                val bucket = storage.reference.bucket
                Log.i("FirebaseDiag", "Storage bucket=$bucket")
                val listResult = storage.reference.child("smoke").list(1).await()
                Log.i("FirebaseDiag", "Storage OK, items=${listResult.items.size}")
            } catch (e: Exception) {
                Log.e("FirebaseDiag", "Storage error", e)
            }

            // Realtime DB: estado de conexión
            try {
                val db = FirebaseDatabase.getInstance()
                val infoSnap = db.getReference(".info/connected").get().await()
                val connected = infoSnap.getValue(Boolean::class.java)
                Log.i("FirebaseDiag", "Realtime DB connected=${connected}")
            } catch (e: Exception) {
                Log.e("FirebaseDiag", "Realtime DB error", e)
            }
        }
    }
}