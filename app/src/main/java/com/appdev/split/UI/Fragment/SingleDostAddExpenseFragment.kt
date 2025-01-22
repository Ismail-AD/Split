package com.appdev.split.UI.Fragment

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.appdev.split.Adapters.MyFriendSelectionAdapter
import com.appdev.split.Model.Data.ExpenseRecord
import com.appdev.split.Model.Data.FriendContact
import com.appdev.split.Model.Data.NameId
import com.appdev.split.Model.Data.Split
import com.appdev.split.Model.Data.SplitType
import com.appdev.split.Model.Data.UiState
import com.appdev.split.Model.ViewModel.MainViewModel
import com.appdev.split.R
import com.appdev.split.UI.Activity.EntryActivity
import com.appdev.split.Utils.Utils
import com.appdev.split.databinding.FragmentPersonalExpenseBinding
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.Calendar


class SingleDostAddExpenseFragment : Fragment() {

    private var _binding: FragmentPersonalExpenseBinding? = null
    private val binding get() = _binding!!
    val mainViewModel by activityViewModels<MainViewModel>()

    private lateinit var adapter: MyFriendSelectionAdapter
    private var friendsList = mutableListOf<FriendContact>()
    private var selectedFriend: FriendContact? = null
    var selectedDate = Utils.getCurrentDate()
    lateinit var dialog: Dialog

    val args: SingleDostAddExpenseFragmentArgs by navArgs()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPersonalExpenseBinding.inflate(layoutInflater, container, false)
        setupShimmer()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? EntryActivity)?.hideBottomBar()
        dialog = Dialog(requireContext())

        if (args.expenseRecord != null) {
            setupEditExpenseMode(args.expenseRecord!!)
            setCalendarFromDate(args.expenseRecord!!.date)
            selectedFriend =
                args.friendData
        } else {
            setupNewExpenseMode()
            setCalendarFromDate(null)
        }

        handleSplitTypeChanges()

        if (args.expenseRecord == null) {
            mainViewModel.fetchAllContacts()
            observeContacts()
        }

        observeExpenseInput()
        setupNavigationListeners()
        setupSaveData()


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


    private fun setupNavigationListeners() {
        binding.addFriends.setOnClickListener {
            val action =
                SingleDostAddExpenseFragmentDirections.actionPersonalExpenseFragmentToAddMembersFragment2(
                    false, ""
                )
            findNavController().navigate(action)
        }

        binding.SplitType.setOnClickListener {
            if (selectedFriend == null) {
                Toast.makeText(requireContext(), "Please select a friend", Toast.LENGTH_SHORT)
                    .show()
            } else {
                if (!TextUtils.isEmpty(binding.amount.editText?.text.toString())) {
                    mainViewModel.updateFriendExpense(
                        title = binding.title.editText?.text.toString(),
                        description = binding.description.editText?.text.toString(),
                        amount = binding.amount.editText?.text.toString()
                    )

                    val action = binding.amount.editText?.let { it1 ->
                        SingleDostAddExpenseFragmentDirections.actionPersonalExpenseFragmentToSplitAmountFragment(
                            null,
                            it1.text.toString()
                                .toFloat(),
                            selectedFriend,
                            binding.splitTypeText.text.toString(),
                            Utils.extractCurrencyCode(binding.currencySpinner.text.toString())
                        )

                    }
                    if (action != null) {
                        mainViewModel.expensePush.value.copy(
                            title = binding.title.editText.toString(),
                            description = binding.description.editText.toString()
                        )
                        findNavController().navigate(action)
                    }
                } else {
                    showError("Amount cannot be empty!")
                }
            }
        }

    }

    fun setCalendarFromDate(dateString: String?) {
        val calendar = Calendar.getInstance()
        var year: Int
        var month: Int
        var day: Int
        if (!dateString.isNullOrEmpty()) {
            val parts = dateString.split("-")
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

        // Open DatePicker with the specified date
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                selectedDate = "$selectedYear-${selectedMonth + 1}-$selectedDay"
                binding.dateCheck.text = selectedDay.toString()
            },
            year, month, day
        )
        binding.calender.setOnClickListener {
            datePickerDialog.show()
        }
    }

    private fun setupSaveData() {
        binding.saveData.setOnClickListener {
            if (validateAndSave()) {
                observeOperationState()
                val title = binding.title.editText?.text.toString()
                val description = binding.description.editText?.text.toString()
                val amount = binding.amount.editText?.text.toString()

                val currentUser = FirebaseAuth.getInstance().currentUser
                val listOUserInSplit: MutableList<FriendContact> = mutableListOf()

                if (currentUser != null && mainViewModel.userData.value != null) {
                    val userId = currentUser.uid // my entry in list for division of amount
                    listOUserInSplit.add(
                        FriendContact(
                            friendId = userId,
                            name = mainViewModel.userData.value!!.name,
                            contact = mainViewModel.userData.value!!.email
                        )
                    )
                    var nameIdList: List<NameId> = listOUserInSplit.map { friend ->
                        NameId(id = friend.friendId, name = friend.name)
                    }


                    // if user didn't change the preset EQUAL SPLIT then calculate data
                    if (mainViewModel.expensePush.value.date.isEmpty() || mainViewModel.expensePush.value.totalAmount < 1f) {
                        if (args.expenseRecord != null) {
                            if (validateAmount(args.expenseRecord!!.splits)) {
                                mainViewModel.updateFriendExpenseDetail(
                                    mainViewModel.expensePush.value.copy(
                                        date = selectedDate,
                                        title = title,
                                        description = description,
                                        totalAmount = amount.toDouble(),
                                        currency = binding.currencySpinner.text.toString(),
                                        expenseCategory = binding.categorySpinner.text.toString(),
                                        splits = if (SplitType.EQUAL.name == binding.splitTypeText.text.toString()
                                            && amount.toDouble() != args.expenseRecord!!.totalAmount
                                        ) handleUpdateSplit(
                                            amount.toDouble(),
                                            args.expenseRecord!!.splits
                                        ) else args.expenseRecord!!.splits,
                                    ), args.expenseRecord!!.id,
                                    args.friendData!!.contact
                                )
                            }
                        } else {
                            selectedFriend?.let { friend ->
                                handleExpenseSplitEqual(amount.toDouble(), nameIdList)
                                mainViewModel.saveFriendExpense(
                                    mainViewModel.expensePush.value.copy(
                                        date = selectedDate,
                                        title = title,
                                        description = description,
                                        totalAmount = amount.toDouble(),
                                        currency = binding.currencySpinner.text.toString(),
                                        expenseCategory = binding.categorySpinner.text.toString(),
                                        paidBy = userId
                                    ),
                                    friend.contact
                                )
                            }
                        }
                    }

                }
            }
        }
    }

    private fun handleExpenseSplitEqual(amount: Double, nameIdList: List<NameId>) {
        val amountHalf = amount / 2

        val distributionList = Utils.createEqualSplits(nameIdList, amountHalf)
        mainViewModel.updateFriendExpense(
            ExpenseRecord(
                totalAmount = amount,
                splits = distributionList,
                date = selectedDate
            )
        )
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
                    is UiState.Error -> showError(state.message)
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


    private fun setupEditExpenseMode(expenseRecord: ExpenseRecord) {
        binding.apply {
            dateCheck.text = Utils.getCurrentDay(expenseRecord.date)
            currencySpinner.selectItemByIndex(getCurrencyIndex(expenseRecord.currency))
            categorySpinner.selectItemByIndex(getCategoryIndex(expenseRecord.expenseCategory))
            title.editText?.setText(expenseRecord.title)
            description.editText?.setText(expenseRecord.description)
            amount.editText?.setText(expenseRecord.totalAmount.toString())
        }

        // Hide contact list as it's not required when editing
        binding.selectedFrisRecyclerView.visibility = View.GONE
        binding.noFriends.visibility = View.GONE
        binding.shimmerViewContainer.visibility = View.GONE
        binding.titleFriends.visibility = View.GONE
        binding.titleTextView.text = "Update expense"
    }

    private fun setupNewExpenseMode() {
        binding.apply {
            dateCheck.text = Utils.getCurrentDay(null)
            currencySpinner.selectItemByIndex(0)
            categorySpinner.selectItemByIndex(0)
        }
        binding.titleTextView.text = "Add expense"
    }

    private fun handleSplitTypeChanges() {
        if (args.expenseRecord != null && mainViewModel.expensePush.value.date.toLong() < args.expenseRecord!!.date.toLong()) {
            val expense = args.expenseRecord!!
            binding.splitTypeText.text = expense.splitType.name
        } else {
            binding.splitTypeText.text = mainViewModel.expensePush.value.splitType.name
        }

    }

    private fun observeContacts() {
        viewLifecycleOwner.lifecycleScope.launch {
            mainViewModel.contactsState.collect { contactsState ->
                when (contactsState) {
                    is UiState.Loading -> showShimmer()
                    is UiState.Success -> {
                        hideShimmer()
                        if (selectedFriend != null) {
                            updateFriendsList(contactsState.data, selectedFriend)
                        } else {
                            updateFriendsList(contactsState.data)
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
                }
            }
        }
    }

    private fun updateFriendsList(
        friends: List<FriendContact>,
        preSelectedFriend: FriendContact? = null
    ) {
        friendsList = friends.toMutableList()
        if (friendsList.isNotEmpty()) {
            binding.selectedFrisRecyclerView.visibility = View.VISIBLE
            binding.noFriends.visibility = View.GONE
            adapter = MyFriendSelectionAdapter(friendsList, isMultiSelect = false) { friend ->
                selectedFriend = if (friend.isNotEmpty()) {
                    friend.first()
                } else {
                    null
                }
            }

            preSelectedFriend?.let { friend ->
                val friendInList = friendsList.find { it.friendId == friend.friendId }
                if (friendInList != null) {
                    adapter.setSelectedFriends(listOf(friendInList))
                    selectedFriend = friendInList
                }
            }

            binding.selectedFrisRecyclerView.adapter = adapter
            binding.selectedFrisRecyclerView.layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter.notifyDataSetChanged()
        } else {
            binding.selectedFrisRecyclerView.visibility = View.GONE
            binding.noFriends.visibility = View.VISIBLE
        }
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

            selectedFriend == null && args.expenseRecord == null -> {
                showToast("Please select at least one friend.")
                return false
            }

            else -> {
                return true
            }
        }
    }

    private fun validateAmount(splitList: List<Split>): Boolean {
        val amount = binding.amount.editText?.text.toString().toDouble()
        val totalAmount = splitList.sumOf { it.amount }
        when {
            args.expenseRecord != null
                    && (args.expenseRecord!!.splitType == SplitType.UNEQUAL || args.expenseRecord!!.splitType == SplitType.PERCENTAGE)
                    && totalAmount != amount -> {
                showToast("Split among group doesn't add up to the total cost!")
                return false
            }

            else -> {
                return true
            }
        }
    }

    fun getCurrencyIndex(name: String): Int {
        val array = resources.getStringArray(R.array.currencies)
        val index = array.indexOf(name)
        return if (index >= 0) {
            index
        } else {
            0
        }
    }

    fun getCategoryIndex(name: String): Int {
        val array = resources.getStringArray(R.array.categories)
        val index = array.indexOf(name)
        return if (index >= 0) {
            index
        } else {
            0
        }
    }


    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun getSelectedRadioButtonText(selectedId: Int): String {
        val view = layoutInflater.inflate(R.layout.bottom_sheet_split_type, null)
        val radioGroup = view.findViewById<RadioGroup>(R.id.splitTypeRadioGroup)
        val selectedRadioButton = radioGroup.findViewById<RadioButton>(selectedId)
        return selectedRadioButton?.text?.toString() ?: " Select split type"
    }


//    private fun showSplitTypeBottomSheet(friendName: String) {
//        val bottomSheetDialog = BottomSheetDialog(requireContext())
//        val view = layoutInflater.inflate(R.layout.bottom_sheet_split_type, null)
//        bottomSheetDialog.setContentView(view)
//
//        var new_id = selectedId
//
//        val radioGroup = view.findViewById<RadioGroup>(R.id.splitTypeRadioGroup)
//        val more = view.findViewById<CardView>(R.id.Save)
//        radioGroup.check(selectedId)
//
//        val friendPaidSplitRadioButton = view.findViewById<RadioButton>(R.id.friendPaidSplit)
//        val friendOwnedFullRadioButton = view.findViewById<RadioButton>(R.id.friendOwnedFull)
//        val iOwnedPaidRadioButton = view.findViewById<RadioButton>(R.id.youPaidSplit)
//        val iOwnedFullRadioButton = view.findViewById<RadioButton>(R.id.youOwnedFull)
//
//        val enteredAmount = binding.amount.editText?.text.toString().toFloatOrNull() ?: 0f
//
//        // Calculate owed amounts based on the entered value
//        val halfAmount = enteredAmount / 2
//
//        iOwnedPaidRadioButton.text = "You paid and split amount\n $friendName owes you $halfAmount"
//        iOwnedFullRadioButton.text = "You owned full amount\n $friendName owes you $enteredAmount"
//        friendPaidSplitRadioButton.text =
//            "$friendName paid and split amount \n You owe $friendName $halfAmount"
//        friendOwnedFullRadioButton.text =
//            "$friendName owned full amount \n You owe $friendName $enteredAmount"
//
//        radioGroup.setOnCheckedChangeListener { _, checkedId ->
//            // Don't update splitTypeText immediately
//            new_id = checkedId
//            selectedId = new_id
//            val selectedOption = when (new_id) {
//                R.id.youPaidSplit -> "You paid and split amount\n $friendName owes you $halfAmount"
//                R.id.youOwnedFull -> "You owned full amount\n $friendName owes you $enteredAmount"
//                R.id.friendPaidSplit -> "$friendName paid and split amount\nYou owe $friendName $halfAmount"
//                R.id.friendOwnedFull -> "$friendName owned full amount\nYou owe $friendName $halfAmount"
//                else -> ""
//            }
//            binding.splitTypeText.text = selectedOption // Now update the text when Save is clicked
////            bottomSheetDialog.dismiss() // Dismiss the bottom sheet after saving
//        }
//
//
//        more.setOnClickListener {
//            bottomSheetDialog.dismiss()
//            if (validateAndSave()) {
//                val action = binding.amount.editText?.let { it1 ->
//                    SingleDostAddExpenseFragmentDirections.actionPersonalExpenseFragmentToSplitAmountFragment(
//                        null,
//                        it1.text.toString()
//                            .toFloat(),
//                        selectedFriend, selectedId
//                    )
//
//                }
//                if (action != null) {
//                    findNavController().navigate(action)
//                }
//            }
//        }
//
//        bottomSheetDialog.show()
//    }

    private fun showError(message: String) {
        Log.d("CHKERR", message)
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
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

    override fun onDestroyView() {
        dialog.dismiss()
        super.onDestroyView()
    }
}

