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
            // Crear/mezclar perfil mínimo del usuario con rol Reader por defecto
            result.user?.uid?.let { uid ->
                val minimalProfile = mapOf(
                    "id" to uid,
                    "email" to email,
                    "fechaRegistro" to System.currentTimeMillis(),
                    "role" to "Reader"
                )
                firestore.collection("users").document(uid).set(minimalProfile, SetOptions.merge()).await()
            }
            Result.success(result.user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Iniciar sesión con email y contraseña
    suspend fun signInUser(email: String, password: String): Result<FirebaseUser?> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            // Asegurar documento y rol por defecto para el usuario al iniciar sesión
            result.user?.uid?.let { uid ->
                val docRef = firestore.collection("users").document(uid)
                val snap = docRef.get().await()
                if (!snap.exists()) {
                    val minimalProfile = mapOf(
                        "id" to uid,
                        "email" to email,
                        "fechaRegistro" to System.currentTimeMillis(),
                        "role" to "Reader"
                    )
                    docRef.set(minimalProfile, SetOptions.merge()).await()
                } else {
                    val currentRole = snap.get("role") as? String
                    if (currentRole.isNullOrBlank()) {
                        docRef.set(mapOf("role" to "Reader"), SetOptions.merge()).await()
                    } else {
                        // No-op: rol ya definido
                    }
                }
            }
            Result.success(result.user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== Roles y permisos dentro de una empresa =====
    suspend fun getUserRoleInCompany(companyId: String): Result<String?> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Usuario no autenticado"))
            // Si es el propietario, considerarlo Administrador
            val companyDoc = firestore.collection("companies").document(companyId).get().await()
            val ownerId = companyDoc.get("ownerId") as? String
            if (ownerId == uid) {
                return Result.success("Administrador")
            }
            // Buscar rol en la subcolección employees por email
            val email = auth.currentUser?.email
            if (!email.isNullOrBlank()) {
                val snap = firestore.collection("companies").document(companyId)
                    .collection("employees").whereEqualTo("email", email).limit(1).get().await()
                val role = snap.documents.firstOrNull()?.get("role") as? String
                Result.success(role)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== Notificaciones =====
    // Agregar notificación en la empresa (en subcolección notifications)
    suspend fun addCompanyNotification(
        companyId: String,
        type: String,
        vehicleId: String? = null,
        message: String? = null,
        extra: Map<String, Any?> = emptyMap()
    ): Result<String> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Usuario no autenticado"))
            val notif = mutableMapOf<String, Any>(
                "type" to type,
                "timestamp" to System.currentTimeMillis(),
                "userId" to uid
            )
            if (!message.isNullOrBlank()) notif["message"] = message
            vehicleId?.let { notif["vehicleId"] = it }
            // Enriquecer con datos del usuario
            kotlin.runCatching {
                val userDoc = firestore.collection("users").document(uid).get().await()
                val nombre = (userDoc.get("nombre") as? String)?.trim().orEmpty()
                val apellido = (userDoc.get("apellido") as? String)?.trim().orEmpty()
                val email = (userDoc.get("email") as? String)?.trim().orEmpty()
                val fullName = listOf(nombre, apellido).filter { it.isNotBlank() }.joinToString(" ")
                if (fullName.isNotBlank()) notif["userName"] = fullName
                if (email.isNotBlank()) notif["userEmail"] = email
            }
            // Enriquecer con datos del vehículo si aplica
            if (!vehicleId.isNullOrBlank()) {
                kotlin.runCatching {
                    val vdoc = firestore.collection("companies").document(companyId)
                        .collection("vehicles").document(vehicleId).get().await()
                    val plate = (vdoc.get("plate") as? String)?.trim().orEmpty()
                    val vname = (vdoc.get("name") as? String)?.trim().orEmpty()
                    if (plate.isNotBlank()) notif["vehiclePlate"] = plate
                    if (vname.isNotBlank()) notif["vehicleName"] = vname
                }
            }
            // Agregar campos extra
            extra.forEach { (k, v) -> if (v != null) notif[k] = v }
            val ref = firestore.collection("companies").document(companyId)
                .collection("notifications").add(notif).await()
            Result.success(ref.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Listar notificaciones de la empresa (ordenadas por tiempo desc)
    suspend fun getCompanyNotifications(companyId: String): Result<List<Map<String, Any>>> {
        return try {
            val snap = firestore.collection("companies").document(companyId)
                .collection("notifications").orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get().await()
            val list = snap.documents.mapNotNull { d ->
                val data = d.data ?: return@mapNotNull null
                val m = data.toMutableMap()
                m["id"] = d.id
                m.toMap()
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun hasVehicleCrudPermission(companyId: String): Result<Boolean> {
        return try {
            val roleRes = getUserRoleInCompany(companyId)
            roleRes.fold(
                onSuccess = { role ->
                    Result.success(role == "Sub-administrador" || role == "Administrador")
                },
                onFailure = { err -> Result.failure(err) }
            )
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
            val mergedData = userData.toMutableMap()
            if (!mergedData.containsKey("role")) {
                mergedData["role"] = "Reader"
            }
            firestore.collection("users")
                .document(userId)
                .set(mergedData, SetOptions.merge())
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
            // Escalar rol a Writer antes de crear la empresa (por reglas)
            firestore.collection("users").document(uid)
                .set(mapOf("role" to "Writer"), SetOptions.merge()).await()
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
            // Añadir empresa al perfil del usuario y fijarla como activa
            firestore.collection("users").document(uid)
                .set(
                    mapOf(
                        "companies" to FieldValue.arrayUnion(docRef.id),
                        "currentCompanyId" to docRef.id
                    ),
                    SetOptions.merge()
                ).await()
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
            // Añadir empresa al perfil del usuario y fijarla como activa
            firestore.collection("users").document(uid)
                .set(
                    mapOf(
                        "companies" to FieldValue.arrayUnion(doc.id),
                        "currentCompanyId" to doc.id
                    ),
                    SetOptions.merge()
                ).await()
            // Notificación: empleado se une a la empresa
            kotlin.runCatching {
                addCompanyNotification(doc.id, type = "employee_join", message = "Empleado se unió a la empresa").getOrThrow()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun generateCompanyCode(): String {
        val chars = (("A"[0]..'Z') + ('0'..'9')).toList()
        return (1..6).map { chars[Random.nextInt(chars.size)] }.joinToString("")
    }

    // ====== Empresas (contexto actual) ======
    suspend fun getCurrentCompanyId(): Result<String?> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Usuario no autenticado"))
            // 1) Intentar usar empresa activa guardada en el perfil del usuario
            val userDoc = firestore.collection("users").document(uid).get().await()
            val saved = (userDoc.get("currentCompanyId") as? String)
            if (!saved.isNullOrBlank()) {
                return Result.success(saved)
            }

            // 2) Fallback: primera empresa donde es miembro
            val companiesCol = firestore.collection("companies")
            val membersQuery = companiesCol.whereArrayContains("members", uid).get().await()
            val idFromMembers = membersQuery.documents.firstOrNull()?.id
            if (idFromMembers != null) {
                return Result.success(idFromMembers)
            }

            // 3) Fallback: empresa donde es propietario (ownerId)
            val ownerQuery = companiesCol.whereEqualTo("ownerId", uid).get().await()
            val idFromOwner = ownerQuery.documents.firstOrNull()?.id
            Result.success(idFromOwner)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Fijar empresa activa en el perfil del usuario
    suspend fun setCurrentCompanyId(companyId: String): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Usuario no autenticado"))
            firestore.collection("users").document(uid)
                .set(mapOf("currentCompanyId" to companyId), SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCompanyData(companyId: String): Result<Map<String, Any>?> {
        return try {
            val doc = firestore.collection("companies").document(companyId).get().await()
            Result.success(doc.data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Agregar: Listar todas las empresas del usuario actual (como miembro o propietario)
    suspend fun getUserCompanies(): Result<List<Map<String, Any>>> {
        return try {
            val uid = getCurrentUser()?.uid ?: return Result.failure(IllegalStateException("Usuario no autenticado"))
            val companiesCol = com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("companies")
            val membersSnap = companiesCol.whereArrayContains("members", uid).get().await()
            val ownerSnap = companiesCol.whereEqualTo("ownerId", uid).get().await()

            val allDocs = (membersSnap.documents + ownerSnap.documents)
            val distinctDocs = allDocs.distinctBy { it.id }

            val list = distinctDocs.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                val mutable = data.toMutableMap()
                mutable["id"] = doc.id
                mutable.toMap()
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ====== Empleados ======
    suspend fun addEmployee(companyId: String, employee: com.DriveLay.JuanPerez.model.Employee): Result<String> {
        return try {
            val ref = firestore.collection("companies").document(companyId).collection("employees").add(employee.toMap()).await()
            // guardar id en el documento
            ref.update("id", ref.id).await()
            Result.success(ref.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getEmployees(companyId: String): Result<List<com.DriveLay.JuanPerez.model.Employee>> {
        return try {
            val snap = firestore.collection("companies").document(companyId).collection("employees").get().await()
            val list = snap.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                com.DriveLay.JuanPerez.model.Employee.fromMap(data)
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateEmployee(companyId: String, employeeId: String, updates: Map<String, Any?>): Result<Unit> {
        return try {
            firestore.collection("companies").document(companyId).collection("employees").document(employeeId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteEmployee(companyId: String, employeeId: String): Result<Unit> {
        return try {
            firestore.collection("companies").document(companyId).collection("employees").document(employeeId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ====== Invitaciones ======
    suspend fun addInvitation(companyId: String, email: String, role: String): Result<String> {
        return try {
            val invitation = com.DriveLay.JuanPerez.model.Invitation(
                companyId = companyId,
                email = email,
                role = role,
                status = "Pendiente"
            )
            val ref = firestore.collection("companies").document(companyId)
                .collection("invitations").add(invitation.toMap()).await()
            ref.update("id", ref.id).await()
            Result.success(ref.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getInvitations(companyId: String): Result<List<com.DriveLay.JuanPerez.model.Invitation>> {
        return try {
            val snap = firestore.collection("companies").document(companyId)
                .collection("invitations").get().await()
            val list = snap.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                com.DriveLay.JuanPerez.model.Invitation.fromMap(data as Map<String, Any>)
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateInvitationStatus(companyId: String, invitationId: String, status: String): Result<Unit> {
        return try {
            firestore.collection("companies").document(companyId)
                .collection("invitations").document(invitationId)
                .update(mapOf("status" to status)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteInvitation(companyId: String, invitationId: String): Result<Unit> {
        return try {
            firestore.collection("companies").document(companyId)
                .collection("invitations").document(invitationId)
                .delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ====== Vehículos ======
    suspend fun addVehicle(companyId: String, vehicle: com.DriveLay.JuanPerez.model.Vehicle): Result<String> {
        return try {
            // Permitir crear sólo a sub-administrador o administrador
            val perm = hasVehicleCrudPermission(companyId)
            val allowed = perm.fold(onSuccess = { it }, onFailure = { return Result.failure(it) })
            if (!allowed) return Result.failure(IllegalAccessException("Sin permiso para crear vehículos"))
            val ref = firestore.collection("companies").document(companyId).collection("vehicles").add(vehicle.toMap()).await()
            ref.update("id", ref.id).await()
            Result.success(ref.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getVehicles(companyId: String): Result<List<com.DriveLay.JuanPerez.model.Vehicle>> {
        return try {
            val snap = firestore.collection("companies").document(companyId).collection("vehicles").get().await()
            val list = snap.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                com.DriveLay.JuanPerez.model.Vehicle.fromMap(data)
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateVehicle(companyId: String, vehicleId: String, updates: Map<String, Any?>): Result<Unit> {
        return try {
            // Editar sólo permitido para sub-administrador o administrador
            val perm = hasVehicleCrudPermission(companyId)
            val allowed = perm.fold(onSuccess = { it }, onFailure = { return Result.failure(it) })
            if (!allowed) return Result.failure(IllegalAccessException("Sin permiso para editar vehículos"))
            firestore.collection("companies").document(companyId).collection("vehicles").document(vehicleId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteVehicle(companyId: String, vehicleId: String): Result<Unit> {
        return try {
            // Borrar sólo permitido para sub-administrador o administrador
            val perm = hasVehicleCrudPermission(companyId)
            val allowed = perm.fold(onSuccess = { it }, onFailure = { return Result.failure(it) })
            if (!allowed) return Result.failure(IllegalAccessException("Sin permiso para borrar vehículos"))
            firestore.collection("companies").document(companyId).collection("vehicles").document(vehicleId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Asignar vehículo al usuario actual (permitido para empleados)
    suspend fun assignVehicleToCurrentUser(companyId: String, vehicleId: String): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Usuario no autenticado"))
            val updates = mapOf(
                "status" to "En Uso",
                "assignedTo" to uid,
                "assignedAt" to System.currentTimeMillis()
            )
            firestore.collection("companies").document(companyId)
                .collection("vehicles").document(vehicleId)
                .update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Obtener un vehículo específico por ID
    suspend fun getVehicle(companyId: String, vehicleId: String): Result<com.DriveLay.JuanPerez.model.Vehicle?> {
        return try {
            val doc = firestore.collection("companies").document(companyId)
                .collection("vehicles").document(vehicleId).get().await()
            val data = doc.data
            if (data != null) {
                Result.success(com.DriveLay.JuanPerez.model.Vehicle.fromMap(data))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Iniciar uso del vehículo (registro de salida)
    suspend fun startVehicleUsage(companyId: String, vehicleId: String, startKm: Int, startTimestamp: Long = System.currentTimeMillis()): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Usuario no autenticado"))
            val updates = mapOf(
                "status" to "En Uso",
                "assignedTo" to uid,
                "assignedAt" to startTimestamp,
                "startKm" to startKm
            )
            firestore.collection("companies").document(companyId)
                .collection("vehicles").document(vehicleId)
                .update(updates).await()
            // Notificación: salida de vehículo
            kotlin.runCatching {
                addCompanyNotification(companyId, type = "vehicle_start", vehicleId = vehicleId, message = "Empleado retiró un vehículo").getOrThrow()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Finalizar uso del vehículo (registro de devolución)
    suspend fun finishVehicleUsage(companyId: String, vehicleId: String, endKm: Int, notes: String?): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Usuario no autenticado"))
            val vehicleDoc = firestore.collection("companies").document(companyId)
                .collection("vehicles").document(vehicleId).get().await()
            val data = vehicleDoc.data
            val startAt = (data?.get("assignedAt") as? Number)?.toLong()
            val startKm = (data?.get("startKm") as? Number)?.toInt()
            val endAt = System.currentTimeMillis()
            val durationMs = endAt - (startAt ?: endAt)
            val distanceKm = (endKm - (startKm ?: endKm)).coerceAtLeast(0)

            // Registrar uso en subcolección usages
            val usage = mutableMapOf<String, Any>(
                "vehicleId" to vehicleId,
                "userId" to uid,
                "endAt" to endAt,
                "endKm" to endKm,
                "durationMs" to durationMs,
                "distanceKm" to distanceKm,
                "companyId" to companyId
            )
            if (startAt != null) usage["startAt"] = startAt
            if (startKm != null) usage["startKm"] = startKm
            if (!notes.isNullOrBlank()) usage["notes"] = notes
            // Enriquecer con datos del usuario y vehículo para listado eficiente
            kotlin.runCatching {
                val udoc = firestore.collection("users").document(uid).get().await()
                val nombre = (udoc.get("nombre") as? String)?.trim().orEmpty()
                val apellido = (udoc.get("apellido") as? String)?.trim().orEmpty()
                val email = (udoc.get("email") as? String)?.trim().orEmpty()
                val fullName = listOf(nombre, apellido).filter { it.isNotBlank() }.joinToString(" ")
                if (fullName.isNotBlank()) usage["userName"] = fullName
                if (email.isNotBlank()) usage["userEmail"] = email
            }
            kotlin.runCatching {
                val vdoc = firestore.collection("companies").document(companyId)
                    .collection("vehicles").document(vehicleId).get().await()
                val plate = (vdoc.get("plate") as? String)?.trim().orEmpty()
                val vname = (vdoc.get("name") as? String)?.trim().orEmpty()
                if (plate.isNotBlank()) usage["vehiclePlate"] = plate
                if (vname.isNotBlank()) usage["vehicleName"] = vname
            }

            firestore.collection("companies").document(companyId)
                .collection("vehicles").document(vehicleId)
                .collection("usages").add(usage).await()

            // Actualizar estado del vehículo y limpiar asignación
            val updates = mapOf(
                "status" to "Activo",
                "assignedTo" to com.google.firebase.firestore.FieldValue.delete(),
                "assignedAt" to com.google.firebase.firestore.FieldValue.delete(),
                "startKm" to com.google.firebase.firestore.FieldValue.delete()
            )
            firestore.collection("companies").document(companyId)
                .collection("vehicles").document(vehicleId)
                .update(updates).await()
            // Notificación: devolución de vehículo
            kotlin.runCatching {
                addCompanyNotification(
                    companyId,
                    type = "vehicle_finish",
                    vehicleId = vehicleId,
                    message = "Empleado devolvió un vehículo",
                    extra = mapOf("durationMs" to durationMs, "distanceKm" to distanceKm)
                ).getOrThrow()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Reportar alerta de accidente para notificar al administrador
    suspend fun reportAccidentAlert(companyId: String, vehicleId: String, message: String?): Result<String> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Usuario no autenticado"))
            val alert = mutableMapOf<String, Any>(
                "type" to "accident",
                "vehicleId" to vehicleId,
                "userId" to uid,
                "timestamp" to System.currentTimeMillis(),
                "status" to "open"
            )
            if (!message.isNullOrBlank()) alert["message"] = message
            val ref = firestore.collection("companies").document(companyId)
                .collection("alerts").add(alert).await()
            // Marcar alerta en vehículo (opcional)
            firestore.collection("companies").document(companyId)
                .collection("vehicles").document(vehicleId)
                .update(mapOf("lastAlertAt" to System.currentTimeMillis())).await()
            // Notificación: solicitud de ayuda
            kotlin.runCatching {
                addCompanyNotification(
                    companyId,
                    type = "help_request",
                    vehicleId = vehicleId,
                    message = message ?: "Solicitud de ayuda"
                ).getOrThrow()
            }
            Result.success(ref.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== Historial de usos (collectionGroup) =====
    suspend fun getCompanyUsageHistory(companyId: String): Result<List<Map<String, Any>>> {
        return try {
            val fs = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            // Intento con ordenamiento en servidor (requiere índice compuesto)
            val snap = fs.collectionGroup("usages")
                .whereEqualTo("companyId", companyId)
                .orderBy("endAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get().await()
            val list = snap.documents.mapNotNull { d ->
                val data = d.data ?: return@mapNotNull null
                val m = data.toMutableMap()
                m["id"] = d.id
                m.toMap()
            }
            Result.success(list)
        } catch (e: Exception) {
            // Fallback: si falta el índice, consultamos sin orderBy y ordenamos en cliente
            return try {
                val fs = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val snapNoOrder = fs.collectionGroup("usages")
                    .whereEqualTo("companyId", companyId)
                    .get().await()
                val unsorted = snapNoOrder.documents.mapNotNull { d ->
                    val data = d.data ?: return@mapNotNull null
                    val m = data.toMutableMap()
                    m["id"] = d.id
                    m.toMap()
                }
                val sorted = unsorted.sortedByDescending { (it["endAt"] as? Number)?.toLong() ?: 0L }
                Result.success(sorted)
            } catch (inner: Exception) {
                Result.failure(inner)
            }
        }
    }

    suspend fun uploadCompanyImage(companyId: String, uri: android.net.Uri): Result<String> {
        return try {
            val ref = com.google.firebase.storage.FirebaseStorage.getInstance().reference.child("company_images/$companyId.jpg")
            ref.putFile(uri).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadCompanyImageFromBytes(companyId: String, bytes: ByteArray): Result<String> {
        return try {
            val ref = com.google.firebase.storage.FirebaseStorage.getInstance().reference.child("company_images/$companyId.jpg")
            ref.putBytes(bytes).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadDefaultCompanyImageFromUrl(companyId: String, url: String): Result<String> {
        return try {
            val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.instanceFollowRedirects = true
            connection.requestMethod = "GET"
            val code = connection.responseCode
            if (code in 200..299) {
                val bytes = connection.inputStream.readBytes()
                connection.inputStream.close()
                connection.disconnect()
                uploadCompanyImageFromBytes(companyId, bytes)
            } else {
                connection.disconnect()
                Result.failure(Exception("HTTP $code al descargar imagen por defecto"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateCompany(companyId: String, updates: Map<String, Any?>): Result<Unit> {
        return try {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("companies").document(companyId)
                .update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}