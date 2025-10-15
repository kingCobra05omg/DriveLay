package com.DriveLay.JuanPerez

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.FirebaseDatabase

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
    }
}