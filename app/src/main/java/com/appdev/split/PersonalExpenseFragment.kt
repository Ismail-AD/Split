package com.appdev.split

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
import androidx.cardview.widget.CardView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.appdev.split.Adapters.MyFriendSelectionAdapter
import com.appdev.split.Model.Data.ExpenseRecord
import com.appdev.split.Model.Data.FriendContact
import com.appdev.split.Model.Data.UiState
import com.appdev.split.Model.ViewModel.MainViewModel
import com.appdev.split.UI.Activity.EntryActivity
import com.appdev.split.UI.Fragment.AddGrpExpenseFragmentDirections
import com.appdev.split.Utils.Utils
import com.appdev.split.databinding.FragmentPersonalExpenseBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar


class PersonalExpenseFragment : Fragment() {

    private var _binding: FragmentPersonalExpenseBinding? = null
    private val binding get() = _binding!!
    val mainViewModel by activityViewModels<MainViewModel>()

    private lateinit var adapter: MyFriendSelectionAdapter
    private var friendsList = mutableListOf<FriendContact>()
    private var selectedFriend: FriendContact? = null
    var selectedId = R.id.youPaidSplit
    var selectedDate = Utils.getCurrentDate()
    lateinit var dialog: Dialog

    val args: PersonalExpenseFragmentArgs by navArgs()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPersonalExpenseBinding.inflate(layoutInflater, container, false)

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
                FriendContact(name = args.friendName!!, contact = args.friendsContact!!)
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


    private fun setupNavigationListeners() {
        binding.addFriends.setOnClickListener {
            val action =
                AddGrpExpenseFragmentDirections.actionAddGrpExpenseFragmentToAddMembersFragment(
                    false
                )
            findNavController().navigate(action)
        }

        binding.SplitType.setOnClickListener {
            if (selectedFriend == null) {
                Toast.makeText(requireContext(), "Please select a friend", Toast.LENGTH_SHORT)
                    .show()
            } else {
                showSplitTypeBottomSheet(selectedFriend!!.name)
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
                val amountHalf = amount.toFloat() / 2

                if (mainViewModel.expensePush.value.date.isEmpty() || mainViewModel.expensePush.value.paidAmount < 1f) {
                    if (args.expenseRecord != null) {
                        handleExpenseSplit(amount.toFloat(), amountHalf)
                    } else {
                        selectedFriend?.let { friend ->
                            handleExpenseSplit(amount.toFloat(), amountHalf)
                        }
                    }
                }

                if (args.expenseRecord != null) {
                    mainViewModel.updateFriendExpenseDetail(
                        mainViewModel.expensePush.value.copy(
                            date = selectedDate,
                            title = title,
                            description = description,
                            amount = amount.toFloat(),
                            currency = binding.currencySpinner.text.toString(),
                            expenseFor = binding.categorySpinner.text.toString()
                        ), args.expenseRecord!!.expenseId,
                        args.friendsContact!!
                    )
                } else {
                    selectedFriend?.let { friend ->
                        mainViewModel.saveFriendExpense(
                            mainViewModel.expensePush.value.copy(
                                date = selectedDate,
                                title = title,
                                description = description,
                                amount = amount.toFloat(),
                                currency = binding.currencySpinner.text.toString(),
                                expenseFor = binding.categorySpinner.text.toString()
                            ),
                            friend.contact
                        )
                    }
                }
            }
        }
    }

    private fun handleExpenseSplit(amount: Float, amountHalf: Float) {
        when (selectedId) {
            R.id.youPaidSplit -> {
                mainViewModel.updateFriendExpense(
                    ExpenseRecord(
                        paidAmount = amount,
                        lentAmount = amountHalf,
                        borrowedAmount = 0f,
                        date = selectedDate
                    ), selectedId
                )
            }

            R.id.youOwnedFull -> {
                mainViewModel.updateFriendExpense(
                    ExpenseRecord(
                        paidAmount = amount,
                        lentAmount = amount,
                        borrowedAmount = 0f,
                        date = selectedDate
                    ), selectedId
                )
            }

            R.id.friendPaidSplit -> {
                mainViewModel.updateFriendExpense(
                    ExpenseRecord(
                        paidAmount = amount,
                        lentAmount = 0f,
                        borrowedAmount = amountHalf,
                        date = selectedDate
                    ), selectedId
                )
            }

            R.id.friendOwnedFull -> {
                mainViewModel.updateFriendExpense(
                    ExpenseRecord(
                        paidAmount = amount,
                        lentAmount = 0f,
                        borrowedAmount = amount,
                        date = selectedDate
                    ), selectedId
                )
            }
        }
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
            categorySpinner.selectItemByIndex(getCategoryIndex(expenseRecord.expenseFor))
            title.editText?.setText(expenseRecord.title)
            description.editText?.setText(expenseRecord.description)
            amount.editText?.setText(expenseRecord.amount.toString())
        }

        // Hide contact list as it's not required when editing
        binding.selectedFrisRecyclerView.visibility = View.GONE
        binding.noFriends.visibility = View.GONE
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
        if (args.expenseRecord != null) {
            val expense = args.expenseRecord!!
            selectedId = when {
                expense.lentAmount > 0f -> {
                    val amount = expense.paidAmount - expense.lentAmount
                    if (amount > 0) R.id.youPaidSplit else R.id.youOwnedFull
                }

                else -> {
                    val amount = expense.paidAmount - expense.borrowedAmount
                    if (amount > 0) R.id.friendPaidSplit else R.id.friendOwnedFull
                }
            }
        } else if (mainViewModel._newSelectedId != -1) {
            selectedId = mainViewModel._newSelectedId
            mainViewModel._newSelectedId = -1
        }

        binding.splitTypeText.text = getSelectedRadioButtonText(selectedId)
    }

    private fun observeContacts() {
        viewLifecycleOwner.lifecycleScope.launch {
            mainViewModel.contactsState.collect { contactsState ->
                when (contactsState) {
                    is UiState.Loading -> showLoadingIndicator()
                    is UiState.Success -> {
                        hideLoadingIndicator()
                        updateFriendsList(contactsState.data)
                    }

                    is UiState.Error -> showError(contactsState.message)
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
                    if (amount.editText?.text.isNullOrEmpty() && expenseInput.amount != 0f) {
                        amount.editText?.setText(expenseInput.amount.toString())
                    }
                }
            }
        }
    }

    private fun updateFriendsList(friends: List<FriendContact>) {
        friendsList = friends.toMutableList()
        if (friendsList.isNotEmpty()) {
            binding.selectedFrisRecyclerView.visibility = View.VISIBLE
            binding.noFriends.visibility = View.GONE
            adapter = MyFriendSelectionAdapter(friendsList, selectedFriend) { friend ->
                selectedFriend = friend
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


    private fun showSplitTypeBottomSheet(friendName: String) {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_split_type, null)
        bottomSheetDialog.setContentView(view)

        var new_id = selectedId

        val radioGroup = view.findViewById<RadioGroup>(R.id.splitTypeRadioGroup)
        val more = view.findViewById<CardView>(R.id.Save)
        radioGroup.check(selectedId)

        val friendPaidSplitRadioButton = view.findViewById<RadioButton>(R.id.friendPaidSplit)
        val friendOwnedFullRadioButton = view.findViewById<RadioButton>(R.id.friendOwnedFull)
        val iOwnedPaidRadioButton = view.findViewById<RadioButton>(R.id.youPaidSplit)
        val iOwnedFullRadioButton = view.findViewById<RadioButton>(R.id.youOwnedFull)

        val enteredAmount = binding.amount.editText?.text.toString().toFloatOrNull() ?: 0f

        // Calculate owed amounts based on the entered value
        val halfAmount = enteredAmount / 2

        iOwnedPaidRadioButton.text = "You paid and split amount\n $friendName owes you $halfAmount"
        iOwnedFullRadioButton.text = "You owned full amount\n $friendName owes you $enteredAmount"
        friendPaidSplitRadioButton.text =
            "$friendName paid and split amount \n You owe $friendName $halfAmount"
        friendOwnedFullRadioButton.text =
            "$friendName owned full amount \n You owe $friendName $enteredAmount"

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            // Don't update splitTypeText immediately
            new_id = checkedId
            selectedId = new_id
            val selectedOption = when (new_id) {
                R.id.youPaidSplit -> "You paid and split amount\n $friendName owes you $halfAmount"
                R.id.youOwnedFull -> "You owned full amount\n $friendName owes you $enteredAmount"
                R.id.friendPaidSplit -> "$friendName paid and split amount\nYou owe $friendName $halfAmount"
                R.id.friendOwnedFull -> "$friendName owned full amount\nYou owe $friendName $halfAmount"
                else -> ""
            }
            binding.splitTypeText.text = selectedOption // Now update the text when Save is clicked
//            bottomSheetDialog.dismiss() // Dismiss the bottom sheet after saving
        }


        more.setOnClickListener {
            bottomSheetDialog.dismiss()
            if (validateAndSave()) {
                val action = binding.amount.editText?.let { it1 ->
                    PersonalExpenseFragmentDirections.actionPersonalExpenseFragmentToSplitAmountFragment(
                        null,
                        it1.text.toString()
                            .toFloat(),
                        selectedFriend, selectedId
                    )

                }
                if (action != null) {
                    findNavController().navigate(action)
                }
            }
        }

        bottomSheetDialog.show()
    }

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

