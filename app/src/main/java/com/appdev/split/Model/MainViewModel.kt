package com.appdev.split.Model

import androidx.lifecycle.ViewModel
import com.appdev.split.Model.Data.UserEntity
import com.appdev.split.Repository.Repo
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject


@HiltViewModel
class MainViewModel @Inject constructor(var repo: Repo) : ViewModel() {

    fun startSignUp(userEntity: UserEntity, result: (message: String, success: Boolean) -> Unit) {
        repo.signUp(userEntity) { message, success ->
            result(message, success)
        }
    }

    fun startLogin(userEntity: UserEntity, result: (message: String, success: Boolean) -> Unit) {
        repo.logIn(userEntity) { message, success ->
            result(message, success)
        }
    }
}