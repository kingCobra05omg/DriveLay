package com.DriveLay.JuanPerez.model

data class Vehicle(
    val id: String = "",
    val companyId: String = "",
    val name: String = "",
    val plate: String = "",
    val status: String = "Activo", // Activo, Mantenimiento, Inactivo
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val assignedTo: String? = null,
    val assignedAt: Long? = null,
    val startKm: Int? = null
) {
    constructor() : this("", "", "", "", "Activo", "", 0L, null, null, null)

    fun toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "id" to id,
            "companyId" to companyId,
            "name" to name,
            "plate" to plate,
            "status" to status,
            "description" to description,
            "createdAt" to createdAt
        )
        assignedTo?.let { map["assignedTo"] = it }
        assignedAt?.let { map["assignedAt"] = it }
        startKm?.let { map["startKm"] = it }
        return map
    }

    companion object {
        fun fromMap(map: Map<String, Any>): Vehicle = Vehicle(
            id = map["id"] as? String ?: "",
            companyId = map["companyId"] as? String ?: "",
            name = map["name"] as? String ?: "",
            plate = map["plate"] as? String ?: "",
            status = map["status"] as? String ?: "Activo",
            description = map["description"] as? String ?: "",
            createdAt = (map["createdAt"] as? Number)?.toLong() ?: 0L,
            assignedTo = map["assignedTo"] as? String,
            assignedAt = (map["assignedAt"] as? Number)?.toLong(),
            startKm = (map["startKm"] as? Number)?.toInt()
        )
    }
}