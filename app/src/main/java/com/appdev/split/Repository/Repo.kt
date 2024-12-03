package com.appdev.split.Repository

import android.net.Uri
import android.util.Log
import com.appdev.split.Model.Data.ExpenseRecord
import com.appdev.split.Model.Data.Friend
import com.appdev.split.Model.Data.FriendContact
import com.appdev.split.Model.Data.UserEntity
import com.appdev.split.Room.DaoClasses.ContactDao
import com.appdev.split.Utils.Utils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class Repo @Inject constructor(
    private val contactDao: ContactDao,
    private val firebaseDatabase: FirebaseDatabase,
    val firebaseAuth: FirebaseAuth
) {

    val currentUser = firebaseAuth.currentUser
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

            // Reference to Firebase node
            val expenseRef = firebaseDatabase.reference
                .child("expenses")
                .child(sanitizedMyEmail)
                .child(sanitizedFriendContact)

            // Push new expense and get the unique key
            val newExpenseRef = expenseRef.push()

            // Set the expense object at the generated key
            newExpenseRef.setValue(expense).await()

            // Notify success
            onResult(true, "Expense saved successfully!")
        } catch (e: Exception) {
            // Handle and log exceptions
            Log.e("Repo", "Failed to save expense: ${e.message}")
            onResult(false, "Failed to save expense: ${e.message}")
        }
    }


}