package com.DriveLay.JuanPerez.model

data class User(
    val id: String = "",
    val nombre: String = "",
    val apellido: String = "",
    val dni: String = "",
    val email: String = "",
    val fechaRegistro: Long = System.currentTimeMillis(),
    val apodo: String = "",
    val fotoUrl: String = ""
) {
    // Constructor sin parámetros requerido por Firestore
    constructor() : this("", "", "", "", "", 0L, "", "")
    
    // Convertir a Map para Firestore
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "nombre" to nombre,
            "apellido" to apellido,
            "dni" to dni,
            "email" to email,
            "fechaRegistro" to fechaRegistro,
            "apodo" to apodo,
            "fotoUrl" to fotoUrl
        )
    }
    
    companion object {
        // Crear User desde Map de Firestore
        fun fromMap(map: Map<String, Any>): User {
            return User(
                id = map["id"] as? String ?: "",
                nombre = map["nombre"] as? String ?: "",
                apellido = map["apellido"] as? String ?: "",
                dni = map["dni"] as? String ?: "",
                email = map["email"] as? String ?: "",
                fechaRegistro = map["fechaRegistro"] as? Long ?: 0L,
                apodo = map["apodo"] as? String ?: "",
                fotoUrl = map["fotoUrl"] as? String ?: ""
            )
        }
    }
}