package com.DriveLay.JuanPerez.model

data class Vehicle(
    val id: String = "",
    val companyId: String = "",
    val name: String = "",
    val plate: String = "",
    val status: String = "Activo", // Activo, Mantenimiento, Inactivo
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    constructor() : this("", "", "", "", "Activo", "", 0L)

    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "companyId" to companyId,
        "name" to name,
        "plate" to plate,
        "status" to status,
        "description" to description,
        "createdAt" to createdAt
    )

    companion object {
        fun fromMap(map: Map<String, Any>): Vehicle = Vehicle(
            id = map["id"] as? String ?: "",
            companyId = map["companyId"] as? String ?: "",
            name = map["name"] as? String ?: "",
            plate = map["plate"] as? String ?: "",
            status = map["status"] as? String ?: "Activo",
            description = map["description"] as? String ?: "",
            createdAt = (map["createdAt"] as? Number)?.toLong() ?: 0L
        )
    }
}