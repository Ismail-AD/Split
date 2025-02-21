package com.appdev.split.Model.ViewModel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdev.split.Model.Data.Contact
import com.appdev.split.Model.Data.ExpenseRecord
import com.appdev.split.Model.Data.Friend
import com.appdev.split.Model.Data.FriendContact
import com.appdev.split.Model.Data.FriendExpenseRecord
import com.appdev.split.Model.Data.GroupMetaData
import com.appdev.split.Model.Data.MySpending
import com.appdev.split.Model.Data.UiState
import com.appdev.split.Model.Data.UserEntity
import com.appdev.split.Repository.Repo
import com.appdev.split.Utils.Utils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume


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

    private val _membersState = MutableStateFlow<UiState<List<FriendContact>>>(UiState.Loading)
    val membersState: StateFlow<UiState<List<FriendContact>>> = _membersState

    private val _operationState = MutableStateFlow<UiState<String>>(UiState.Stable)
    val operationState: StateFlow<UiState<String>> = _operationState

    private val _individualExpensesState =
        MutableStateFlow<UiState<List<ExpenseRecord>>>(UiState.Loading)
    val individualExpensesState: StateFlow<UiState<List<ExpenseRecord>>> get() = _individualExpensesState

    private val _allExpensesState =
        MutableStateFlow<UiState<Map<String, List<FriendExpenseRecord>>>>(UiState.Success(emptyMap()))
    val allExpensesState: StateFlow<UiState<Map<String, List<FriendExpenseRecord>>>> get() = _allExpensesState

    private val _monthBaseExpensesState =
        MutableStateFlow<UiState<List<FriendExpenseRecord>>>(UiState.Loading)
    val monthBaseExpensesState: StateFlow<UiState<List<FriendExpenseRecord>>> get() = _monthBaseExpensesState

    private val _monthsTotalSpentState =
        MutableStateFlow<UiState<List<MySpending>>>(UiState.Loading)
    val monthsTotalSpentState: StateFlow<UiState<List<MySpending>>> get() = _monthsTotalSpentState

    private val _GroupExpensesState =
        MutableStateFlow<UiState<List<ExpenseRecord>>>(UiState.Loading)
    val groupExpensesState: StateFlow<UiState<List<ExpenseRecord>>> get() = _GroupExpensesState

    private val _groupsState = MutableStateFlow<UiState<List<GroupMetaData>>>(UiState.Loading)
    val GroupsState: StateFlow<UiState<List<GroupMetaData>>> get() = _groupsState


    private val _individualFriendState = MutableStateFlow<UiState<FriendContact>>(UiState.Loading)
    val FriendState: StateFlow<UiState<FriendContact>> get() = _individualFriendState

    private val _friendExpenseToPush = MutableStateFlow(FriendExpenseRecord())
    val friendExpensePush: MutableStateFlow<FriendExpenseRecord> get() = _friendExpenseToPush

    private val _expenseToPush = MutableStateFlow(ExpenseRecord())
    val expensePush: MutableStateFlow<ExpenseRecord> get() = _expenseToPush

    private val _expenseCategory = MutableStateFlow("")
    val expenseCategory: MutableStateFlow<String> get() = _expenseCategory
    private val _selectedFriendIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedFriendIds: StateFlow<Set<String>> = _selectedFriendIds


    var selectedMonthYear = MutableLiveData<String>()

    private var cachedFriend: FriendContact? = null // Cache variable for storing friend data


    var monthsWithYears: List<String> = emptyList()

    private val _selectedMonthYears = MutableStateFlow(Utils.getYearMonth())
    val selectedMonthYears = _selectedMonthYears.asStateFlow()

    fun updateSelectedMonth(monthYear: String) {
        viewModelScope.launch {
            _selectedMonthYears.emit(monthYear)
        }
    }

    private val _loadingState = MutableStateFlow(false)
    val loadingState: StateFlow<Boolean> = _loadingState

    private var expensesListener: ListenerRegistration? = null
    private val friendExpenseListeners = mutableMapOf<String, ListenerRegistration>()
    private val monthSpendingListeners = mutableMapOf<String, ListenerRegistration>()

    init {
        fetchAllContacts()
    }

    fun updateStateToStable() {
        _operationState.value = UiState.Stable
    }

    fun updateFriendStateToStable() {
        _individualFriendState.value = UiState.Stable
    }


    fun updateExpRec(expenseRecord: ExpenseRecord) {
        _expenseToPush.value = expenseRecord
    }

    fun updateFriendExpRec(expenseRecord: FriendExpenseRecord) {
        _friendExpenseToPush.value = expenseRecord
    }

    fun updateSelectedFriends(friends: List<FriendContact>) {
        _selectedFriendIds.value = friends.map { it.friendId }.toSet()
    }

    fun clearSelectedFriends() {
        _selectedFriendIds.value = emptySet()
    }

    //--------------------LISTNERS-------------------

    fun setupRealTimeExpensesListener(targetDate: String) {
        val currentUser = firebaseAuth.currentUser ?: return
        val userId = currentUser.uid
        _monthBaseExpensesState.value = UiState.Loading

        cleanupListeners()

        expensesListener = firestore.collection("expenses")
            .document(userId)
            .collection("friendsExpenses")
            .whereEqualTo("startDate", targetDate)
            .orderBy("timeStamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _monthBaseExpensesState.value = UiState.Error(error.message ?: "Unknown error")
                    return@addSnapshotListener
                }

                val expenses = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(FriendExpenseRecord::class.java)
                } ?: emptyList()

                _monthBaseExpensesState.value = UiState.Success(expenses)
            }
    }


    fun setupRealTimeMonthlySpendingListener(monthYearList: List<String>) {
        val currentUser = firebaseAuth.currentUser ?: return
        val userId = currentUser.uid
        _monthsTotalSpentState.value = UiState.Loading

        cleanupMonthlySpendingListeners()

        viewModelScope.launch {
            val results = mutableListOf<MySpending>()
            val deferredResults = monthYearList.map { monthYear ->
                async {
                    val parts = monthYear.split("-")
                    val year = parts[0]
                    val month = parts[1]

                    suspendCancellableCoroutine { continuation ->
                        val listener = firestore.collection("mySpending")
                            .document(userId)
                            .collection(year + month)
                            .addSnapshotListener { snapshot, error ->
                                if (error != null) {
                                    continuation.resume(
                                        MySpending(
                                            month = month,
                                            year = year,
                                            totalAmountSpend = 0.0
                                        )
                                    )
                                    return@addSnapshotListener
                                }

                                val spending = snapshot?.documents?.firstOrNull()
                                    ?.toObject(MySpending::class.java)
                                    ?: MySpending(
                                        month = month,
                                        year = year,
                                        totalAmountSpend = 0.0
                                    )

                                if (!continuation.isCompleted) {
                                    continuation.resume(spending)
                                }
                            }

                        monthSpendingListeners[monthYear] = listener

                        continuation.invokeOnCancellation {
                            listener.remove()
                            monthSpendingListeners.remove(monthYear)
                        }
                    }
                }
            }

            // Wait for all results before updating state
            val spendingData = deferredResults.awaitAll()
            results.addAll(spendingData)

            _monthsTotalSpentState.value = UiState.Success(results)
        }
    }


    //---------------------EXPENSE LISTENER------------------

    fun setupRealTimeExpensesListener() {
        val currentUser = firebaseAuth.currentUser ?: return
        val userId = currentUser.uid

        // Cancel any existing listeners
        cleanupListeners()

        expensesListener = firestore.collection("users")
            .document(userId)
            .collection("friends")
            .addSnapshotListener { friendsSnapshot, friendsError ->
                if (friendsError != null || friendsSnapshot == null) {
                    _allExpensesState.value =
                        UiState.Error(friendsError?.message ?: "Unknown error")
                    return@addSnapshotListener
                }

                // Clear existing friend expense listeners
                cleanupFriendListeners()

                // Create a new scope for managing concurrent listeners
                viewModelScope.launch {
                    val allExpenses = mutableMapOf<String, List<FriendExpenseRecord>>()
                    val deferredResults = friendsSnapshot.documents.map { friendDoc ->
                        async {
                            val friendContact = friendDoc.id
                            val expenses =
                                suspendCancellableCoroutine<List<FriendExpenseRecord>> { continuation ->
                                    val listener = firestore.collection("expenses")
                                        .document(userId)
                                        .collection("friendsExpenses")
                                        .whereEqualTo("friendId", friendContact)
                                        .addSnapshotListener { expensesSnapshot, expensesError ->
                                            if (expensesError != null) {
                                                Log.e(
                                                    "MainViewModel",
                                                    "Error fetching expenses",
                                                    expensesError
                                                )
                                                continuation.resume(emptyList())
                                                return@addSnapshotListener
                                            }

                                            val friendExpenses =
                                                expensesSnapshot?.documents?.mapNotNull { document ->
                                                    document.toObject(FriendExpenseRecord::class.java)
                                                } ?: emptyList()

                                            if (!continuation.isCompleted) {
                                                continuation.resume(friendExpenses)
                                            }
                                        }

                                    // Store the listener for cleanup
                                    friendExpenseListeners[friendContact] = listener

                                    continuation.invokeOnCancellation {
                                        listener.remove()
                                        friendExpenseListeners.remove(friendContact)
                                    }
                                }
                            friendDoc.id to expenses
                        }
                    }

                    // Wait for all expenses to be collected
                    deferredResults.awaitAll().forEach { (friendId, expenses) ->
                        if (expenses.isNotEmpty()) {
                            allExpenses[friendId] = expenses
                        }
                    }

                    // Update the state with all collected expenses
                    _allExpensesState.value = UiState.Success(allExpenses)
                }
            }
    }

    // Call this in onCleared to prevent memory leaks
    override fun onCleared() {
        super.onCleared()
        cleanupListeners()
    }


    fun cleanupListeners() {
        expensesListener?.remove()
        expensesListener = null
        cleanupFriendListeners()
    }

    private fun cleanupFriendListeners() {
        friendExpenseListeners.values.forEach { it.remove() }
        friendExpenseListeners.clear()
    }

    fun cleanupMonthlySpendingListeners() {
        monthSpendingListeners.values.forEach { it.remove() }
        monthSpendingListeners.clear()
    }

    //---------------------MANAGE MEMBERS--------------------
    fun fetchAllGroupMembers(groupId: String) {
        _membersState.value = UiState.Loading
        viewModelScope.launch {
            try {
                firebaseAuth.currentUser?.uid?.let { uid ->
                    repo.getGroupMembers(groupId = groupId, onSuccess = { list ->
                        _membersState.value = UiState.Success(list)
                    }) { msg ->
                        _membersState.value = UiState.Error(msg)
                    }
                }
            } catch (e: Exception) {
                _membersState.value = UiState.Error(e.message ?: "Failed to fetch contacts")
            }
        }
    }

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
                firebaseAuth.currentUser?.uid?.let { uid ->
                    repo.getAllGroups(
                        userid = uid, onSuccess = { list ->
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
                firebaseAuth.currentUser?.uid?.let { uid ->
                    repo.uploadImageAndSaveGroup(
                        uid,
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
                firebaseAuth.currentUser?.uid?.let { uid ->
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

    fun deleteGroupExpense(expenseId: String, groupId: String) {
        _operationState.value = UiState.Loading
        viewModelScope.launch {
            try {
                firebaseAuth.currentUser?.uid?.let { uid ->
                    repo.deleteGroupExpense(
                        groupId,
                        expenseId
                    ) { success, message ->
                        _operationState.value = if (success) {
                            UiState.Success(message)
                        } else {
                            UiState.Error(message)
                        }
                    }
                }
            } catch (e: Exception) {
                _operationState.value = UiState.Error(e.message ?: "Failed to delete expense")
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
                firebaseAuth.currentUser?.uid?.let { uid ->
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


    // For getting all friends' expenses
    fun getAllGroupExpenses(groupId: String) {
        _GroupExpensesState.value = UiState.Loading
        viewModelScope.launch {
            try {
                firebaseAuth.currentUser?.uid?.let { myId ->
                    repo.getAllGroupExpenses(
                        groupId = groupId
                    ) { success, allExpenses, message ->
                        if (success && allExpenses != null) {
                            _GroupExpensesState.value = UiState.Success(allExpenses)
                        } else {
                            _GroupExpensesState.value = UiState.Error(message)
                        }
                    }
                }
            } catch (e: Exception) {
                _GroupExpensesState.value =
                    UiState.Error(e.message ?: "Failed to retrieve all expenses")
            }
        }
    }


    //---------------------Friend Expense----------------------
    fun saveFriendExpense(
        expenseRecord: FriendExpenseRecord
    ) {
        Log.d("CHKITMOM", "${expenseRecord.splits}")
        _operationState.value = UiState.Loading
        viewModelScope.launch {
            try {
                firebaseAuth.currentUser?.uid?.let { myId ->
                    repo.saveFriendExpense(
                        myId,
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

    fun getCurrentUserId() = repo.getCurrentUserId()

    fun updateFriendExpenseDetail(
        expenseRecord: FriendExpenseRecord,
        expenseId: String,
        amount: Double,
        oldStartDate: String
    ) {
        _operationState.value = UiState.Loading
        viewModelScope.launch {
            try {
                firebaseAuth.currentUser?.uid?.let { uid ->
                    repo.updateFriendExpense(
                        oldStartDate = oldStartDate,
                        uid,
                        expenseId, oldAmount = amount,
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
        paidAmountByMe: Double,
        startDate: String
    ) {
        _operationState.value = UiState.Loading
        viewModelScope.launch {
            try {
                firebaseAuth.currentUser?.uid?.let { uid ->
                    repo.deleteFriendExpense(
                        uid, paidAmountByMe, startDate,
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


    fun getMonthlyExpense(
        targetDate: String
    ) {
        Log.d("CHKAZa", "I AM CALLED with ${targetDate}")

        _monthBaseExpensesState.value = UiState.Loading
        viewModelScope.launch {
            try {
                firebaseAuth.currentUser?.uid?.let { myId ->
                    repo.getExpensesByDate(
                        myId,
                        targetDate
                    ) { listOfExpenses, message ->

                        _monthBaseExpensesState.value = UiState.Success(listOfExpenses)

                    }
                }
            } catch (e: Exception) {
                _monthBaseExpensesState.value = UiState.Error(e.message ?: "Failed to save expense")
            }
        }
    }

    fun getMonthsTotalSpent(
        monthYearList: List<String>
    ) {
        monthsWithYears = monthYearList
        _monthsTotalSpentState.value = UiState.Loading
        viewModelScope.launch {
            try {
                firebaseAuth.currentUser?.uid?.let { myId ->
                    repo.getMonthsTotalSpent(
                        monthYearList,
                        myId
                    ) { listOfExpenses, message ->
                        if (listOfExpenses != null) {
                            if (listOfExpenses.isNotEmpty()) {
                                _monthsTotalSpentState.value = UiState.Success(listOfExpenses)
                            } else {
                                _monthsTotalSpentState.value = UiState.Success(emptyList())
                            }
                        } else if (message != null) {
                            _monthsTotalSpentState.value = UiState.Error(message)
                        }
                    }
                }
            } catch (e: Exception) {
                _monthsTotalSpentState.value = UiState.Error(e.message ?: "Failed to save expense")
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

    fun updateExpenseBeforeNav(
        title: String,
        description: String,
        amount: String,
        currency: String
    ) {
        _expenseToPush.value = _expenseToPush.value.copy(
            title = title,
            description = description,
            totalAmount = amount.toDouble(),
            currency = currency
        )
    }

    fun updateFriendExpenseBeforeNav(
        title: String,
        description: String,
        amount: String,
        currency: String
    ) {
        _friendExpenseToPush.value = _friendExpenseToPush.value.copy(
            title = title,
            description = description,
            totalAmount = amount.toDouble(),
            currency = currency
        )
    }


//    fun updateExpenseCategory(
//        category: String
//    ) {
//        _expenseToPush.value = _expenseToPush.value.copy(
//            expenseCategory = category
//        )
//    }

    fun updateExpenseCategory(
        category: String
    ) {
        _expenseCategory.value = category
        Log.d(
            "CKLA",
            "---AFTER BUTTON CLICKED: ${_expenseCategory.value}"
        )
    }


    fun updateFriendExpenseDate(
        startDate: String,
        endDate: String
    ) {
        _friendExpenseToPush.value =
            _friendExpenseToPush.value.copy(startDate = startDate, endDate = endDate)
    }

    fun updateExpenseDate(
        startDate: String,
        endDate: String
    ) {
        _expenseToPush.value = _expenseToPush.value.copy(startDate = startDate, endDate = endDate)
    }


//    fun prepareFinalExpense(expenseRecord: ExpenseRecord) {
//        Log.d("DEBUG_EXPENSE_", "IN VM: " + expenseRecord.paidBy)
//        _expenseToPush.value = _expenseToPush.value.copy(
//            totalAmount = expenseRecord.totalAmount,
//            currency = expenseRecord.currency,
//            expenseCategory = expenseRecord.expenseCategory,
//            paidBy = expenseRecord.paidBy,
//            splitType = expenseRecord.splitType,
//            // Preserve existing title and description
//            title = expenseRecord.title,
//            description = expenseRecord.description,
//            splits = expenseRecord.splits
//        )
//        Log.d("DEBUG_EXPENSE_", expensePush.value.toString())
//        Log.d("DEBUG_EXPENSE_", _expenseToPush.value.toString())
//    }

    fun updateFriendExpense(expenseRecord: FriendExpenseRecord) {
        _friendExpenseToPush.value = _friendExpenseToPush.value.copy(
            splitType = expenseRecord.splitType,
            totalAmount = expenseRecord.totalAmount,
            splits = expenseRecord.splits,
            // Preserve existing title and description
            title = _friendExpenseToPush.value.title.ifEmpty { expenseRecord.title },
            description = _friendExpenseToPush.value.description.ifEmpty { expenseRecord.description }
        )
    }

    fun updateGroupExpense(expenseRecord: ExpenseRecord) {
        _expenseToPush.value = _expenseToPush.value.copy(
            splitType = expenseRecord.splitType,
            totalAmount = expenseRecord.totalAmount,
            splits = expenseRecord.splits,
            // Preserve existing title and description
            title = _expenseToPush.value.title.ifEmpty { expenseRecord.title },
            description = _expenseToPush.value.description.ifEmpty { expenseRecord.description }
        )
    }

    fun updateGroupExpenseToEmpty(expenseRecord: ExpenseRecord) {
        _expenseToPush.value = expenseRecord
    }

    fun updateGFriendExpenseToEmpty(expenseRecord: FriendExpenseRecord) {
        _friendExpenseToPush.value = expenseRecord
    }


    fun getExpenseObject(): ExpenseRecord {
        return _expenseToPush.value
    }

    fun getFriendExpenseObject(): FriendExpenseRecord {
        return _friendExpenseToPush.value
    }

    fun setDefault() {
        _expenseToPush.value = ExpenseRecord()
    }

    fun setFriendDefault() {
        _friendExpenseToPush.value = FriendExpenseRecord()
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


    fun updateUserDataLocally(updatedUser: UserEntity) {
        _userData.value = updatedUser
    }

    fun updateUserName(
        newName: String,
        result: (message: String, success: Boolean) -> Unit
    ) {
        viewModelScope.launch {
            firebaseAuth.currentUser?.uid?.let { uid ->
                repo.updateUserName(userId = uid, newName = newName) { message, success ->
                    result(message, success)
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