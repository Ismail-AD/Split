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
    private val firebaseDatabase: FirebaseDatabase,
    val firebaseAuth: FirebaseAuth,
    val supabaseClient: SupabaseClient
) {

    val currentUser = firebaseAuth.currentUser
    private val bucketId = "groupImages"
    private val folderPath = "public/7pgyxj_1"

    suspend fun uploadImageAndSaveGroup(
        mail: String,
        imageUri: Uri?, // Make imageUri nullable
        imageBytes: ByteArray?, // Make imageUri nullable
        title: String,
        groupType: String,
        onSuccess: (String, String) -> Unit, // (message, groupId)
        onError: (String) -> Unit
    ) {
        try {
            val publicUrl = if (imageUri != null && imageBytes != null) {
                uploadImageToSupabase(imageUri, imageBytes)
            } else {
                "" // Use an empty string or a placeholder URL
            }

            val groupId = saveGroupToFirebase(publicUrl, title, groupType, mail)
            onSuccess("Group created successfully", groupId)
        } catch (e: Exception) {
            onError("Failed to create group: ${e.message}")
        }
    }

    private suspend fun uploadImageToSupabase(imageUri: Uri, imageBytes: ByteArray): String {
        return withContext(Dispatchers.IO) {
            try {
                val extension = imageUri.lastPathSegment?.substringAfterLast('.') ?: "jpg"
                val fileName = "${UUID.randomUUID()}.$extension"
                val fullPath = "$folderPath/$fileName"

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

    private suspend fun saveGroupToFirebase(
        imageUrl: String,
        title: String,
        groupType: String,
        mail: String
    ): String {
        return withContext(Dispatchers.IO) {
            try {
                val sanitizedMail = Utils.sanitizeEmailForFirebase(mail)
                val groupRef = firebaseDatabase.reference
                    .child("groups").child(sanitizedMail)
                    .push()

                val groupId = groupRef.key ?: throw Exception("Failed to generate group ID")
                val groupData = GroupMetaData(
                    groupId = groupId,
                    image = imageUrl,
                    title = title,
                    groupType = groupType
                )

                groupRef.setValue(groupData).await()
                groupId
            } catch (e: Exception) {
                throw Exception("Failed to save group data: ${e.message}")
            }
        }
    }

    suspend fun getAllGroups(mail: String, onSuccess: (List<GroupMetaData>) -> Unit, onError: (String) -> Unit) {
        try {
            val sanitizedMail = Utils.sanitizeEmailForFirebase(mail)
            val groupsRef = firebaseDatabase.reference
                .child("groups").child(sanitizedMail)

            val snapshot = groupsRef.get().await()
            val groups = mutableListOf<GroupMetaData>()

            if (snapshot.exists()) {
                for (groupSnapshot in snapshot.children) {
                    val group = groupSnapshot.getValue(GroupMetaData::class.java)
                    group?.let { groups.add(it) }
                }
            }

            onSuccess(groups)
        } catch (e: Exception) {
            Log.d("CJKA","${e.message}")
            onError("Failed to fetch groups: ${e.message}")
        }
    }




    fun signUp(
        userEntity: UserEntity,
        uri: Uri?,
        result: (message: String, success: Boolean) -> Unit
    ) {
        val sanitizedMail = Utils.sanitizeEmailForFirebase(userEntity.email)
        firebaseAuth.createUserWithEmailAndPassword(userEntity.email, userEntity.password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = firebaseAuth.currentUser?.uid ?: return@addOnCompleteListener
                    val userProfile = hashMapOf(
                        "name" to userEntity.name,
                        "email" to userEntity.email
                    )

                    firebaseDatabase.reference.child("profiles").child(sanitizedMail)
                        .setValue(userProfile)
                        .addOnSuccessListener {
                            result("Account created successfully", true)
                        }
                        .addOnFailureListener {
                            result("Account created, but data upload failed", false)
                        }
                } else {
                    result(task.exception!!.message.toString(), false)
                }
            }
    }

    fun fetchUserData(
        userId: String,
        result: (userData: UserEntity?, message: String, success: Boolean) -> Unit
    ) {
        val sanitizedMail = Utils.sanitizeEmailForFirebase(userId)
        Log.d("CHKSHOT", userId)
        Log.d("CHKSHOT", sanitizedMail)

        firebaseDatabase.reference.child("profiles").child(sanitizedMail).get()
            .addOnSuccessListener { snapshot ->
                val name = snapshot.child("name").value.toString()
                val email = snapshot.child("email").value.toString()
                Log.d("CHKSHOT", "Got it")
                if (name != null && email != null) {
                    val user = UserEntity(name, email, "")
                    result(user, "User data fetched successfully", true)
                } else {
                    result(null, "User data not found", false)
                }
            }
            .addOnFailureListener {
                Log.d("CHKSHOT", "fail")

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


    //--------------------------ADD CONTACT------------------------------

//    fun getAllContactsFromRoom(): Flow<List<Friend>> = contactDao.getAllContacts()

    fun getAllContacts(email: String): Flow<List<FriendContact>> = flow {
        val sanitizedMail = Utils.sanitizeEmailForFirebase(email)
        if (Utils.isInternetAvailable()) {
            // Fetch from Firebase
            val friendsList = mutableListOf<FriendContact>()
            currentUser?.let { user ->
                val snapshot =
                    firebaseDatabase.reference.child("users").child(sanitizedMail).child("friends")
                        .get()
                        .await()
                for (data in snapshot.children) {
                    data.getValue(FriendContact::class.java)?.let { friendsList.add(it) }
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
            currentUser?.let { user ->
                val friendContact = FriendContact(
                    contact = contact.contact,
                    name = contact.name,
                    profileImageUrl = contact.profileImageUrl
                )
                firebaseDatabase.reference.child("users").child(sanitizedMail).child("friends")
                    .child(contact.contact).setValue(friendContact).await()
            }
        }
    }

    // Update Contact
    suspend fun updateContact(contact: Friend, email: String) {
        val sanitizedMail = Utils.sanitizeEmailForFirebase(email)
        contactDao.updateContact(contact)
        if (Utils.isInternetAvailable()) {
            currentUser?.let { user ->
                val friendContact = FriendContact(
                    contact = contact.contact,
                    name = contact.name,
                    profileImageUrl = contact.profileImageUrl
                )
                firebaseDatabase.reference.child("users").child(sanitizedMail).child("friends")
                    .child(contact.contact).setValue(friendContact).await()
            }
        }
    }

    // Delete Contact
    suspend fun deleteContact(contact: Friend, email: String) {
        val sanitizedMail = Utils.sanitizeEmailForFirebase(email)
        contactDao.deleteContact(contact)
        if (Utils.isInternetAvailable()) {
            currentUser?.let { user ->
                firebaseDatabase.reference.child("users").child(sanitizedMail).child("friends")
                    .child(contact.contact).removeValue().await()
            }
        }
    }

    // Get Contact by Contact
//    suspend fun getContactByContact(contact: String): Friend? {
//        return if (Utils.isInternetAvailable()) {
//            currentUser?.let { user ->
//                val snapshot =
//                    firebaseDatabase.reference.child("users").child(email).child("friends")
//                        .child(contact).get().await()
//                snapshot.getValue(Friend::class.java)
//            }
//        } else {
//            contactDao.getContactByName(contact)
//        }
//    }

    suspend fun insertContacts(contacts: List<Friend>, email: String) {
        val sanitizedMail = Utils.sanitizeEmailForFirebase(email)
        // Insert into local Room database
        contactDao.insertContacts(contacts)

        // If internet is available, sync with Firebase
        Log.d("CHKME", Utils.isInternetAvailable().toString())
        if (Utils.isInternetAvailable()) {
            currentUser?.let { user ->
                val userFriendsRef = firebaseDatabase.reference
                    .child("users")
                    .child(sanitizedMail)
                    .child("friends")

                // Batch insert contacts to Firebase
                contacts.forEach { contact ->
                    val friendContact = FriendContact(
                        contact = contact.contact,
                        name = contact.name,
                        profileImageUrl = contact.profileImageUrl
                    )
                    userFriendsRef
                        .child(contact.contact)
                        .setValue(friendContact)
                        .await()
                }
            }
        }
    }

    suspend fun updateContacts(contacts: List<Friend>, email: String) {
        val sanitizedMail = Utils.sanitizeEmailForFirebase(email)
        contactDao.updateContacts(contacts)
        if (Utils.isInternetAvailable()) {
            currentUser?.let { user ->
                val userFriendsRef = firebaseDatabase.reference
                    .child("users")
                    .child(sanitizedMail)
                    .child("friends")

                // Batch insert contacts to Firebase
                contacts.forEach { contact ->
                    val friendContact = FriendContact(
                        contact = contact.contact,
                        name = contact.name,
                        profileImageUrl = contact.profileImageUrl
                    )
                    userFriendsRef
                        .child(contact.contact)
                        .setValue(friendContact)
                        .await()
                }
            }
        }
    }

    suspend fun getFriendByContactId(email: String, contactId: String): Friend? {
        val sanitizedMail = Utils.sanitizeEmailForFirebase(email)
        Log.d("CHKIS", "$email $contactId")

        return if (Utils.isInternetAvailable()) {
            // Fetch from Firebase if internet is available
            val friendRef = firebaseDatabase.reference
                .child("users")
                .child(sanitizedMail)
                .child("friends")
                .child(contactId)

            try {
                val snapshot = friendRef.get().await()
                snapshot.getValue(Friend::class.java) // Deserialize to Friend object
            } catch (e: Exception) {
                Log.e("Repo", "Failed to fetch friend: ${e.message}")
                null
            }
        } else {
            // Fallback to local Room database if offline
            contactDao.getContactById(contactId)
        }
    }


    //    suspend fun insertContacts(contacts: List<Friend>) = contactDao.insertContacts(contacts)
//    suspend fun getContactById(id: Int): Friend? = contactDao.getContactById(id)
//    suspend fun getContactByName(name: String): Friend? = contactDao.getContactByName(name)

//    suspend fun insertContact(contact: Friend) = contactDao.insertContact(contact)
//
//    suspend fun updateContact(contact: Friend) = contactDao.updateContact(contact)
//    suspend fun updateContacts(contacts: List<Friend>) = contactDao.updateContacts(contacts)
//
//    suspend fun deleteContact(contact: Friend) = contactDao.deleteContact(contact)


    //------------------------------Individual Expense---------------------------------------
    suspend fun saveFriendExpense(
        myEmail: String,
        friendContact: String,
        expense: ExpenseRecord,
        onResult: (success: Boolean, message: String) -> Unit
    ) {
        try {
            val sanitizedMyEmail = Utils.sanitizeEmailForFirebase(myEmail)
            val sanitizedFriendContact = Utils.sanitizeEmailForFirebase(friendContact)

            val expenseRef = firebaseDatabase.reference
                .child("expenses")
                .child(sanitizedMyEmail)
                .child(sanitizedFriendContact).push()

            expenseRef.key?.let {
                val expenseWithId =
                    expense.copy(expenseId = it, timeStamp = System.currentTimeMillis())
                expenseRef.setValue(expenseWithId).await()
                onResult(true, "Expense saved successfully!")
            }
        } catch (e: Exception) {
            // Handle and log exceptions
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

            // Reference to the specific expense in Firebase
            val expenseRef = firebaseDatabase.reference
                .child("expenses")
                .child(sanitizedMyEmail)
                .child(sanitizedFriendContact)
                .child(expenseId)

            // Update the expense
            expenseRef.setValue(updatedExpense).await()
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

            // Reference to the specific expense in Firebase
            val expenseRef = firebaseDatabase.reference
                .child("expenses")
                .child(sanitizedMyEmail)
                .child(sanitizedFriendContact)
                .child(expenseId)

            // Delete the expense
            expenseRef.removeValue().await()
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

            // Reference to Firebase node for a specific friend
            val expenseRef = firebaseDatabase.reference
                .child("expenses")
                .child(sanitizedMyEmail)
                .child(sanitizedFriendContact)

            // Retrieve data
            val snapshot = expenseRef.get().await()

            if (snapshot.exists()) {
                val expenses =
                    snapshot.children.mapNotNull { it.getValue(ExpenseRecord::class.java) }
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

            // Reference to Firebase node for all friends
            val expenseRef = firebaseDatabase.reference
                .child("expenses")
                .child(sanitizedMyEmail)

            // Retrieve data
            val snapshot = expenseRef.get().await()

            if (snapshot.exists()) {
                val allExpenses = mutableMapOf<String, List<ExpenseRecord>>()

                for (friendSnapshot in snapshot.children) {
                    val friendContact = friendSnapshot.key ?: continue
                    val friendExpenses =
                        friendSnapshot.children.mapNotNull { it.getValue(ExpenseRecord::class.java) }
                    allExpenses[friendContact] = friendExpenses
                }

                onResult(true, allExpenses, "All expenses retrieved successfully!")
            } else {
                onResult(false, null, "No expenses found for any friend.")
            }
        } catch (e: Exception) {
            Log.e("Repo", "Failed to retrieve all expenses: ${e.message}")
            onResult(false, null, "Failed to retrieve all expenses: ${e.message}")
        }
    }


}