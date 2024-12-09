package com.appdev.split.Model.ViewModel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdev.split.Model.Data.ExpenseRecord
import com.appdev.split.Model.Data.ExpenseUserInput
import com.appdev.split.Model.Data.Friend
import com.appdev.split.Model.Data.FriendContact
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

    private val _contactsState = MutableStateFlow<UiState<List<FriendContact>>>(UiState.Loading)
    val contactsState: StateFlow<UiState<List<FriendContact>>> = _contactsState

    private val _operationState = MutableStateFlow<UiState<Unit>>(UiState.Success(Unit))
    val operationState: StateFlow<UiState<Unit>> = _operationState

    private val _individualExpensesState =
        MutableStateFlow<UiState<List<ExpenseRecord>>>(UiState.Loading)
    val individualExpensesState: StateFlow<UiState<List<ExpenseRecord>>> get() = _individualExpensesState

    private val _allExpensesState =
        MutableStateFlow<UiState<Map<String, List<ExpenseRecord>>>>(UiState.Loading)
    val allExpensesState: StateFlow<UiState<Map<String, List<ExpenseRecord>>>> get() = _allExpensesState


    private val _individualFriendState = MutableStateFlow<UiState<Friend>>(UiState.Loading)
    val FriendState: StateFlow<UiState<Friend>> get() = _individualFriendState

    private val _expenseToPush = MutableStateFlow(ExpenseRecord())
    val expensePush: MutableStateFlow<ExpenseRecord> get() = _expenseToPush

    var _newSelectedId = -1
    private var cachedFriend: Friend? = null // Cache variable for storing friend data

    private val _loadingState = MutableStateFlow(false)
    val loadingState: StateFlow<Boolean> = _loadingState

    init {
        fetchAllContacts()
        getAllFriendExpenses()
    }

    //---------------------Friend Expense----------------------
    fun saveFriendExpense(
        expenseRecord: ExpenseRecord,
        friendsContact: String
    ) {
        _operationState.value = UiState.Loading
        viewModelScope.launch {
            try {
                firebaseAuth.currentUser?.email?.let { mail ->
                    repo.saveFriendExpense(
                        mail,
                        friendsContact,
                        expenseRecord
                    ) { success, message ->
                        if (success) {
                            _operationState.value = UiState.Success(Unit)

                        } else {
                            _operationState.value = UiState.Error(message)
                        }
                    }
                }
            } catch (e: Exception) {
                _operationState.value = UiState.Error(e.message ?: "Failed to save expense")
            }
        }
    }

    // For getting expenses of an individual friend
    fun getIndividualFriendExpenses(friendContact: String) {
        _individualExpensesState.value = UiState.Loading
        viewModelScope.launch {
            try {
                firebaseAuth.currentUser?.email?.let { mail ->
                    repo.getIndividualFriendExpenses(
                        myEmail = mail,
                        friendContact = friendContact
                    ) { success, expenses, message ->
                        if (success && expenses != null) {
                            _individualExpensesState.value = UiState.Success(expenses)
                        } else {
                            _individualExpensesState.value = UiState.Error(message)
                        }
                    }
                }
            } catch (e: Exception) {
                _individualExpensesState.value =
                    UiState.Error(e.message ?: "Failed to retrieve expenses")
            }
        }
    }

    // For getting all friends' expenses
    fun getAllFriendExpenses() {
        _allExpensesState.value = UiState.Loading
        viewModelScope.launch {
            try {
                firebaseAuth.currentUser?.email?.let { mail ->
                    repo.getAllFriendExpenses(
                        myEmail = mail
                    ) { success, allExpenses, message ->
                        if (success && allExpenses != null) {
                            _allExpensesState.value = UiState.Success(allExpenses)
                        } else {
                            _allExpensesState.value = UiState.Error(message)
                        }
                    }
                }
            } catch (e: Exception) {
                _allExpensesState.value =
                    UiState.Error(e.message ?: "Failed to retrieve all expenses")
            }
        }
    }

    fun getFriendNameById(contactId: String) {
        if (cachedFriend != null && cachedFriend?.contact == contactId) {
            // If the cached friend's contact ID matches, use the cached data
            _individualFriendState.value = UiState.Success(cachedFriend!!)
            return
        }
        _individualFriendState.value = UiState.Loading
        viewModelScope.launch {
            try {
                firebaseAuth.currentUser?.email?.let { mail ->
                    val friend = repo.getFriendByContactId(mail, contactId)
                    if (friend != null) {
                        cachedFriend = friend
                        _individualFriendState.value = UiState.Success(friend)
                    } else {
                        _individualFriendState.value = UiState.Error("Something went wrong")
                    }
                }
            } catch (e: Exception) {
                _individualFriendState.value =
                    UiState.Error(e.message ?: "Failed to retrieve friend info")
            }
        }
    }


    fun updateGeneralInfo(expenseRecord: ExpenseRecord) {
        _expenseToPush.value = expenseRecord.copy(
            title = expenseRecord.title,
            description = expenseRecord.description,
            amount = expenseRecord.amount,
            currency = expenseRecord.currency,
            expenseFor = expenseRecord.expenseFor
        )
    }

    fun updateFriendExpense(expenseRecord: ExpenseRecord, selectedId: Int) {

        _expenseToPush.value = expenseRecord.copy(
            paidAmount = expenseRecord.paidAmount,
            lentAmount = expenseRecord.lentAmount,
            borrowedAmount = expenseRecord.borrowedAmount,
            date = expenseRecord.date
        )
        _newSelectedId = selectedId
        Log.d("CHKFRIE", selectedId.toString())
        Log.d("CHKFRIE", _expenseToPush.value.toString())
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