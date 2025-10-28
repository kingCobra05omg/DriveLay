package com.DriveLay.JuanPerez.model

data class Employee(
    val id: String = "",
    val companyId: String = "",
    val name: String = "",
    val role: String = "Empleado",
    val active: Boolean = true,
    val email: String = "",
    val phone: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    constructor() : this("", "", "", "Empleado", true, "", "", 0L)

    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "companyId" to companyId,
        "name" to name,
        "role" to role,
        "active" to active,
        "email" to email,
        "phone" to phone,
        "createdAt" to createdAt
    )

    companion object {
        fun fromMap(map: Map<String, Any>): Employee = Employee(
            id = map["id"] as? String ?: "",
            companyId = map["companyId"] as? String ?: "",
            name = map["name"] as? String ?: "",
            role = map["role"] as? String ?: "Empleado",
            active = map["active"] as? Boolean ?: true,
            email = map["email"] as? String ?: "",
            phone = map["phone"] as? String ?: "",
            createdAt = (map["createdAt"] as? Number)?.toLong() ?: 0L
        )
    }
}