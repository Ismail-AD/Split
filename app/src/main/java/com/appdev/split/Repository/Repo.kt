package com.appdev.split.Repository

import android.net.Uri
import android.util.Log
import androidx.core.net.toFile
import com.appdev.split.Model.Data.ExpenseRecord
import com.appdev.split.Model.Data.Friend
import com.appdev.split.Model.Data.FriendContact
import com.appdev.split.Model.Data.GroupMetaData
import com.appdev.split.Model.Data.UserEntity
import com.appdev.split.Room.DaoClasses.ContactDao
import com.appdev.split.Utils.Utils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

import javax.inject.Inject

class Repo @Inject constructor(
    private val contactDao: ContactDao,
    private val firestore:FirebaseFirestore,
    val firebaseAuth: FirebaseAuth,
    val supabaseClient: SupabaseClient
) {

    val currentUser = firebaseAuth.currentUser
    private val bucketId = "groupImages"
    private val folderPath = "public/7pgyxj_1"

    private val profileBucketId = "profileImages"
    private val profileFolderPath = "public/userimages"

    suspend fun uploadImageAndSaveGroup(
        mail: String,
        imageUri: Uri?,
        imageBytes: ByteArray?,
        title: String,
        groupType: String,
        onSuccess: (String, String) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val publicUrl = if (imageUri != null && imageBytes != null) {
                uploadImageToSupabase(imageUri, imageBytes)
            } else {
                ""
            }

            val groupId = saveGroupToFirestore(publicUrl, title, groupType, mail)
            onSuccess("Group created successfully", groupId)
        } catch (e: Exception) {
            Log.d("CHKJM","${e.message}")
            onError("Failed to create group: ${e.message}")
        }
    }

    private suspend fun uploadImageToSupabase(imageUri: Uri, imageBytes: ByteArray): String {
        return withContext(Dispatchers.IO) {
            try {
                val extension = when {
                    // Try to get from Uri
                    imageUri.lastPathSegment?.contains(".") == true ->
                        imageUri.lastPathSegment?.substringAfterLast('.')?.lowercase()
                    // Fallback to detecting from bytes
                    else -> when {
                        imageBytes.size >= 2 && imageBytes[0] == 0xFF.toByte() && imageBytes[1] == 0xD8.toByte() -> "jpg"
                        imageBytes.size >= 8 && String(imageBytes.take(8).toByteArray()) == "PNG\r\n\u001a\n" -> "png"
                        else -> "jpg" // Default fallback
                    }
                } ?: "jpg"

                // Validate extension
                val safeExtension = when (extension.lowercase()) {
                    "jpg", "jpeg", "png", "gif" -> extension
                    else -> "jpg"
                }
                val fileName = "${UUID.randomUUID()}.$safeExtension"
                val fullPath = "$folderPath/$fileName"
                Log.d("CHKJM","$fullPath")

                val bucket = supabaseClient.storage.from(bucketId)
                bucket.upload(
                    path = fullPath,
                    data = imageBytes
                ) {
                    upsert = false
                }

                bucket.publicUrl(fullPath)
            } catch (e: Exception) {
                throw Exception("Failed to upload image: ${e.message}")
            }
        }
    }

    private suspend fun saveGroupToFirestore(
        imageUrl: String,
        title: String,
        groupType: String,
        mail: String
    ): String {
        return withContext(Dispatchers.IO) {
            try {
                val sanitizedMail = Utils.sanitizeEmailForFirebase(mail)
                val groupsCollection = firestore.collection("groups")
                    .document(sanitizedMail)
                    .collection("userGroups")

                val groupRef = groupsCollection.document()
                val groupId = groupRef.id

                val groupData = GroupMetaData(
                    groupId = groupId,
                    image = imageUrl,
                    title = title,
                    groupType = groupType
                )

                groupRef.set(groupData).await()
                groupId
            } catch (e: Exception) {
                throw Exception("Failed to save group data: ${e.message}")
            }
        }
    }

    suspend fun getAllGroups(mail: String, onSuccess: (List<GroupMetaData>) -> Unit, onError: (String) -> Unit) {
        try {
            val sanitizedMail = Utils.sanitizeEmailForFirebase(mail)
            val groupsSnapshot = firestore.collection("groups")
                .document(sanitizedMail)
                .collection("userGroups")
                .get()
                .await()

            val groups = groupsSnapshot.documents.mapNotNull { doc ->
                doc.toObject(GroupMetaData::class.java)
            }

            onSuccess(groups)
        } catch (e: Exception) {
            Log.d("CJKA", "${e.message}")
            onError("Failed to fetch groups: ${e.message}")
        }
    }

    suspend fun signUp(
        userEntity: UserEntity,
        imageUri: Uri?,
        imageBytes: ByteArray?,
        result: (message: String, success: Boolean) -> Unit
    ) {
        try {
            // Upload image first if provided
            val profileImageUrl = if (imageUri != null && imageBytes != null) {
                uploadProfileImageToSupabase(imageUri, imageBytes)
            } else {
                ""
            }

            val sanitizedMail = Utils.sanitizeEmailForFirebase(userEntity.email)
            firebaseAuth.createUserWithEmailAndPassword(userEntity.email, userEntity.password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userId = firebaseAuth.currentUser?.uid ?: return@addOnCompleteListener
                        val userProfile = hashMapOf(
                            "name" to userEntity.name,
                            "email" to userEntity.email,
                            "profileImage" to profileImageUrl  // Add profile image URL
                        )

                        firestore.collection("profiles")
                            .document(sanitizedMail)
                            .set(userProfile)
                            .addOnSuccessListener {
                                result("Account created successfully", true)
                            }
                            .addOnFailureListener { e->
                                e.message?.let { Log.d("CHKAZ", it) }
                                result("Account created, but data upload failed", false)
                            }
                    } else {
                        result(task.exception!!.message.toString(), false)
                    }
                }
        } catch (e: Exception) {
            result("Failed to create account: ${e.message}", false)
        }
    }

    private suspend fun uploadProfileImageToSupabase(imageUri: Uri, imageBytes: ByteArray): String {
        return withContext(Dispatchers.IO) {
            try {
                val extension = when {
                    // Try to get from Uri
                    imageUri.lastPathSegment?.contains(".") == true ->
                        imageUri.lastPathSegment?.substringAfterLast('.')?.lowercase()
                    // Fallback to detecting from bytes
                    else -> when {
                        imageBytes.size >= 2 && imageBytes[0] == 0xFF.toByte() && imageBytes[1] == 0xD8.toByte() -> "jpg"
                        imageBytes.size >= 8 && String(imageBytes.take(8).toByteArray()) == "PNG\r\n\u001a\n" -> "png"
                        else -> "jpg" // Default fallback
                    }
                } ?: "jpg"

                // Validate extension
                val safeExtension = when (extension.lowercase()) {
                    "jpg", "jpeg", "png", "gif" -> extension
                    else -> "jpg"
                }
                val fileName = "${UUID.randomUUID()}.$safeExtension"
                val fullPath = "$profileFolderPath/$fileName"

                val bucket = supabaseClient.storage.from(profileBucketId)
                bucket.upload(
                    path = fullPath,
                    data = imageBytes
                ) {
                    upsert = false
                }

                bucket.publicUrl(fullPath)
            } catch (e: Exception) {
                Log.d("CHKAZ","${e.message}")
                throw Exception("${e.message}")
            }
        }
    }

    fun fetchUserData(
        userId: String,
        result: (userData: UserEntity?, message: String, success: Boolean) -> Unit
    ) {
        val sanitizedMail = Utils.sanitizeEmailForFirebase(userId)

        firestore.collection("profiles")
            .document(sanitizedMail)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("name")
                    val email = document.getString("email")
                    if (name != null && email != null) {
                        val user = UserEntity(name, email, "")
                        result(user, "User data fetched successfully", true)
                    } else {
                        result(null, "User data not found", false)
                    }
                } else {
                    result(null, "User document not found", false)
                }
            }
            .addOnFailureListener {
                result(null, "Failed to fetch user data", false)
            }
    }

    fun logIn(userEntity: UserEntity, result: (message: String, success: Boolean) -> Unit) {
        firebaseAuth.signInWithEmailAndPassword(userEntity.email, userEntity.password)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    result("Login Successfully", true)
                } else {
                    result(it.exception!!.message.toString(), false)
                }
            }
    }

    fun getAllContacts(email: String): Flow<List<FriendContact>> = flow {
        val sanitizedMail = Utils.sanitizeEmailForFirebase(email)
        if (Utils.isInternetAvailable()) {
            val friendsList = mutableListOf<FriendContact>()
            currentUser?.let {
                val snapshot = firestore.collection("users")
                    .document(sanitizedMail)
                    .collection("friends")
                    .get()
                    .await()

                for (doc in snapshot.documents) {
                    doc.toObject(FriendContact::class.java)?.let { friendsList.add(it) }
                }
            }
            Log.d("CHKJA", friendsList.toString())
            emit(friendsList)
        } else {
            val friendsRoom = contactDao.getAllContacts().first()
            val newList = friendsRoom.map { friend ->
                FriendContact(
                    contact = friend.contact,
                    name = friend.name,
                    profileImageUrl = friend.profileImageUrl
                )
            }
            emit(newList)
        }
    }

    suspend fun insertContact(contact: Friend, email: String) {
        val sanitizedMail = Utils.sanitizeEmailForFirebase(email)
        contactDao.insertContact(contact)
        if (Utils.isInternetAvailable()) {
            currentUser?.let {
                val friendContact = FriendContact(
                    contact = contact.contact,
                    name = contact.name,
                    profileImageUrl = contact.profileImageUrl
                )
                firestore.collection("users")
                    .document(sanitizedMail)
                    .collection("friends")
                    .document(contact.contact)
                    .set(friendContact)
                    .await()
            }
        }
    }

    suspend fun updateContact(contact: Friend, email: String) {
        val sanitizedMail = Utils.sanitizeEmailForFirebase(email)
        contactDao.updateContact(contact)
        if (Utils.isInternetAvailable()) {
            currentUser?.let {
                val friendContact = FriendContact(
                    contact = contact.contact,
                    name = contact.name,
                    profileImageUrl = contact.profileImageUrl
                )
                firestore.collection("users")
                    .document(sanitizedMail)
                    .collection("friends")
                    .document(contact.contact)
                    .set(friendContact)
                    .await()
            }
        }
    }

    suspend fun deleteContact(contact: Friend, email: String) {
        val sanitizedMail = Utils.sanitizeEmailForFirebase(email)
        contactDao.deleteContact(contact)
        if (Utils.isInternetAvailable()) {
            currentUser?.let {
                firestore.collection("users")
                    .document(sanitizedMail)
                    .collection("friends")
                    .document(contact.contact)
                    .delete()
                    .await()
            }
        }
    }

    suspend fun insertContacts(contacts: List<Friend>, email: String) {
        val sanitizedMail = Utils.sanitizeEmailForFirebase(email)
        contactDao.insertContacts(contacts)

        if (Utils.isInternetAvailable()) {
            currentUser?.let {
                val batch = firestore.batch()
                contacts.forEach { contact ->
                    val friendContact = FriendContact(
                        contact = contact.contact,
                        name = contact.name,
                        profileImageUrl = contact.profileImageUrl
                    )
                    val docRef = firestore.collection("users")
                        .document(sanitizedMail)
                        .collection("friends")
                        .document(contact.contact)
                    batch.set(docRef, friendContact)
                }
                batch.commit().await()
            }
        }
    }

    suspend fun updateContacts(contacts: List<Friend>, email: String) {
        val sanitizedMail = Utils.sanitizeEmailForFirebase(email)
        contactDao.updateContacts(contacts)
        if (Utils.isInternetAvailable()) {
            currentUser?.let {
                val batch = firestore.batch()
                contacts.forEach { contact ->
                    val friendContact = FriendContact(
                        contact = contact.contact,
                        name = contact.name,
                        profileImageUrl = contact.profileImageUrl
                    )
                    val docRef = firestore.collection("users")
                        .document(sanitizedMail)
                        .collection("friends")
                        .document(contact.contact)
                    batch.set(docRef, friendContact)
                }
                batch.commit().await()
            }
        }
    }

    suspend fun getFriendByContactId(email: String, contactId: String): Friend? {
        val sanitizedMail = Utils.sanitizeEmailForFirebase(email)
        Log.d("CHKIS", "$email $contactId")

        return if (Utils.isInternetAvailable()) {
            try {
                val doc = firestore.collection("users")
                    .document(sanitizedMail)
                    .collection("friends")
                    .document(contactId)
                    .get()
                    .await()

                doc.toObject(Friend::class.java)
            } catch (e: Exception) {
                Log.e("Repo", "Failed to fetch friend: ${e.message}")
                null
            }
        } else {
            contactDao.getContactById(contactId)
        }
    }

    suspend fun saveFriendExpense(
        myEmail: String,
        friendContact: String,
        expense: ExpenseRecord,
        onResult: (success: Boolean, message: String) -> Unit
    ) {
        try {
            val sanitizedMyEmail = Utils.sanitizeEmailForFirebase(myEmail)
            val sanitizedFriendContact = Utils.sanitizeEmailForFirebase(friendContact)

            val expenseDoc = firestore.collection("expenses")
                .document(sanitizedMyEmail)
                .collection(sanitizedFriendContact)
                .document()

            val expenseWithId = expense.copy(
                expenseId = expenseDoc.id,
                timeStamp = System.currentTimeMillis()
            )

            expenseDoc.set(expenseWithId).await()
            onResult(true, "Expense saved successfully!")
        } catch (e: Exception) {
            Log.e("Repo", "Failed to save expense: ${e.message}")
            onResult(false, "Failed to save expense: ${e.message}")
        }
    }

    suspend fun updateFriendExpense(
        myEmail: String,
        friendContact: String,
        expenseId: String,
        updatedExpense: ExpenseRecord,
        onResult: (success: Boolean, message: String) -> Unit
    ) {
        try {
            val sanitizedMyEmail = Utils.sanitizeEmailForFirebase(myEmail)
            val sanitizedFriendContact = Utils.sanitizeEmailForFirebase(friendContact)

            firestore.collection("expenses")
                .document(sanitizedMyEmail)
                .collection(sanitizedFriendContact)
                .document(expenseId)
                .set(updatedExpense)
                .await()

            onResult(true, "Expense updated successfully!")
        } catch (e: Exception) {
            Log.e("Repo", "Failed to update expense: ${e.message}")
            onResult(false, "Failed to update expense: ${e.message}")
        }
    }

    suspend fun deleteFriendExpense(
        myEmail: String,
        friendContact: String,
        expenseId: String,
        onResult: (success: Boolean, message: String) -> Unit
    ) {
        try {
            val sanitizedMyEmail = Utils.sanitizeEmailForFirebase(myEmail)
            val sanitizedFriendContact = Utils.sanitizeEmailForFirebase(friendContact)

            firestore.collection("expenses")
                .document(sanitizedMyEmail)
                .collection(sanitizedFriendContact)
                .document(expenseId)
                .delete()
                .await()

            onResult(true, "Expense deleted successfully!")
        } catch (e: Exception) {
            Log.e("Repo", "Failed to delete expense: ${e.message}")
            onResult(false, "Failed to delete expense: ${e.message}")
        }
    }

    suspend fun getIndividualFriendExpenses(
        myEmail: String,
        friendContact: String,
        onResult: (success: Boolean, expenses: List<ExpenseRecord>?, message: String) -> Unit
    ) {
        try {
            val sanitizedMyEmail = Utils.sanitizeEmailForFirebase(myEmail)
            val sanitizedFriendContact = Utils.sanitizeEmailForFirebase(friendContact)

            // Reference to Firestore collection for a specific friend's expenses
            val expensesSnapshot = firestore.collection("expenses")
                .document(sanitizedMyEmail)
                .collection(sanitizedFriendContact)
                .get()
                .await()

            if (!expensesSnapshot.isEmpty) {
                val expenses = expensesSnapshot.documents.mapNotNull {
                    it.toObject(ExpenseRecord::class.java)
                }
                onResult(true, expenses, "Expenses retrieved successfully!")
            } else {
                onResult(false, null, "No expenses found for the friend.")
            }
        } catch (e: Exception) {
            Log.e("Repo", "Failed to retrieve expenses: ${e.message}")
            onResult(false, null, "Failed to retrieve expenses: ${e.message}")
        }
    }

    suspend fun getAllFriendExpenses(
        myEmail: String,
        onResult: (success: Boolean, expenses: Map<String, List<ExpenseRecord>>?, message: String) -> Unit
    ) {
        try {
            val sanitizedMyEmail = Utils.sanitizeEmailForFirebase(myEmail)

            // First, get the friends list to know which collections to query
            val friendsSnapshot = firestore.collection("users")
                .document(sanitizedMyEmail)
                .collection("friends")
                .get()
                .await()

            if (!friendsSnapshot.isEmpty) {
                val allExpenses = mutableMapOf<String, List<ExpenseRecord>>()

                // For each friend
                for (friendDoc in friendsSnapshot.documents) {
                    val friendContact = friendDoc.id

                    // Get expenses for this friend
                    val friendExpensesSnapshot = firestore.collection("expenses")
                        .document(sanitizedMyEmail)
                        .collection(friendContact)
                        .get()
                        .await()

                    val friendExpenses = friendExpensesSnapshot.documents.mapNotNull {
                        it.toObject(ExpenseRecord::class.java)
                    }

                    if (friendExpenses.isNotEmpty()) {
                        allExpenses[friendContact] = friendExpenses
                    }
                }

                if (allExpenses.isNotEmpty()) {
                    onResult(true, allExpenses, "All expenses retrieved successfully!")
                } else {
                    onResult(false, null, "No expenses found for any friend.")
                }
            } else {
                onResult(false, null, "No friends found.")
            }
        } catch (e: Exception) {
            Log.e("Repo", "Failed to retrieve all expenses: ${e.message}")
            onResult(false, null, "Failed to retrieve all expenses: ${e.message}")
        }
    }


}