package com.appdev.split.UI.Fragment

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.appdev.split.Adapters.MemberAdapter
import com.appdev.split.Adapters.MyFriendSelectionAdapter
import com.appdev.split.Model.Data.ExpenseRecord
import com.appdev.split.Model.Data.Friend
import com.appdev.split.Model.Data.FriendContact
import com.appdev.split.Model.Data.NameId
import com.appdev.split.Model.Data.Split
import com.appdev.split.Model.Data.SplitType
import com.appdev.split.Model.Data.UiState
import com.appdev.split.Model.ViewModel.MainViewModel
import com.appdev.split.R
import com.appdev.split.UI.Activity.EntryActivity
import com.appdev.split.Utils.Utils
import com.appdev.split.databinding.DialogMemberListBinding
import com.appdev.split.databinding.FragmentAddGrpExpenseBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.skydoves.powerspinner.OnSpinnerItemSelectedListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar

@AndroidEntryPoint
class AddGrpExpenseFragment : Fragment() {
    private var _binding: FragmentAddGrpExpenseBinding? = null
    private val binding get() = _binding!!
    val mainViewModel by activityViewModels<MainViewModel>()

    private lateinit var adapter: MyFriendSelectionAdapter
    private var friendsList = mutableListOf<FriendContact>()
    private var allGroupMembersList = mutableListOf<FriendContact>()
    var selectedFriends = mutableSetOf<FriendContact>()
    val args: AddGrpExpenseFragmentArgs by navArgs()
    lateinit var dialog: Dialog
    var expObjectReceived: ExpenseRecord? = null
    var selectedDate = Utils.getCurrentDate()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentAddGrpExpenseBinding.inflate(layoutInflater, container, false)
        if (args.expenseRecord == null) {
            setupShimmer()
        }
        mainViewModel.updateStateToStable()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog = Dialog(requireContext())
        args.groupId?.let { mainViewModel.fetchAllGroupMembers(it) }
        if (args.expenseRecord != null && (mainViewModel.expensePush.value.splits.isEmpty())) {
            selectedDate = args.expenseRecord!!.date
            expObjectReceived = args.expenseRecord
            Log.d("CASZ", mainViewModel.expensePush.value.paidBy)
            mainViewModel.updateExpRec(args.expenseRecord!!)
            setupEditExpenseMode(args.expenseRecord!!)
            observeContactsForUpdate()
        } else if (args.expenseRecord == null && mainViewModel.expensePush.value.id.trim()
                .isEmpty()
        ) {
            setupNewExpenseMode()
            observeContacts()
        }

        if (args.expenseRecord != null) {
            hideForUpdate()
            setCalendarFromDate(mainViewModel.expensePush.value.date)
        } else {
            if (mainViewModel.expenseCategory.value.trim().isEmpty()) {
                mainViewModel.updateExpenseCategory(binding.categorySpinner.text.toString())
            }
            setCalendarFromDate(null)
        }
        handleSplitTypeChanges()
        observeExpenseInput()
        setupNavigationListeners()
        setupSaveData()


    }


    private fun setupSaveData() {
        binding.doneTextView.setOnClickListener {
            if (validateAndSave()) {
                observeOperationState()
                val title = binding.title.editText?.text.toString()
                val description = binding.description.editText?.text.toString()
                val amount = binding.amount.editText?.text.toString()

                val currentUser = FirebaseAuth.getInstance().currentUser
                val listOUserInSplit: MutableList<FriendContact> = mutableListOf()

                selectedFriends.forEach { friend ->
                    listOUserInSplit.add(friend)
                }

                if (mainViewModel.expensePush.value.splits.isEmpty() && args.expenseRecord == null) {
                    currentUser?.uid?.let {
                        listOUserInSplit.add(
                            FriendContact(
                                friendId = it,
                                name = mainViewModel.userData.value!!.name,
                                contact = mainViewModel.userData.value!!.email,
                                profileImageUrl = mainViewModel.userData.value!!.imageUrl
                            )
                        )
                    }
                }

                var nameIdList: List<NameId> =
                    if (args.groupId != null && args.expenseRecord != null) args.expenseRecord!!.splits.map { splitData ->
                        NameId(id = splitData.userId, name = splitData.username)
                    } else {
                        listOUserInSplit.map { friend ->
                            NameId(id = friend.friendId, name = friend.name)
                        }
                    }

                val computedSplits = when {
                    args.expenseRecord == null && mainViewModel.expensePush.value.splits.isEmpty() &&
                            binding.splitTypeText.text.toString() == SplitType.EQUAL.name -> {
                        handleExpenseSplitEqual(amount.toDouble(), nameIdList)
                    }

                    args.expenseRecord != null && SplitType.EQUAL.name == binding.splitTypeText.text.toString() &&
                            amount.toDouble() != mainViewModel.expensePush.value.totalAmount -> {
                        handleUpdateSplit(amount.toDouble(), mainViewModel.expensePush.value.splits)
                    }

                    else -> mainViewModel.expensePush.value.splits
                }

                if (validateAmount(computedSplits)) {
                    if (args.expenseRecord != null) {
                        Log.d("CASZ", "IN ON CLICK " + mainViewModel.expensePush.value.paidBy)
                        mainViewModel.updateGroupExpenseDetail(
                            ExpenseRecord(
                                date = mainViewModel.expensePush.value.date,
                                title = title,
                                description = description,
                                totalAmount = amount.toDouble(),
                                currency = binding.currencySpinner.text.toString(),
                                expenseCategory = binding.categorySpinner.text.toString(),
                                splits = computedSplits,
                                paidBy = mainViewModel.expensePush.value.paidBy,
                                id = mainViewModel.expensePush.value.id,
                                splitType = binding.splitTypeText.text.toString(),
                                timeStamp = System.currentTimeMillis()
                            ),
                            args.expenseRecord!!.id,
                            args.groupId!!
                        )
                    } else {
                        currentUser?.uid?.let {
                            Log.d("CASZ", "IN ON CLICK MY ID $it")
                            Log.d("CASZ", "IN ON CLICK EXPENSE CAT ${binding.categorySpinner.text}")
                            mainViewModel.prepareFinalExpense(
                                ExpenseRecord(
                                    date = mainViewModel.expensePush.value.date,
                                    title = title,
                                    description = description,
                                    totalAmount = amount.toDouble(),
                                    currency = binding.currencySpinner.text.toString(),
                                    expenseCategory = binding.categorySpinner.text.toString(),
                                    paidBy = it,
                                    splitType = binding.splitTypeText.text.toString(),
                                    splits = computedSplits
                                )
                            )
                            mainViewModel.saveGroupExpense(
                                mainViewModel.expensePush.value,
                                args.groupId!!
                            )
                        }
                    }
                }
            }
        }
    }

    private fun setupNavigationListeners() {
        binding.categorySpinner.setOnSpinnerItemSelectedListener(OnSpinnerItemSelectedListener<String> { _, _, _, newItem ->
            mainViewModel.updateExpenseCategory(newItem)
        })

        binding.Split.setOnClickListener {
            if (selectedFriends.isEmpty() && args.expenseRecord == null) {
                Toast.makeText(requireContext(), "Please select friends", Toast.LENGTH_SHORT).show()
            } else {
                if (!TextUtils.isEmpty(binding.amount.editText?.text.toString())) {
                    mainViewModel.updateExpenseBeforeNav(
                        title = binding.title.editText?.text.toString(),
                        description = binding.description.editText?.text.toString(),
                        amount = binding.amount.editText?.text.toString(),
                        currency = binding.currencySpinner.text.toString()
                    )
                    val action = binding.amount.editText?.let { it1 ->
                        AddGrpExpenseFragmentDirections.actionAddGrpExpenseFragmentToSplitAmountFragment(
                            selectedFriends.toList().toTypedArray(),
                            it1.text.toString().toFloat(),
                            null,
                            binding.splitTypeText.text.toString(),
                            Utils.extractCurrencyCode(binding.currencySpinner.text.toString())
                        )
                    }
                    if (action != null) {
                        findNavController().navigate(action)
                    }
                } else {
                    showError("Amount cannot be empty!")
                }
            }
        }

        binding.closeIcon.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.addFriends.setOnClickListener {
            args.groupId?.let {
                val action = AddGrpExpenseFragmentDirections
                    .actionAddGrpExpenseFragmentToAddMembersFragment(true, it)
                findNavController().navigate(action)
            }
        }

        binding.addAfterFriends.setOnClickListener {
            args.groupId?.let {
                val action = AddGrpExpenseFragmentDirections
                    .actionAddGrpExpenseFragmentToAddMembersFragment(true, it)
                findNavController().navigate(action)
            }
        }
    }

    private fun handleSplitTypeChanges() {
        if (args.expenseRecord != null && args.expenseRecord!!.splitType == mainViewModel.expensePush.value.splitType) {
            val expense = args.expenseRecord!!
            binding.splitTypeText.text = expense.splitType
        } else if (mainViewModel.expensePush.value.splitType.trim().isNotEmpty()) {
            binding.splitTypeText.text = mainViewModel.expensePush.value.splitType
        } else {
            binding.splitTypeText.text = SplitType.EQUAL.name
        }

    }

    private fun validateAmount(splitList: List<Split>): Boolean {
        val amount = binding.amount.editText?.text.toString().toDouble()
        val totalAmount = splitList.sumOf { it.amount }
        when {
            args.expenseRecord != null
                    && (args.expenseRecord!!.splitType == SplitType.UNEQUAL.name || args.expenseRecord!!.splitType == SplitType.PERCENTAGE.name)
                    && totalAmount != amount -> {
                showToast("Split among group doesn't add up to the total cost!")
                return false
            }

            else -> {
                return true
            }
        }
    }

    private fun handleExpenseSplitEqual(amount: Double, nameIdList: List<NameId>): List<Split> {
        val amountHalf = amount / 2

        val distributionList = Utils.createEqualSplits(nameIdList, amountHalf)
        return distributionList
    }


    private fun handleUpdateSplit(amount: Double, splits: List<Split>): List<Split> {
        val amountHalf = amount / 2
        val distributionList = Utils.updateEqualSplits(splits, amountHalf)
        return distributionList

    }


    private fun observeOperationState() {
        viewLifecycleOwner.lifecycleScope.launch {
            mainViewModel.operationState.collect { state ->
                when (state) {
                    is UiState.Error -> {
                        hideLoadingIndicator()
                        showError(state.message)
                    }

                    UiState.Loading -> showLoadingIndicator()
                    is UiState.Success -> {
                        hideLoadingIndicator()
                        findNavController().navigateUp()
                    }

                    UiState.Stable -> {}
                }
            }
        }
    }

    private fun observeExpenseInput() {
        viewLifecycleOwner.lifecycleScope.launch {
            mainViewModel.expensePush.collect { expenseInput ->
                binding.apply {
                    if (title.editText?.text.isNullOrEmpty()) {
                        title.editText?.setText(expenseInput.title)
                    }
                    if (description.editText?.text.isNullOrEmpty()) {
                        description.editText?.setText(expenseInput.description)
                    }
                    if (amount.editText?.text.isNullOrEmpty() && expenseInput.totalAmount != 0.0) {
                        amount.editText?.setText(expenseInput.totalAmount.toString())
                    }
                    if (expenseInput.currency.trim().isNotEmpty()) {
                        currencySpinner.selectItemByIndex(getCurrencyIndex(expenseInput.currency))
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            mainViewModel.expenseCategory.collect {
                if (it.trim().isNotEmpty()) {
                    Log.d("CKLA", "IN COLLECT $it")
                    binding.categorySpinner.selectItemByIndex(getCategoryIndex(it))
                }
            }
        }
    }


    private fun setupEditExpenseMode(expenseRecord: ExpenseRecord) {
        binding.apply {
            dateCheck.text = Utils.getCurrentDay(expenseRecord.date)
            currencySpinner.selectItemByIndex(getCurrencyIndex(expenseRecord.currency))
            categorySpinner.selectItemByIndex(getCategoryIndex(expenseRecord.expenseCategory))
            title.editText?.setText(expenseRecord.title)
            description.editText?.setText(expenseRecord.description)
            amount.editText?.setText(expenseRecord.totalAmount.toString())
            splitTypeText.text = expenseRecord.splitType
        }
    }

    private fun setupNewExpenseMode() {
        binding.apply {
            dateCheck.text = Utils.getCurrentDay(null)
            currencySpinner.selectItemByIndex(0)
            categorySpinner.selectItemByIndex(0)
            splitTypeText.text = SplitType.EQUAL.name
        }
        binding.titleTextView.text = "Add group expense"
    }

    private fun hideForUpdate() {
        // Hide contact list as it's not required when editing
        binding.selectedFrisRecyclerView.visibility = View.GONE
        binding.noFriends.visibility = View.GONE
        binding.shimmerViewContainer.visibility = View.GONE
        binding.titleFriends.visibility = View.GONE
        binding.titleTextView.text = "Update group expense"
    }

    // Helper functions for spinner indices
    private fun getCurrencyIndex(name: String): Int {
        val array = resources.getStringArray(R.array.currencies)
        val index = array.indexOf(name)
        return if (index >= 0) {
            index
        } else {
            0
        }
    }

    private fun getCategoryIndex(name: String): Int {
        val array = resources.getStringArray(R.array.categories)
        val index = array.indexOf(name)
        return if (index >= 0) {
            index
        } else {
            0
        }
    }

    private fun observeContacts() {
        viewLifecycleOwner.lifecycleScope.launch {
            mainViewModel.membersState.collect { contactsState ->
                when (contactsState) {
                    is UiState.Loading -> showShimmer()
                    is UiState.Success -> {
                        hideShimmer()
                        updateFriendsList(contactsState.data)
                        allGroupMembersList = contactsState.data.toMutableList()
                        val selectedIds = mainViewModel.selectedFriendIds.value
                        if (selectedIds.isNotEmpty()) {
                            adapter.setPreSelectedFriends(selectedIds)
                        }
                    }

                    is UiState.Error -> {
                        hideShimmer()
                        showError(contactsState.message)
                    }

                    else -> {}
                }
            }
        }
    }

    private fun observeContactsForUpdate() {
        viewLifecycleOwner.lifecycleScope.launch {
            mainViewModel.membersState.collect { contactsState ->
                when (contactsState) {
                    is UiState.Loading -> showLoadingIndicator()
                    is UiState.Success -> {
                        hideLoadingIndicator()
                        val allGroupMembers = contactsState.data

                        val selectedMemberIds =
                            args.expenseRecord?.splits?.map { it.userId }?.toSet() ?: emptySet()

                        selectedFriends =
                            allGroupMembers.filter { it.friendId in selectedMemberIds }
                                .toMutableSet()
                    }

                    is UiState.Error -> {
                        hideLoadingIndicator()
                        showError(contactsState.message)
                    }

                    else -> {}
                }
            }
        }
    }


    private fun updateFriendsList(friends: List<FriendContact>) {
        friendsList = friends.toMutableList()
        if (friendsList.isNotEmpty()) {
            binding.addAfterFriends.visibility = View.VISIBLE
            binding.selectedFrisRecyclerView.visibility = View.VISIBLE
            binding.noFriends.visibility = View.GONE
            adapter = MyFriendSelectionAdapter(friendsList, true) { selectedFriends ->
                this.selectedFriends.clear()
                this.selectedFriends.addAll(selectedFriends)

                // Update ViewModel with selected friends
                mainViewModel.updateSelectedFriends(selectedFriends)
            }
            binding.selectedFrisRecyclerView.adapter = adapter
            binding.selectedFrisRecyclerView.layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter.notifyDataSetChanged()
        } else {
            binding.selectedFrisRecyclerView.visibility = View.GONE
            binding.noFriends.visibility = View.VISIBLE
            binding.addAfterFriends.visibility = View.GONE
        }
    }

    private fun setCalendarFromDate(dateString: String?) {
        Log.d("CJL", "REC: $dateString")
        val calendar = Calendar.getInstance()
        var year: Int
        var month: Int
        var day: Int
        val finalString = dateString ?: mainViewModel.expensePush.value.date
        if (finalString.trim().isNotEmpty()) {
            val parts = finalString.split("-")
            if (parts.size == 3) {
                year = parts[0].toInt()
                month = parts[1].toInt() - 1 // Calendar months are 0-based
                day = parts[2].toInt()

                calendar.set(year, month, day)
            }
        }
        year = calendar.get(Calendar.YEAR)
        month = calendar.get(Calendar.MONTH)
        day = calendar.get(Calendar.DAY_OF_MONTH)
        selectedDate = "$year-${month + 1}-$day"
        Log.d("CJL", "CONSTRUCTED: $year-${month + 1}-$day")

        // Open DatePicker with the specified date
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
            },
            year, month, day
        )
        binding.dateCheck.text = day.toString()
        mainViewModel.updateExpenseDate(selectedDate)
        binding.calender.setOnClickListener {
            datePickerDialog.show()
            datePickerDialog.setOnDateSetListener { view, yearn, monthn, dayOfMonthn ->
                selectedDate = "$yearn-${monthn + 1}-$dayOfMonthn"
                binding.dateCheck.text = dayOfMonthn.toString()
                mainViewModel.updateExpenseDate(selectedDate)
            }
        }
    }

    private fun showLoadingIndicator() {
        Log.d("CHKERR", "Loading from input")

        dialog.setContentView(R.layout.progress_dialog)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCancelable(false)
        dialog.show()
    }

    private fun hideLoadingIndicator() {
        Log.d("CHKERR", "hiding from input")
        dialog.dismiss()
    }

    private fun setupShimmer() {
        // Set the layout for the ViewStub
        binding.shimmerViewFriendList.layoutResource = R.layout.friendslist_shimmer
        binding.shimmerViewFriendList.inflate()
    }

    private fun showShimmer() {
        binding.shimmerViewContainer.visibility = View.VISIBLE
        binding.shimmerViewContainer.startShimmer()

        // Hide actual content while shimmer is showing
        binding.selectedFrisRecyclerView.visibility = View.GONE
    }

    private fun hideShimmer() {
        binding.shimmerViewContainer.stopShimmer()
        binding.shimmerViewContainer.visibility = View.GONE

        binding.selectedFrisRecyclerView.visibility = View.VISIBLE

    }

    private fun showError(message: String) {
        Log.d("CHKERR", message)
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }


    private fun validateAndSave(): Boolean {
        val title = binding.title.editText?.text.toString()
        val description = binding.description.editText?.text.toString()
        val amount = binding.amount.editText?.text.toString()

        when {
            TextUtils.isEmpty(title) -> {
                showToast("Title cannot be empty")
                return false
            }

            TextUtils.isEmpty(description) -> {
                showToast("Description cannot be empty")
                return false
            }

            TextUtils.isEmpty(amount) -> {
                showToast("Amount cannot be empty")
                return false
            }

            selectedFriends.isEmpty() && args.expenseRecord == null -> {
                showToast("Please select at least one member.")
                return false
            }


            else -> {
                return true
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}