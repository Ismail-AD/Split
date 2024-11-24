package com.appdev.split.Repository

import android.net.Uri
import com.appdev.split.Model.Data.Contact
import com.appdev.split.Model.Data.Friend
import com.appdev.split.Model.Data.UserEntity
import com.appdev.split.Room.DaoClasses.ContactDao
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class Repo @Inject constructor(private val contactDao: ContactDao) {
    var firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    fun signUp(
        userEntity: UserEntity,
        uri: Uri?,
        result: (message: String, success: Boolean) -> Unit
    ) {
        firebaseAuth.createUserWithEmailAndPassword(userEntity.email, userEntity.password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = firebaseAuth.currentUser?.uid ?: return@addOnCompleteListener
                    val userProfile = hashMapOf(
                        "name" to userEntity.name,
                        "email" to userEntity.email
                    )

                    val dbReference = FirebaseDatabase.getInstance().reference
                    dbReference.child("users").child(userId).setValue(userProfile)
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
        val dbReference = FirebaseDatabase.getInstance().reference.child("users").child(userId)
        dbReference.get()
            .addOnSuccessListener { snapshot ->
                val name = snapshot.child("name").value.toString()
                val email = snapshot.child("email").value.toString()

                if (name != null && email != null) {
                    val user = UserEntity(name, email, "")
                    result(user, "User data fetched successfully", true)
                } else {
                    result(null, "User data not found", false)
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


    //--------------------------ADD CONTACT------------------------------

    fun getAllContacts(): Flow<List<Friend>> = contactDao.getAllContacts()

    suspend fun insertContact(contact: Friend) = contactDao.insertContact(contact)

    suspend fun updateContact(contact: Friend) = contactDao.updateContact(contact)

    suspend fun deleteContact(contact: Friend) = contactDao.deleteContact(contact)

    suspend fun insertContacts(contacts: List<Friend>) = contactDao.insertContacts(contacts)
    suspend fun getContactById(id: Int): Friend? = contactDao.getContactById(id)
    suspend fun getContactByName(name: String): Friend? = contactDao.getContactByName(name)
}