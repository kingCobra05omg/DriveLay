package com.DriveLay.JuanPerez.model

data class Invitation(
    val id: String = "",
    val companyId: String = "",
    val email: String = "",
    val role: String = "Empleado",
    val status: String = "Pendiente",
    val createdAt: Long = System.currentTimeMillis()
) {
    constructor() : this("", "", "", "Empleado", "Pendiente", 0L)

    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "companyId" to companyId,
        "email" to email,
        "role" to role,
        "status" to status,
        "createdAt" to createdAt
    )

    companion object {
        fun fromMap(map: Map<String, Any>): Invitation = Invitation(
            id = map["id"] as? String ?: "",
            companyId = map["companyId"] as? String ?: "",
            email = map["email"] as? String ?: "",
            role = map["role"] as? String ?: "Empleado",
            status = map["status"] as? String ?: "Pendiente",
            createdAt = (map["createdAt"] as? Number)?.toLong() ?: 0L
        )
    }
}