package com.appdev.split.Model.ViewModel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdev.split.Model.Data.ExpenseUserInput
import com.appdev.split.Model.Data.Friend
import com.appdev.split.Model.Data.UiState
import com.appdev.split.Model.Data.UserEntity
import com.appdev.split.Repository.Repo
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class MainViewModel @Inject constructor(var repo: Repo, val firebaseAuth: FirebaseAuth) :
    ViewModel() {
    private val _userData = MutableStateFlow<UserEntity?>(null)
    val userData: StateFlow<UserEntity?> = _userData

    private val _contactsState = MutableStateFlow<UiState<List<Friend>>>(UiState.Loading)
    val contactsState: StateFlow<UiState<List<Friend>>> = _contactsState

    private val _operationState = MutableStateFlow<UiState<Unit>>(UiState.Success(Unit))
    val operationState: StateFlow<UiState<Unit>> = _operationState

    private val _expenseToStore = MutableStateFlow<List<Friend>>(emptyList())
    val expenseToStore: StateFlow<List<Friend>> get() = _expenseToStore

    private val _expenseInput = MutableStateFlow(ExpenseUserInput())
    val expenseInput: MutableStateFlow<ExpenseUserInput> get() = _expenseInput


    var _newSelectedId = -1

    private val _loadingState = MutableStateFlow(false)
    val loadingState: StateFlow<Boolean> = _loadingState

    init {
        fetchAllContacts()
    }

    fun updateTitle(title: String) {
        _expenseInput.value = _expenseInput.value.copy(title = title)
    }

    fun updateDescription(description: String) {
        _expenseInput.value = _expenseInput.value.copy(description = description)
    }

    fun updateAmount(amount: Float) {
        _expenseInput.value = _expenseInput.value.copy(amount = amount)
    }

    fun updateFriendsList(newList: List<Friend>, selectedId: Int) {
        _expenseToStore.value = newList
        _newSelectedId = selectedId
        Log.d("CHKFRIE", selectedId.toString())
        Log.d("CHKFRIE", newList.size.toString())
       newList.forEach { fri->
           Log.d("CHKFRIE", fri.toString())
       }
    }


    //---------------------CONTACTS--------------------
    fun fetchAllContacts() {
        _contactsState.value = UiState.Loading
        viewModelScope.launch {
            try {
                firebaseAuth.currentUser?.email?.let { mail ->
                    repo.getAllContacts(mail).collect { contacts ->
                        _contactsState.value = UiState.Success(contacts)
                    }
                }
            } catch (e: Exception) {
                _contactsState.value = UiState.Error(e.message ?: "Failed to fetch contacts")
            }
        }
    }

    fun addContact(contact: Friend) {
        _operationState.value = UiState.Loading
        viewModelScope.launch {
            try {
                userData.value?.let { repo.insertContact(contact, it.email) }
                _operationState.value = UiState.Success(Unit)
            } catch (e: Exception) {
                _operationState.value = UiState.Error(e.message ?: "Failed to add contact")
            }
        }
    }

    fun addContacts(contacts: List<Friend>) {
        _operationState.value = UiState.Loading
        Log.d("CHKUSER", userData.value.toString())

        viewModelScope.launch {
            try {
                Log.d("CHKUSER", userData.value?.email ?: "no mail")
                userData.value?.let { repo.insertContacts(contacts, it.email) }
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
                userData.value?.let { repo.updateContact(contact, it.email) }
                _operationState.value = UiState.Success(Unit)
                fetchAllContacts()
            } catch (e: Exception) {
                _operationState.value = UiState.Error(e.message ?: "Failed to update contact")
            }
        }
    }

    fun updateContacts(contacts: List<Friend>) {
        _operationState.value = UiState.Loading
        viewModelScope.launch {
            try {
                userData.value?.let { repo.updateContacts(contacts, it.email) }
                _operationState.value = UiState.Success(Unit)
            } catch (e: Exception) {
                _operationState.value = UiState.Error(e.message ?: "Failed to update contact")
            }
        }
    }

    fun deleteContact(contact: Friend) {
        _operationState.value = UiState.Loading
        viewModelScope.launch {
            try {
                userData.value?.let { repo.deleteContact(contact, it.email) }
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
        repo.signUp(userEntity, uri) { message, success ->
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
                        Log.d("CHKUSER", userData.value?.email ?: "after fetch nah")
                    } else {
                        _userData.emit(null) // Emit null if fetching fails
                    }
                    _loadingState.emit(false)
                }
            }
        }
    }


}