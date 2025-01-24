package com.appdev.split.Model.ViewModel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdev.split.Model.Data.Contact
import com.appdev.split.Model.Data.ExpenseRecord
import com.appdev.split.Model.Data.Friend
import com.appdev.split.Model.Data.FriendContact
import com.appdev.split.Model.Data.GroupMetaData
import com.appdev.split.Model.Data.UiState
import com.appdev.split.Model.Data.UserEntity
import com.appdev.split.Repository.Repo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class MainViewModel @Inject constructor(
    var repo: Repo,
    val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) :
    ViewModel() {
    private val _userData = MutableStateFlow<UserEntity?>(null)
    val userData: StateFlow<UserEntity?> = _userData

    private val _contactsState = MutableStateFlow<UiState<List<FriendContact>>>(UiState.Loading)
    val contactsState: StateFlow<UiState<List<FriendContact>>> = _contactsState

    private val _operationState = MutableStateFlow<UiState<String>>(UiState.Stable)
    val operationState: StateFlow<UiState<String>> = _operationState

    private val _individualExpensesState =
        MutableStateFlow<UiState<List<ExpenseRecord>>>(UiState.Loading)
    val individualExpensesState: StateFlow<UiState<List<ExpenseRecord>>> get() = _individualExpensesState

    private val _allExpensesState =
        MutableStateFlow<UiState<Map<String, List<ExpenseRecord>>>>(UiState.Success(emptyMap()))
    val allExpensesState: StateFlow<UiState<Map<String, List<ExpenseRecord>>>> get() = _allExpensesState


    private val _groupsState = MutableStateFlow<UiState<List<GroupMetaData>>>(UiState.Loading)
    val GroupsState: StateFlow<UiState<List<GroupMetaData>>> get() = _groupsState


    private val _individualFriendState = MutableStateFlow<UiState<FriendContact>>(UiState.Loading)
    val FriendState: StateFlow<UiState<FriendContact>> get() = _individualFriendState

    private val _expenseToPush = MutableStateFlow(ExpenseRecord())
    val expensePush: MutableStateFlow<ExpenseRecord> get() = _expenseToPush

    private val _expenseReceived = MutableStateFlow(ExpenseRecord())
    val expenseReceived: MutableStateFlow<ExpenseRecord> get() = _expenseReceived
    var _newSelectedId = -1
    private var cachedFriend: FriendContact? = null // Cache variable for storing friend data

    private val _loadingState = MutableStateFlow(false)
    val loadingState: StateFlow<Boolean> = _loadingState

    private var expensesListener: ListenerRegistration? = null

    init {
        fetchAllContacts()
        getAllFriendExpenses()
    }

    fun updateStateToStable() {
        _operationState.value = UiState.Stable
    }

    fun updateExpRec(expenseRecord: ExpenseRecord) {
        _expenseToPush.value = expenseRecord
    }

    //---------------------EXPENSE LISTENER------------------

    fun setupRealTimeExpensesListener() {
        val currentUser = firebaseAuth.currentUser ?: return
        val userId = currentUser.uid

        // Cancel any existing listener
        expensesListener?.remove()
        expensesListener = firestore.collection("users")
            .document(userId)
            .collection("friends")
            .addSnapshotListener { friendsSnapshot, friendsError ->
                if (friendsError != null || friendsSnapshot == null) {
                    _allExpensesState.value = UiState.Error(friendsError?.message ?: "Unknown error")
                    return@addSnapshotListener
                }

                viewModelScope.launch {
                    val allExpenses = mutableMapOf<String, List<ExpenseRecord>>()

                    // For each friend, set up a listener for their expenses
                    for (friendDoc in friendsSnapshot.documents) {
                        val friendContact = friendDoc.id

                        // Add a listener to this friend's expenses subcollection
                        firestore.collection("expenses")
                            .document(userId)
                            .collection(friendContact)
                            .addSnapshotListener { expensesSnapshot, expensesError ->
                                if (expensesError != null) {
                                    Log.e("MainViewModel", "Error fetching expenses", expensesError)
                                    return@addSnapshotListener
                                }

                                val friendExpenses = expensesSnapshot?.documents?.mapNotNull { document ->
                                    document.toObject(ExpenseRecord::class.java)
                                } ?: emptyList()

                                if (friendExpenses.isNotEmpty()) {
                                    allExpenses[friendContact] = friendExpenses
                                    _allExpensesState.value = UiState.Success(allExpenses)
                                }
                            }
                    }
                }
            }
    }

    // Call this in onCleared to prevent memory leaks
    override fun onCleared() {
        super.onCleared()
        expensesListener?.remove()
    }

    //---------------------MANAGE MEMBERS--------------------
    fun addNewMembersToGroup(
        newMembers: MutableList<Contact>,
        groupId: String
    ) {
        _operationState.value = UiState.Loading
        val groupMatesList = convertContactsToGroupFriends(newMembers)
        viewModelScope.launch {
            try {
                firebaseAuth.currentUser?.uid?.let { myId ->
                    repo.addMembersToGroup(
                        newMembers = groupMatesList, groupId = groupId, onSuccess = { message ->
                            _operationState.value = UiState.Success(message)
                        }
                    ) { message ->
                        _operationState.value = UiState.Error(message)
                    }
                }
            } catch (e: Exception) {
                _operationState.value = UiState.Error(e.message ?: "Failed to save group")
            }
        }
    }

    //---------------------ADD GROUP-------------------------

    fun getAllGroups() {
        _groupsState.value = UiState.Loading
        viewModelScope.launch {
            try {
                firebaseAuth.currentUser?.email?.let { mail ->
                    repo.getAllGroups(
                        mail = mail, onSuccess = { list ->
                            _groupsState.value = UiState.Success(list)
                        }
                    ) { error ->
                        _groupsState.value = UiState.Error(error)
                    }
                }
            } catch (e: Exception) {
                _allExpensesState.value =
                    UiState.Error(e.message ?: "Failed to retrieve all groups")
            }
        }
    }

    fun saveNewGroup(
        imageUri: Uri?,
        imagebytes: ByteArray?,
        title: String,
        groupType: String,
    ) {
        _operationState.value = UiState.Loading
        viewModelScope.launch {
            try {
                firebaseAuth.currentUser?.email?.let { mail ->
                    repo.uploadImageAndSaveGroup(
                        mail,
                        imageUri,
                        imagebytes,
                        title, groupType, onSuccess = { message, grpId ->
                            _operationState.value = UiState.Success(message)
                        }
                    ) { message ->
                        _operationState.value = UiState.Error(message)
                    }
                }
            } catch (e: Exception) {
                _operationState.value = UiState.Error(e.message ?: "Failed to save group")
            }
        }
    }

    //---------------------GROUP Expense----------------------
    fun saveGroupExpense(
        expenseRecord: ExpenseRecord,
        groupId: String
    ) {
        _operationState.value = UiState.Loading
        viewModelScope.launch {
            try {
                firebaseAuth.currentUser?.email?.let { mail ->
                    repo.saveGroupExpense(
                        groupId,
                        expenseRecord
                    ) { success, message ->
                        if (success) {
                            _operationState.value = UiState.Success(message)

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

    fun updateGroupExpenseDetail(
        expenseRecord: ExpenseRecord,
        expenseId: String,
        groupId: String
    ) {
        _operationState.value = UiState.Loading
        viewModelScope.launch {
            try {
                firebaseAuth.currentUser?.email?.let { mail ->
                    repo.updateGroupExpense(
                        groupId,
                        expenseId,
                        expenseRecord
                    ) { success, message ->
                        if (success) {
                            _operationState.value = UiState.Success(message)

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


    //---------------------Friend Expense----------------------
    fun saveFriendExpense(
        expenseRecord: ExpenseRecord,
        friendsId: String
    ) {
        Log.d("CHKITMOM", "${expenseRecord.splits}")
        _operationState.value = UiState.Loading
        viewModelScope.launch {
            try {
                firebaseAuth.currentUser?.uid?.let { myId ->
                    repo.saveFriendExpense(
                        myId,
                        friendsId,
                        expenseRecord
                    ) { success, message ->
                        if (success) {
                            _operationState.value = UiState.Success(message)

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

    fun updateFriendExpenseDetail(
        expenseRecord: ExpenseRecord,
        expenseId: String,
        friendsContact: String
    ) {
        _operationState.value = UiState.Loading
        viewModelScope.launch {
            try {
                firebaseAuth.currentUser?.uid?.let { uid ->
                    repo.updateFriendExpense(
                       uid,
                        friendsContact,
                        expenseId,
                        expenseRecord
                    ) { success, message ->
                        if (success) {
                            _operationState.value = UiState.Success(message)

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


    fun deleteFriendExpenseDetail(
        expenseId: String,
        friendsId: String
    ) {
        _operationState.value = UiState.Loading
        viewModelScope.launch {
            try {
                firebaseAuth.currentUser?.uid?.let { uid ->
                    repo.deleteFriendExpense(
                        uid,
                        friendsId,
                        expenseId
                    ) { success, message ->
                        if (success) {
                            _operationState.value = UiState.Success(message)

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
                firebaseAuth.currentUser?.uid?.let { myId ->
                    repo.getAllFriendExpenses(
                        myUserId = myId
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
        Log.d("CHKIS", "id of friend: $contactId")
        if (cachedFriend != null && cachedFriend?.contact == contactId) {
            // If the cached friend's contact ID matches, use the cached data
            _individualFriendState.value = UiState.Success(cachedFriend!!)
            return
        }
        _individualFriendState.value = UiState.Loading
        viewModelScope.launch {
            try {
                firebaseAuth.currentUser?.uid?.let { uid ->
                    val friend = repo.getFriendByContactId(uid, contactId)
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
            totalAmount = expenseRecord.totalAmount,
            currency = expenseRecord.currency,
            expenseCategory = expenseRecord.expenseCategory
        )
    }

    fun updateFriendExpense(title: String, description: String, amount: String,currency:String,category:String) {

        _expenseToPush.value = _expenseToPush.value.copy(
            title = title,
            description = description,
            totalAmount = amount.toDouble(),
            currency = currency,
            expenseCategory = category
        )
    }

    fun prepareFinalExpense(expenseRecord: ExpenseRecord) {
        _expenseToPush.value = _expenseToPush.value.copy(
            totalAmount = expenseRecord.totalAmount,
            date = expenseRecord.date,
            currency = expenseRecord.currency,
            expenseCategory = expenseRecord.expenseCategory,
            paidBy = expenseRecord.paidBy,
            splitType = expenseRecord.splitType,
            // Preserve existing title and description
            title = expenseRecord.title,
            description = expenseRecord.description,
            splits = expenseRecord.splits
        )
    }

    fun updateFriendExpense(expenseRecord: ExpenseRecord) {
        _expenseToPush.value = _expenseToPush.value.copy(
            splitType = expenseRecord.splitType,
            totalAmount = expenseRecord.totalAmount,
            splits = expenseRecord.splits,
            date = expenseRecord.date,
            // Preserve existing title and description
            title = _expenseToPush.value.title.ifEmpty { expenseRecord.title },
            description = _expenseToPush.value.description.ifEmpty { expenseRecord.description }
        )
    }

    fun getExpenseObject(): ExpenseRecord {
        return _expenseToPush.value
    }

    fun setDefault() {
        _expenseToPush.value = ExpenseRecord()
    }

    //---------------------CONTACTS--------------------
    fun fetchAllContacts() {
        _contactsState.value = UiState.Loading
        viewModelScope.launch {
            try {
                firebaseAuth.currentUser?.uid?.let { uid ->
                    repo.getAllContacts(uid).collect { contacts ->
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
            firebaseAuth.currentUser?.uid?.let { myId ->
                repo.insertContact(contact, myId) { success, message ->
                    _operationState.value = if (success) {
                        UiState.Success(message)
                    } else {
                        UiState.Error(message)
                    }
                }
            }
        }
    }

    fun addContacts(selectedContacts: MutableList<Contact>) {
        _operationState.value = UiState.Loading
        val selectedContactsForOffline = convertContactsToFriends(selectedContacts)
        viewModelScope.launch {
            firebaseAuth.currentUser?.uid?.let { myId ->
                repo.insertContacts(
                    selectedContactsForOffline,
                    selectedContacts,
                    myId
                ) { success, message ->
                    _operationState.value = if (success) {
                        UiState.Success(message)
                    } else {
                        UiState.Error(message)
                    }
                }
            }
        }
    }
    private fun convertContactsToFriends(contacts: List<Contact>): List<Friend> {
        return contacts.map { contact ->
            Friend(
                name = contact.name,
                profileImageUrl = contact.imageUrl,
                contact = contact.number  // Using email instead of phone number
            )
        }
    }

    private fun convertContactsToGroupFriends(contacts: List<Contact>): List<FriendContact> {
        return contacts.map { contact ->
            FriendContact(
                name = contact.name,
                profileImageUrl = contact.imageUrl,
                contact = contact.number  // Using email instead of phone number,
                , friendId = contact.friendId
            )
        }
    }

    fun updateContact(contact: Friend) {
        _operationState.value = UiState.Loading
        viewModelScope.launch {
            firebaseAuth.currentUser?.uid?.let { myId ->
                repo.updateContact(contact, myId) { success, message ->
                    _operationState.value = if (success) {
                        fetchAllContacts()
                        UiState.Success(message)
                    } else {
                        UiState.Error(message)
                    }
                }
            }
        }
    }

    fun updateContacts(contacts: List<Friend>) {
        _operationState.value = UiState.Loading
        viewModelScope.launch {
            firebaseAuth.currentUser?.uid?.let { myId ->
                repo.updateContacts(contacts, myId) { success, message ->
                    _operationState.value = if (success) {
                        UiState.Success(message)
                    } else {
                        UiState.Error(message)
                    }
                }
            }
        }
    }

    fun deleteContact(contact: Friend) {
        _operationState.value = UiState.Loading
        viewModelScope.launch {
            userData.value?.let { user ->
                repo.deleteContact(contact, user.email) { success, message ->
                    _operationState.value = if (success) {
                        fetchAllContacts()
                        UiState.Success(message)
                    } else {
                        UiState.Error(message)
                    }
                }
            }
        }
    }


    fun startSignUp(
        uri: Uri? = null,
        userEntity: UserEntity,
        imageBytes: ByteArray?,
        result: (message: String, success: Boolean) -> Unit
    ) {
        viewModelScope.launch {
            repo.signUp(userEntity, uri, imageBytes) { message, success ->
                result(message, success)
            }
        }
    }

    fun startLogin(userEntity: UserEntity, result: (message: String, success: Boolean) -> Unit) {
        repo.logIn(userEntity) { message, success ->
            result(message, success)
        }
    }

    fun fetchUserData(userId: String) {
        Log.d("CHKUSER", "HERE TO FETCH ${userId}")

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