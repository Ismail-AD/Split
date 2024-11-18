package com.appdev.split.Repository

import com.appdev.split.Model.Data.UserEntity
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject

class Repo @Inject constructor() {
    var firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    fun signUp(userEntity: UserEntity, result: (message: String, success: Boolean) -> Unit) {
        firebaseAuth.createUserWithEmailAndPassword(userEntity.email, userEntity.password)
            .addOnCompleteListener {
                if (it.isSuccessful) {
//                val userProfile= userProfile((R.drawable.profileimageph).toString(),userName)
//                firebaseDatabase.reference.child("userProfiles")
//                    .child(firebaseAuth.uid!!)
//                    .setValue(userProfile)
//                    .addOnSuccessListener {
//
//                    }
                    result("Account created Successfully", true)
                } else {
                    result(it.exception!!.message.toString(), false)
                }
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
}