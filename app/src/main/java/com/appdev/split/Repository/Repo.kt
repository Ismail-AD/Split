package com.appdev.split.Repository

import android.net.Uri
import android.util.Log
import com.appdev.split.Model.Data.Contact
import com.appdev.split.Model.Data.ExpenseRecord
import com.appdev.split.Model.Data.Friend
import com.appdev.split.Model.Data.FriendContact
import com.appdev.split.Model.Data.GroupMembersWrapper
import com.appdev.split.Model.Data.GroupMetaData
import com.appdev.split.Model.Data.UserEntity
import com.appdev.split.Room.DaoClasses.ContactDao
import com.appdev.split.Utils.Utils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
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
    private val profileFolderPath = "public/1cp17k1_1"

    //----------------------MANGE GROUP EXPENSE------------------

    suspend fun saveGroupExpense(
        groupId: String,
        expense: ExpenseRecord,
        onResult: (success: Boolean, message: String) -> Unit
    ) {
        try {

            val groupExpensesCollection = firestore.collection("groupExpenses")
                .document(groupId)
                .collection("expenses") // Create a subcollection for expenses

            // Generate a new document with auto-generated ID in the expenses subcollection
            val newExpenseDoc = groupExpensesCollection.document()

            // Add ID and timestamp to the expense record
            val expenseWithId = expense.copy(
                id = newExpenseDoc.id,
                timeStamp = System.currentTimeMillis()
            )

            // Save the expense in the subcollection
            newExpenseDoc.set(expenseWithId).await()
            onResult(true, "Expense saved successfully!")
        } catch (e: Exception) {
            Log.e("Repo", "Failed to save expense: ${e.message}")
            onResult(false, "Failed to save expense: ${e.message}")
        }
    }

    suspend fun updateGroupExpense(
        groupId: String,
        expenseId: String,
        updatedExpense: ExpenseRecord,
        onResult: (success: Boolean, message: String) -> Unit
    ) {
        try {
            // Make sure to preserve the original expenseId and update timestamp
            val expenseToUpdate = updatedExpense.copy(
                id = expenseId,
                timeStamp = System.currentTimeMillis()
            )

            firestore.collection("groupExpenses")
                .document(groupId)
                .collection("expenses")
                .document(expenseId)
                .set(expenseToUpdate)
                .await()

            onResult(true, "Group expense updated successfully!")
        } catch (e: Exception) {
            Log.e("Repo", "Failed to update group expense: ${e.message}")
            onResult(false, "Failed to update group expense: ${e.message}")
        }
    }

    suspend fun getAllGroupExpenses(
        groupId: String,
        onResult: (success: Boolean, expenses: List<ExpenseRecord>?, message: String) -> Unit
    ) {
        try {
            val expensesSnapshot = firestore.collection("groupExpenses")
                .document(groupId)
                .collection("expenses")
                .get()
                .await()

            if (!expensesSnapshot.isEmpty) {
                val expenses = expensesSnapshot.documents.mapNotNull { document ->
                    try {
                        document.toObject(ExpenseRecord::class.java)
                    } catch (e: Exception) {
                        Log.e("Repo", "Failed to map document to ExpenseRecord: ${document.id}, error: ${e.message}")
                        null
                    }
                }

                if (expenses.isNotEmpty()) {
                    onResult(true, expenses, "Group expenses retrieved successfully!")
                } else {
                    onResult(false, null, "No expenses found for this group.")
                }
            } else {
                onResult(false, null, "No expenses found for this group.")
            }
        } catch (e: Exception) {
            Log.e("Repo", "Failed to retrieve group expenses: ${e.message}")
            onResult(false, null, "Failed to retrieve group expenses: ${e.message}")
        }
    }



    //----------------------MANAGE GROUP MEMBERS-----------------

    suspend fun addMembersToGroup(
        groupId: String,
        newMembers: List<FriendContact>,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val groupRef = firestore.collection("groupMembers")
                .document(groupId)

            // Get existing members
            val existingMembersDoc = groupRef.get().await()

            // Get current members or empty list if none exist
            val existingMembers = if (existingMembersDoc.exists()) {
                existingMembersDoc.toObject(GroupMembersWrapper::class.java)?.members
                    ?: emptyList()
            } else {
                emptyList()
            }

            // Combine existing and new members, removing duplicates
            val updatedMembers = (existingMembers + newMembers).distinctBy { it.contact }

            // Update Firestore with combined list
            groupRef.set(GroupMembersWrapper(updatedMembers)).await()

            onSuccess("Members added successfully")
        } catch (e: Exception) {
            Log.d("CHKJM", "${e.message}")
            onError("Failed to add members: ${e.message}")
        }
    }

    suspend fun getGroupMembers(
        groupId: String,
        onSuccess: (List<FriendContact>) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val groupRef = firestore.collection("groupMembers")
                .document(groupId)

            // Fetch the group members document
            val groupMembersDoc = groupRef.get().await()

            if (groupMembersDoc.exists()) {
                // Convert document to GroupMembersWrapper and extract members list
                val membersWrapper = groupMembersDoc.toObject(GroupMembersWrapper::class.java)
                val members = membersWrapper?.members ?: emptyList()

                onSuccess(members)
            } else {
                // If document doesn't exist, return empty list
                onSuccess(emptyList())
            }
        } catch (e: Exception) {
            Log.d("CHKJM", "Error fetching group members: ${e.message}")
            onError("Failed to fetch group members: ${e.message}")
        }
    }


    suspend fun uploadImageAndSaveGroup(
        userid: String,
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

            val groupId = saveGroupToFirestore(publicUrl, title, groupType, userid)
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
        userid: String
    ): String {
        return withContext(Dispatchers.IO) {
            try {
//                val sanitizedMail = Utils.sanitizeEmailForFirebase(mail)
                val groupsCollection = firestore.collection("groups")
                    .document(userid)
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




    suspend fun getAllGroups(userid: String, onSuccess: (List<GroupMetaData>) -> Unit, onError: (String) -> Unit) {
        try {
//            val sanitizedMail = Utils.sanitizeEmailForFirebase(mail)
            val groupsSnapshot = firestore.collection("groups")
                .document(userid)
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

//            val sanitizedMail = Utils.sanitizeEmailForFirebase(userEntity.email)
            firebaseAuth.createUserWithEmailAndPassword(userEntity.email, userEntity.password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userId = firebaseAuth.currentUser?.uid ?: return@addOnCompleteListener
                        val userProfile = hashMapOf(
                            "name" to userEntity.name,
                            "email" to userEntity.email,
                            "profileImage" to profileImageUrl,  // Add profile image URL
                            "userId" to userId
                        )

                        firestore.collection("profiles")
                            .document(userId)
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
                    val url = document.getString("profileImage")
                    Log.d("CHKUSER", "here is url : $url")

                    if (name != null && email != null) {
                        val user = UserEntity(name, email, "", imageUrl = url)
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

    fun getAllContacts(myUserId: String): Flow<List<FriendContact>> = flow {
//        val sanitizedMail = Utils.sanitizeEmailForFirebase(email)
//        if (Utils.isInternetAvailable()) {
            val friendsList = mutableListOf<FriendContact>()
            currentUser?.let {
                val snapshot = firestore.collection("users")
                    .document(myUserId)
                    .collection("friends")
                    .get()
                    .await()

                for (doc in snapshot.documents) {
                    doc.toObject(FriendContact::class.java)?.let { friendsList.add(it) }
                }
            }
            Log.d("CHKJA", friendsList.toString())
            emit(friendsList)
//        } else {
//            val friendsRoom = contactDao.getAllContacts().first()
//            val newList = friendsRoom.map { friend ->
//                FriendContact(
//                    contact = friend.contact,
//                    name = friend.name,
//                    profileImageUrl = friend.profileImageUrl
//                )
//            }
//            emit(newList)
//        }
    }

    suspend fun insertContact(contact: Friend, myUserId: String, onResult: (success: Boolean, message: String) -> Unit) {
//        val sanitizedMail = Utils.sanitizeEmailForFirebase(email)
        try {
            contactDao.insertContact(contact)
            if (Utils.isInternetAvailable()) {
                currentUser?.let {
                    val friendContact = FriendContact(
                        friendId = it.uid,
                        contact = contact.contact,
                        name = contact.name,
                        profileImageUrl = contact.profileImageUrl
                    )
                    firestore.collection("users")
                        .document(myUserId)
                        .collection("friends")
                        .document(contact.contact)
                        .set(friendContact)
                        .await()
                    onResult(true, "Contact added successfully")
                }
            }
        }catch (e:Exception){
            onResult(false, "Failed to save contact: ${e.message}")
        }
    }


    suspend fun updateContact(contact: Friend, myId: String,onResult: (success: Boolean, message: String) -> Unit) {
//        val sanitizedMail = Utils.sanitizeEmailForFirebase(email)
        try {
            contactDao.updateContact(contact)
            if (Utils.isInternetAvailable()) {
                currentUser?.let {
                    val friendContact = FriendContact(
                        friendId = it.uid,
                        contact = contact.contact,
                        name = contact.name,
                        profileImageUrl = contact.profileImageUrl
                    )
                    firestore.collection("users")
                        .document(myId)
                        .collection("friends")
                        .document(contact.contact)
                        .set(friendContact)
                        .await()
                    onResult(true, "Contact updated successfully")
                }
            }
        } catch (e: Exception) {
            onResult(false, "Failed to delete contact: ${e.message}")
        }
    }

    suspend fun deleteContact(
        contact: Friend,
        email: String,
        onResult: (success: Boolean, message: String) -> Unit
    ) {
        try {
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
                    onResult(true, "Contact deleted successfully")
                }
            }
        } catch (e: Exception) {
            onResult(false, "Failed to delete contact: ${e.message}")
        }
    }

    suspend fun insertContacts(
        contacts: List<Friend>,
        selectedContacts: MutableList<Contact>,
        myId: String,
        onResult: (success: Boolean, message: String) -> Unit
    ) {
        try {
            contactDao.insertContacts(contacts)
            if (Utils.isInternetAvailable()) {
                currentUser?.let {
                    val batch = firestore.batch()
                    selectedContacts.forEach { contact ->
                        val friendContact = FriendContact(
                            friendId = contact.friendId,
                            contact = contact.number,
                            name = contact.name,
                            profileImageUrl = contact.imageUrl
                        )
                        val docRef = firestore.collection("users")
                            .document(myId)
                            .collection("friends")
                            .document(contact.friendId)
                        batch.set(docRef, friendContact)
                    }
                    batch.commit().await()
                    onResult(true, "Contacts inserted successfully")
                }
            }
        } catch (e: Exception) {
            onResult(false, "Failed to insert contacts: ${e.message}")
        }
    }

    suspend fun updateContacts(
        contacts: List<Friend>,
        myId: String,
        onResult: (success: Boolean, message: String) -> Unit
    ) {
        try {
            contactDao.updateContacts(contacts)
            if (Utils.isInternetAvailable()) {
                currentUser?.let {
                    val batch = firestore.batch()
                    contacts.forEach { contact ->
                        val friendContact = FriendContact(
                            friendId = it.uid,
                            contact = contact.contact,
                            name = contact.name,
                            profileImageUrl = contact.profileImageUrl
                        )
                        val docRef = firestore.collection("users")
                            .document(myId)
                            .collection("friends")
                            .document(contact.contact)
                        batch.set(docRef, friendContact)
                    }
                    batch.commit().await()
                    onResult(true, "Contacts updated successfully")
                }
            }
        } catch (e: Exception) {
            onResult(false, "Failed to update contacts: ${e.message}")
        }
    }

    suspend fun getFriendByContactId(myId: String, friendId: String): FriendContact? {
        return try {
            val doc = firestore.collection("users")
                .document(myId)
                .collection("friends")
                .document(friendId)
                .get()
                .await()

            doc.toObject(FriendContact::class.java)
        } catch (e: Exception) {
            Log.e("Repo", "Failed to fetch friend: ${e.message}")
            null
        }
//        if (Utils.isInternetAvailable()) {

//        } else {
//            contactDao.getContactById(friendId)
//        }
    }

    suspend fun saveFriendExpense(
        myUserId: String,
        myFriendId: String,
        expense: ExpenseRecord,
        onResult: (success: Boolean, message: String) -> Unit
    ) {
        try {
//            val sanitizedMyEmail = Utils.sanitizeEmailForFirebase(myEmail)
//            val sanitizedFriendContact = Utils.sanitizeEmailForFirebase(friendContact)

            val expenseDoc = firestore.collection("expenses")
                .document(myUserId)
                .collection(myFriendId)
                .document()

            val expenseWithId = expense.copy(
                id = expenseDoc.id,
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
        myUserId: String,
        friendId: String,
        expenseId: String,
        updatedExpense: ExpenseRecord,
        onResult: (success: Boolean, message: String) -> Unit
    ) {
        try {
//            val sanitizedMyEmail = Utils.sanitizeEmailForFirebase(myEmail)
//            val sanitizedFriendContact = Utils.sanitizeEmailForFirebase(friendContact)

            firestore.collection("expenses")
                .document(myUserId)
                .collection(friendId)
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
        myUserId: String,
        friendId: String,
        expenseId: String,
        onResult: (success: Boolean, message: String) -> Unit
    ) {
        try {

            firestore.collection("expenses")
                .document(myUserId)
                .collection(friendId)
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
        myUserId: String,
        onResult: (success: Boolean, expenses: Map<String, List<ExpenseRecord>>?, message: String) -> Unit
    ) {
        try {
            val friendsSnapshot = firestore.collection("users")
                .document(myUserId)
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
                        .document(myUserId)
                        .collection(friendContact)
                        .get()
                        .await()
                    Log.d("CHKIU","FRIEND SHOT :${friendExpensesSnapshot.isEmpty}")

                    val friendExpenses = friendExpensesSnapshot.documents.mapNotNull { document ->
                        try {
                            val expense = document.toObject(ExpenseRecord::class.java)
                            Log.d("CHKIU", "Mapped ExpenseRecord: $expense")
                            expense
                        } catch (e: Exception) {
                            Log.e("CHKIU", "Failed to map document to ExpenseRecord: ${document.id}, error: ${e.message}")
                            null
                        }
                    }
                    Log.d("CHKIU", "FRIEND OBJECT: $friendExpenses")

                    Log.d("CHKIU","FRIEND OBJECT :${friendExpenses}")

                    if (friendExpenses.isNotEmpty()) {
                        allExpenses[friendContact] = friendExpenses
                    }
                }
                Log.d("CHKIU","$allExpenses")

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