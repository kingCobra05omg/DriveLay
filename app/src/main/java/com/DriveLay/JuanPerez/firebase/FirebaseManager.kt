package com.DriveLay.JuanPerez.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import android.net.Uri
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

class FirebaseManager {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val realtimeDb: FirebaseDatabase = FirebaseDatabase.getInstance()
    
    // Obtener usuario actual
    fun getCurrentUser(): FirebaseUser? = auth.currentUser
    
    // Registrar usuario con email y contraseña
    suspend fun registerUser(email: String, password: String): Result<FirebaseUser?> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            Result.success(result.user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Iniciar sesión con email y contraseña
    suspend fun signInUser(email: String, password: String): Result<FirebaseUser?> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            Result.success(result.user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Cerrar sesión
    fun signOut() {
        auth.signOut()
    }
    
    // ===== Realtime Database =====
    fun getRef(path: String): DatabaseReference = realtimeDb.getReference(path)

    // Escribir o sobrescribir un valor en una ruta específica
    suspend fun setValue(path: String, value: Any): Result<Unit> {
        return try {
            getRef(path).setValue(value).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Crear un hijo con clave única (push) y escribir valor
    suspend fun push(path: String, value: Any): Result<String> {
        return try {
            val ref = getRef(path).push()
            ref.setValue(value).await()
            Result.success(ref.key ?: "")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Actualizar múltiples campos de un nodo sin sobrescribir todo
    suspend fun updateChildren(path: String, updates: Map<String, Any?>): Result<Unit> {
        return try {
            getRef(path).updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Escuchar cambios en tiempo real con ValueEventListener
    fun addValueListener(
        path: String,
        onData: (DataSnapshot) -> Unit,
        onError: (DatabaseError) -> Unit
    ): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) { onData(snapshot) }
            override fun onCancelled(error: DatabaseError) { onError(error) }
        }
        getRef(path).addValueEventListener(listener)
        return listener
    }

    fun removeListener(path: String, listener: ValueEventListener) {
        getRef(path).removeEventListener(listener)
    }
    
    // Guardar datos del usuario en Firestore
    suspend fun saveUserData(userId: String, userData: Map<String, Any>): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(userId)
                .set(userData)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Obtener datos del usuario desde Firestore
    suspend fun getUserData(userId: String): Result<Map<String, Any>?> {
        return try {
            val document = firestore.collection("users")
                .document(userId)
                .get()
                .await()
            Result.success(document.data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Subir imagen de perfil a Firebase Storage y devolver URL
    suspend fun uploadProfileImage(uri: Uri): Result<String> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Usuario no autenticado"))
            val storage = FirebaseStorage.getInstance()
            val ref = storage.reference.child("profile_images/$uid.jpg")
            ref.putFile(uri).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Actualizar perfil del usuario (apodo, foto, número de empleado y teléfono)
    suspend fun updateUserProfile(
        nickname: String?,
        photoUrl: String?,
        employeeNumber: String?,
        phoneNumber: String?
    ): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Usuario no autenticado"))
            val data = mutableMapOf<String, Any>()
            // Permitir limpiar campos si vienen vacíos (delete)
            data["apodo"] = if (nickname.isNullOrBlank()) FieldValue.delete() else nickname
            data["fotoUrl"] = if (photoUrl.isNullOrBlank()) FieldValue.delete() else photoUrl
            data["numeroEmpleado"] = if (employeeNumber.isNullOrBlank()) FieldValue.delete() else employeeNumber
            data["telefono"] = if (phoneNumber.isNullOrBlank()) FieldValue.delete() else phoneNumber
            firestore.collection("users").document(uid).set(data, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Crear empresa en Firestore
    suspend fun createCompany(name: String, employees: String, vehicles: String): Result<String> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Usuario no autenticado"))
            val code = generateCompanyCode()
            val data = mapOf(
                "name" to name,
                "employees" to (employees.toIntOrNull() ?: 0),
                "vehicles" to (vehicles.toIntOrNull() ?: 0),
                "ownerId" to uid,
                "members" to listOf(uid),
                "code" to code,
                "createdAt" to System.currentTimeMillis()
            )
            val docRef = firestore.collection("companies").add(data).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Unirse a empresa por código
    suspend fun joinCompany(code: String): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Usuario no autenticado"))
            val query = firestore.collection("companies").whereEqualTo("code", code).get().await()
            if (query.isEmpty) return Result.failure(IllegalArgumentException("Código no válido"))
            val doc = query.documents.first()
            val members = (doc.get("members") as? List<*>)?.map { it as String } ?: emptyList()
            val updated = members.toMutableSet().apply { add(uid) }.toList()
            doc.reference.update("members", updated).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun generateCompanyCode(): String {
        val chars = (('A'..'Z') + ('0'..'9')).toList()
        return (1..6).map { chars[Random.nextInt(chars.size)] }.joinToString("")
    }
}