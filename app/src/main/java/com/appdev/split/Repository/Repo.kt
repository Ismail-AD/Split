package com.appdev.split.Repository

import android.net.Uri
import android.os.Bundle
import android.util.Log
import com.appdev.split.Model.Data.Contact
import com.appdev.split.Model.Data.ExpenseRecord
import com.appdev.split.Model.Data.Friend
import com.appdev.split.Model.Data.FriendContact
import com.appdev.split.Model.Data.FriendExpenseRecord
import com.appdev.split.Model.Data.GroupMembersWrapper
import com.appdev.split.Model.Data.GroupMetaData
import com.appdev.split.Model.Data.MySpending
import com.appdev.split.Model.Data.UserEntity
import com.appdev.split.Room.DaoClasses.ContactDao
import com.appdev.split.Utils.Utils
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
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
    private val firestore: FirebaseFirestore,
    val firebaseAuth: FirebaseAuth,
    val supabaseClient: SupabaseClient,
    var firebaseAnalytics: FirebaseAnalytics
) {

    val currentUser = firebaseAuth.currentUser
    private val bucketId = "groupImages"
    private val folderPath = "public/7pgyxj_1"

    private val profileBucketId = "profileImages"
    private val profileFolderPath = "public/1cp17k1_1"

    fun getCurrentUserId(): String {
        return firebaseAuth.currentUser?.uid ?: ""
    }


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
            logExpenseAdded(expense)
            onResult(true, "Expense saved successfully!")
        } catch (e: Exception) {
            e.localizedMessage?.let { logExpenseError(it,groupId) }
            Log.e("Repo", "Failed to save expense: ${e.message}")
            onResult(false, "Failed to save expense: ${e.message}")
        }
    }
    private fun logExpenseAdded(expense: ExpenseRecord) {

        // Custom event for expense tracking
        val expenseParams = Bundle().apply {
            putDouble("amount", expense.totalAmount)
            putString("category", expense.expenseCategory)
            putString("currency", Utils.extractCurrencyCode(expense.currency))
            putString("split_type", expense.splitType)
            putBoolean("is_update", expense.id.trim().isNotEmpty())
            putInt("member_count", expense.splits.size)
        }
        firebaseAnalytics.logEvent("group_expense_added", expenseParams)
    }


    suspend fun deleteGroupExpense(
        groupId: String,
        expenseId: String,
        onResult: (success: Boolean, message: String) -> Unit
    ) {
        try {
            val groupExpensesCollection = firestore.collection("groupExpenses")
                .document(groupId)
                .collection("expenses")

            groupExpensesCollection.document(expenseId)
                .delete()
                .await()

            onResult(true, "Expense deleted successfully!")
        } catch (e: Exception) {
            Log.e("Repo", "Failed to delete expense: ${e.message}")
            onResult(false, "Failed to delete expense: ${e.message}")
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
                        Log.e(
                            "Repo",
                            "Failed to map document to ExpenseRecord: ${document.id}, error: ${e.message}"
                        )
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
            val groupRef = firestore.collection("groups").document(groupId)

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(groupRef)

                if (!snapshot.exists()) {
                    throw Exception("Group not found")
                }

                val group = snapshot.toObject(GroupMetaData::class.java)
                    ?: throw Exception("Invalid group data")

                val updatedMembers = (group.members + newMembers).distinctBy { it.friendId }
                val updatedMemberIds = (group.memberIds + newMembers.map { it.friendId }).distinct()

                transaction.update(groupRef, "members", updatedMembers)
                transaction.update(groupRef, "memberIds", updatedMemberIds)
            }.await()

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
            val groupSnapshot = firestore.collection("groups").document(groupId).get().await()

            if (groupSnapshot.exists()) {
                val members =
                    groupSnapshot.toObject(GroupMetaData::class.java)?.members ?: emptyList()
                onSuccess(members)
            } else {
                onSuccess(emptyList())
            }
        } catch (e: Exception) {
            Log.d("CHKJM", "Error fetching group members: ${e.message}")
            onError("Failed to fetch group members: ${e.message}")
        }
    }


    suspend fun uploadImageAndSaveGroup(
        myData: FriendContact,
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

            val groupId = saveGroupToFirestore(myData, publicUrl, title, groupType, userid)
            onSuccess("Group created successfully", groupId)
        } catch (e: Exception) {
            Log.d("CHKJM", "${e.message}")
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
                        imageBytes.size >= 8 && String(
                            imageBytes.take(8).toByteArray()
                        ) == "PNG\r\n\u001a\n" -> "png"

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
                Log.d("CHKJM", "$fullPath")

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
        myData: FriendContact,
        imageUrl: String,
        title: String,
        groupType: String,
        userid: String
    ): String {
        return withContext(Dispatchers.IO) {
            try {
//                val sanitizedMail = Utils.sanitizeEmailForFirebase(mail)
                val groupsCollection = firestore.collection("groups")


                val groupRef = groupsCollection.document()
                val groupId = groupRef.id

                val groupData = GroupMetaData(
                    groupId = groupId,
                    image = imageUrl,
                    title = title,
                    groupType = groupType,
                    createdBy = userid,
                    members = listOf(myData),
                    memberIds = listOf(userid)
                )

                groupRef.set(groupData).await()
                groupId
            } catch (e: Exception) {
                throw Exception("Failed to save group data: ${e.message}")
            }
        }
    }


    suspend fun getAllGroups(
        userId: String,
        onSuccess: (List<GroupMetaData>) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val groupsSnapshot = firestore.collection("groups")
                .whereArrayContains("memberIds", userId)
                .get()
                .await()

            val groups =
                groupsSnapshot.documents.mapNotNull { it.toObject(GroupMetaData::class.java) }

            onSuccess(groups)
        } catch (e: Exception) {
            Log.d("CJKA", "Error fetching groups: ${e.message}")
            onError("Failed to fetch groups: ${e.message}")
        }
    }


    suspend fun updateUserName(
        userId: String,
        newName: String,
        result: (message: String, success: Boolean) -> Unit
    ) {
        try {
            // Update only the name field in Firestore
            firestore.collection("profiles")
                .document(userId)
                .update("name", newName)
                .addOnSuccessListener {
                    result("Name updated successfully", true)
                }
                .addOnFailureListener { e ->
                    Log.e("CHKAZ", "Failed to update name: ${e.message}")
                    result("Failed to update name", false)
                }
        } catch (e: Exception) {
            Log.e("CHKAZ", "Exception while updating name: ${e.message}")
            result("Failed to update name: ${e.message}", false)
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
                            .addOnFailureListener { e ->
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
                        imageBytes.size >= 8 && String(
                            imageBytes.take(8).toByteArray()
                        ) == "PNG\r\n\u001a\n" -> "png"

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
                Log.d("CHKAZ", "${e.message}")
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

    suspend fun insertContact(
        contact: Friend,
        myUserId: String,
        onResult: (success: Boolean, message: String) -> Unit
    ) {
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
        } catch (e: Exception) {
            onResult(false, "Failed to save contact: ${e.message}")
        }
    }


    suspend fun updateContact(
        contact: Friend,
        myId: String,
        onResult: (success: Boolean, message: String) -> Unit
    ) {
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
                        Log.d("CHKZMA","${contact.friendId}")

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
            Log.d("CHKZMA","${e.localizedMessage}")
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

            val friend = doc.toObject(FriendContact::class.java)
            friend
        } catch (e: Exception) {
            Log.e("Repo", "Failed to fetch friend: ${e.message}")
            null
        }
    }

    suspend fun getProfileById(userId: String): FriendContact? {

        return try {
            val doc = firestore.collection("profiles")
                .document(userId)
                .get()
                .await()
            if (doc.exists()) {
                val name = doc.getString("name") ?: ""
                val email = doc.getString("email") ?: ""
                val profileImage = doc.getString("profileImage") ?: ""
                val friendId = doc.getString("userId") ?: ""

                FriendContact(
                    name = name,
                    profileImageUrl = profileImage,
                    contact = email,
                    friendId = friendId
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("Repo", "Failed to fetch profile: ${e.message}")
            null
        }
    }

//-------------------------EXPENSES---------------------------

    suspend fun settleFriendExpense(
        myUserId: String,
        expenseId: String,
        updatedExpense: FriendExpenseRecord,
        onResult: (Boolean, String) -> Unit
    ) {
        try {
            val batch = firestore.batch()

            Log.d("CHKAZX","Current user id ${myUserId}")
            Log.d("CHKAZX","friend user id ${updatedExpense.friendId}")
            // Update the expense record in both user's collections
            val myExpenseRef = firestore.collection("expenses")
                .document(myUserId)
                .collection("friendsExpenses")
                .document(expenseId)

            val friendExpenseRef = firestore.collection("expenses")
                .document(updatedExpense.paidBy)
                .collection("friendsExpenses")
                .document(expenseId)

            batch.set(myExpenseRef, updatedExpense)
            batch.set(friendExpenseRef, updatedExpense)

            batch.commit().await()
            onResult(true, "Expense settled successfully!")
        } catch (e: Exception) {
            Log.e("Repo", "Failed to settle expense: ${e.message}")
            onResult(false, "Failed to settle expense: ${e.message}")
        }
    }

    suspend fun settleGroupExpense(
        groupId: String,
        expenseId: String,
        updatedExpense: ExpenseRecord,
        onResult: (Boolean, String) -> Unit
    ) {
        try {
            // Update the expense in the group's expenses collection
            firestore.collection("groupExpenses")
                .document(groupId)
                .collection("expenses")
                .document(expenseId)
                .set(updatedExpense)
                .await()

            onResult(true, "Group expense settled successfully!")
        } catch (e: Exception) {
            Log.e("Repo", "Failed to settle group expense: ${e.message}")
            onResult(false, "Failed to settle group expense: ${e.message}")
        }
    }
    suspend fun saveFriendExpense(
        myUserId: String,
        expense: FriendExpenseRecord,
        onResult: (Boolean, String) -> Unit
    ) {
        val myContribution = expense.splits.find { it.userId == myUserId }?.amount ?: 0.0
        val expenseDocId = firestore.collection("expenses")
            .document(myUserId)
            .collection("friendsExpenses")
            .document().id

        val expenseWithId = expense.copy(id = expenseDocId, timeStamp = System.currentTimeMillis())

        try {
            val batch = firestore.batch()

            val myExpenseRef = firestore.collection("expenses")
                .document(myUserId)
                .collection("friendsExpenses")
                .document(expenseDocId)

            val friendExpenseRef = firestore.collection("expenses")
                .document(expense.friendId)
                .collection("friendsExpenses")
                .document(expenseDocId)

            batch.set(myExpenseRef, expenseWithId)
            batch.set(friendExpenseRef, expenseWithId)

            batch.commit().await()

            updateTotalExpense(myContribution, expense.startDate, myUserId)
            updateTotalExpense(myContribution, expense.startDate, expense.friendId)
            logExpenseAdded(expense)
            onResult(true, "Expense saved successfully!")
        } catch (e: Exception) {
            Log.e("Repo", "${expense.friendId}  Failed to save expense: ${e.message}")
            e.localizedMessage?.let { logExpenseError(it) }
            onResult(false, "Failed to save expense: ${e.message}")
        }
    }

    private fun logExpenseError(errorMessage: String,groupId: String) {
        val params = Bundle().apply {
            putString("error_message", errorMessage)
            putString("group_id", groupId)
        }
        firebaseAnalytics.logEvent("group_expense_error", params)
    }
    private fun logExpenseAdded(expense: FriendExpenseRecord) {
        // Custom event for expense tracking
        val expenseParams = Bundle().apply {
            putDouble("amount", expense.totalAmount)
            putString("category", expense.expenseCategory)
            putString("currency", Utils.extractCurrencyCode(expense.expenseCategory))
            putString("split_type", expense.splitType)
            putBoolean("is_update", expense.id.trim().isNotEmpty())
        }
        firebaseAnalytics.logEvent("expense_added_OR_updated", expenseParams)
    }

    private fun logExpenseError(errorMessage: String) {
        val params = Bundle().apply {
            putString("error_message", errorMessage)
        }
        firebaseAnalytics.logEvent("expense_error", params)
    }
    suspend fun getExpensesByDate(
        myUserId: String,
        targetDate: String,
        onResult: (List<FriendExpenseRecord>, String?) -> Unit
    ) {
        try {
            val snapshot = firestore.collection("expenses")
                .document(myUserId)
                .collection("friendsExpenses")
                .whereEqualTo("startDate", targetDate)
                .orderBy("timeStamp", Query.Direction.DESCENDING)
                .get()
                .await()

            val expenses = snapshot.documents.mapNotNull { doc ->
                doc.toObject(FriendExpenseRecord::class.java)
            }

            Log.d("CHJAZ", expenses.toString())
            onResult(expenses, null)
        } catch (e: Exception) {
            Log.e("Repo", "Failed to fetch expenses: ${e.message}")
            onResult(emptyList(), e.message)
        }
    }

    suspend fun getMonthsTotalSpent(
        monthYearList: List<String>,
        userId: String,
        onResult: (List<MySpending>?, String?) -> Unit
    ) {
        try {
            val results = monthYearList.map { monthYear ->
                val parts = monthYear.split("-")
                val year = parts[0]
                val month = parts[1]

                try {
                    val data = firestore.collection("mySpending")
                        .document(userId)
                        .collection(year + month)
                        .get()
                        .await()

                    if (data.isEmpty) {
                        MySpending(month, month = month, year = month, 0.0)
                    } else {
                        val spending =
                            data.documents[0].toObject(MySpending::class.java) ?: MySpending(
                                month,
                                month = month,
                                year = month,
                                0.0
                            )
                        spending
                    }
                } catch (e: Exception) {
                    Log.e("Repo", "Failed to fetch expense for $monthYear: ${e.message}")
                    MySpending(month, month = month, year = month, 0.0)
                }
            }

            onResult(results, null)
        } catch (e: Exception) {
            Log.e("Repo", "Failed to fetch multiple months expense: ${e.message}")
            onResult(null, e.message)
        }
    }

    private suspend fun removeAndUpdateTotalExpense(
        oldStartDate: String,
        newAmount: Double,
        oldAmount: Double,
        newStartDate: String,
        userId: String
    ) {
        try {
            // Handle old date - remove amount
            val oldParts = oldStartDate.split("-")
            if (oldParts.size > 1) {
                val oldYear = oldParts[0]
                val oldMonth = oldParts[1]
                val oldCollectionRef = firestore.collection("mySpending")
                    .document(userId)
                    .collection(oldYear + oldMonth)

                val oldDataFetched = oldCollectionRef.get().await()
                if (!oldDataFetched.isEmpty) {
                    val oldSpending = oldDataFetched.documents[0].toObject(MySpending::class.java)
                    oldSpending?.let { spending ->
                        val updatedOldAmount =
                            (spending.totalAmountSpend - oldAmount).coerceAtLeast(0.0)
                        val updatedOldSpending = spending.copy(totalAmountSpend = updatedOldAmount)
                        oldCollectionRef.document(spending.id).set(updatedOldSpending).await()
                    }
                }
            }

            // Handle new date - add amount
            val newParts = newStartDate.split("-")
            if (newParts.size > 1) {
                val newYear = newParts[0]
                val newMonth = newParts[1]
                val newCollectionRef = firestore.collection("mySpending")
                    .document(userId)
                    .collection(newYear + newMonth)

                val newDataFetched = newCollectionRef.get().await()
                if (newDataFetched.isEmpty) {
                    // Create new spending record for the new month
                    val newSpending = MySpending(
                        id = newCollectionRef.document().id,
                        year = newYear,
                        month = newMonth,
                        totalAmountSpend = newAmount
                    )
                    newCollectionRef.document(newSpending.id).set(newSpending).await()
                } else {
                    // Update existing spending record for the new month
                    val existingSpending =
                        newDataFetched.documents[0].toObject(MySpending::class.java)
                    existingSpending?.let { spending ->
                        val updatedNewSpending = spending.copy(
                            totalAmountSpend = spending.totalAmountSpend + newAmount
                        )
                        newCollectionRef.document(spending.id).set(updatedNewSpending).await()
                    }
                }
            }
        } catch (e: Exception) {
            // Handle the error appropriately
            throw e
        }
    }


    private suspend fun updateTotalExpense(
        amount: Double,
        startDateString: String,
        userId: String
    ) {
        try {
            val parts = startDateString.split("-")
            if (parts.size > 1) {
                val year = parts[0]
                val month = parts[1]
                val collectionRef = firestore.collection("mySpending")
                    .document(userId)
                    .collection(year + month)

                // Try to fetch existing spending document for this month
                val dataFetched = collectionRef.get().await()

                if (dataFetched.isEmpty) {
                    // No existing spending record for this month - create new one
                    val newSpending = MySpending(
                        id = collectionRef.document().id,
                        year = year, month = month,
                        totalAmountSpend = amount
                    )
                    collectionRef.document(newSpending.id).set(newSpending).await()
                } else {
                    // Update existing spending record
                    val existingSpending = dataFetched.documents[0].toObject(MySpending::class.java)
                    existingSpending?.let { spending ->
                        val updatedSpending = spending.copy(
                            totalAmountSpend = spending.totalAmountSpend + amount
                        )
                        collectionRef.document(spending.id).set(updatedSpending).await()
                    }
                }
            }
        } catch (e: Exception) {
            // Handle the error appropriately
            throw e
        }
    }

    private suspend fun removeFromTotalExpense(
        amount: Double,
        startDateString: String,
        userId: String
    ) {
        try {
            val parts = startDateString.split("-")
            if (parts.size > 1) {
                val year = parts[0]
                val month = parts[1]
                val collectionRef = firestore.collection("mySpending")
                    .document(userId)
                    .collection(year + month)

                val dataFetched = collectionRef.get().await()

                if (!dataFetched.isEmpty) {
                    val existingSpending = dataFetched.documents[0].toObject(MySpending::class.java)
                    existingSpending?.let { spending ->
                        val updatedAmount = (spending.totalAmountSpend - amount).coerceAtLeast(0.0)
                        val updatedSpending = spending.copy(totalAmountSpend = updatedAmount)
                        collectionRef.document(spending.id).set(updatedSpending).await()
                    }
                }
            }
        } catch (e: Exception) {
            throw e
        }
    }


    suspend fun updateFriendExpense(
        oldStartDate: String,
        myUserId: String,
        expenseId: String,
        oldAmount: Double,
        updatedExpense: FriendExpenseRecord,
        onResult: (Boolean, String) -> Unit
    ) {
        try {
            val batch = firestore.batch()

            val myExpenseRef = firestore.collection("expenses")
                .document(myUserId)
                .collection("friendsExpenses")
                .document(expenseId)

            val friendExpenseRef = firestore.collection("expenses")
                .document(updatedExpense.friendId)
                .collection("friendsExpenses")
                .document(expenseId)

            batch.set(myExpenseRef, updatedExpense)
            batch.set(friendExpenseRef, updatedExpense)

            batch.commit().await()

            val newAmount = updatedExpense.splits.find { it.userId == myUserId }?.amount ?: 0.0

            removeAndUpdateTotalExpense(
                oldStartDate,
                newAmount,
                oldAmount,
                updatedExpense.startDate,
                myUserId
            )
            removeAndUpdateTotalExpense(
                oldStartDate,
                newAmount,
                oldAmount,
                updatedExpense.startDate,
                updatedExpense.friendId
            )

            onResult(true, "Expense updated successfully!")
        } catch (e: Exception) {
            Log.e("Repo", "Failed to update expense: ${e.message}")
            onResult(false, "Failed to update expense: ${e.message}")
        }
    }


    suspend fun deleteFriendExpense(
        myUserId: String,
        friendId: String,
        paidAmountByMe: Double,
        startDate: String,
        expenseId: String,
        onResult: (Boolean, String) -> Unit
    ) {
        try {
            val batch = firestore.batch()

            val myExpenseRef = firestore.collection("expenses")
                .document(myUserId)
                .collection("friendsExpenses")
                .document(expenseId)

            val friendExpenseRef = firestore.collection("expenses")
                .document(friendId)
                .collection("friendsExpenses")
                .document(expenseId)

            batch.delete(myExpenseRef)
            batch.delete(friendExpenseRef)

            batch.commit().await()

            removeFromTotalExpense(paidAmountByMe, startDate, myUserId)
            removeFromTotalExpense(paidAmountByMe, startDate, friendId)

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
        onResult: (success: Boolean, expenses: Map<String, List<FriendExpenseRecord>>?, message: String) -> Unit
    ) {
        try {
            val friendsSnapshot = firestore.collection("users")
                .document(myUserId)
                .collection("friends")
                .get()
                .await()
            if (!friendsSnapshot.isEmpty) {
                val allExpenses = mutableMapOf<String, List<FriendExpenseRecord>>()

                // For each friend
                for (friendDoc in friendsSnapshot.documents) {
                    val friendContact = friendDoc.id

                    // Get expenses for this friend
                    val friendExpensesSnapshot = firestore.collection("expenses")
                        .document(myUserId)
                        .collection("friendsExpenses").whereEqualTo("friendId", friendContact)
                        .get()
                        .await()
                    Log.d("CHKIU", "FRIEND SHOT :${friendExpensesSnapshot.isEmpty}")

                    val friendExpenses = friendExpensesSnapshot.documents.mapNotNull { document ->
                        try {
                            val expense = document.toObject(FriendExpenseRecord::class.java)
                            Log.d("CHKIU", "Mapped ExpenseRecord: $expense")
                            expense
                        } catch (e: Exception) {
                            Log.e(
                                "CHKIU",
                                "Failed to map document to ExpenseRecord: ${document.id}, error: ${e.message}"
                            )
                            null
                        }
                    }
                    Log.d("CHKIU", "FRIEND OBJECT: $friendExpenses")

                    Log.d("CHKIU", "FRIEND OBJECT :${friendExpenses}")

                    if (friendExpenses.isNotEmpty()) {
                        allExpenses[friendContact] = friendExpenses
                    }
                }
                Log.d("CHKIU", "$allExpenses")

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