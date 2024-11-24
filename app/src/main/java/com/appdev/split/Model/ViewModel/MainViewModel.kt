package com.appdev.split.Model.ViewModel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdev.split.Model.Data.Friend
import com.appdev.split.Model.Data.UiState
import com.appdev.split.Model.Data.UserEntity
import com.appdev.split.Repository.Repo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class MainViewModel @Inject constructor(var repo: Repo) : ViewModel() {
    private val _userData = MutableStateFlow<UserEntity?>(null)
    val userData: StateFlow<UserEntity?> = _userData

    private val _contactsState = MutableStateFlow<UiState<List<Friend>>>(UiState.Loading)
    val contactsState: StateFlow<UiState<List<Friend>>> = _contactsState

    private val _operationState = MutableStateFlow<UiState<Unit>>(UiState.Success(Unit))
    val operationState: StateFlow<UiState<Unit>> = _operationState


    private val _loadingState = MutableStateFlow(false)
    val loadingState: StateFlow<Boolean> = _loadingState

    init {
        fetchAllContacts()
    }


    //---------------------CONTACTS--------------------
    fun fetchAllContacts() {
        _contactsState.value = UiState.Loading
        viewModelScope.launch {
            try {
                repo.getAllContacts().collect { contacts ->
                    _contactsState.value = UiState.Success(contacts)
                }
            } catch (e: Exception) {
                _contactsState.value = UiState.Error(e.message ?: "Failed to fetch contacts")
            }
        }
    }
    fun addContacts(contacts: List<Friend>) {
        _operationState.value = UiState.Loading
        viewModelScope.launch {
            try {
                repo.insertContacts(contacts)
                _operationState.value = UiState.Success(Unit)
            } catch (e: Exception) {
                _operationState.value = UiState.Error(e.message ?: "Failed to add contacts")
            }
        }
    }

    fun updateContact(contact: Friend) {
        _operationState.value = UiState.Loading
        viewModelScope.launch {
            try {
                repo.updateContact(contact)
                _operationState.value = UiState.Success(Unit)
                fetchAllContacts()
            } catch (e: Exception) {
                _operationState.value = UiState.Error(e.message ?: "Failed to update contact")
            }
        }
    }

    fun deleteContact(contact: Friend) {
        _operationState.value = UiState.Loading
        viewModelScope.launch {
            try {
                repo.deleteContact(contact)
                _operationState.value = UiState.Success(Unit)
                fetchAllContacts()
            } catch (e: Exception) {
                _operationState.value = UiState.Error(e.message ?: "Failed to delete contact")
            }
        }
    }


    fun startSignUp(
        uri: Uri? = null,
        userEntity: UserEntity,
        result: (message: String, success: Boolean) -> Unit
    ) {
        repo.signUp(userEntity,uri) { message, success ->
            result(message, success)
        }
    }

    fun startLogin(userEntity: UserEntity, result: (message: String, success: Boolean) -> Unit) {
        repo.logIn(userEntity) { message, success ->
            result(message, success)
        }
    }

    fun fetchUserData(userId: String) {
        if (_userData.value != null) return // Prevent re-fetching if data is already available

        viewModelScope.launch {
            _loadingState.emit(true)
            repo.fetchUserData(userId) { user, _, success ->
                viewModelScope.launch {
                    if (success) {
                        _userData.emit(user) // Cache the fetched user data
                    } else {
                        _userData.emit(null) // Emit null if fetching fails
                    }
                    _loadingState.emit(false)
                }
            }
        }
    }



}