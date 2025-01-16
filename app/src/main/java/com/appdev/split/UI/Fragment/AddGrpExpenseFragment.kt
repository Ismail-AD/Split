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
import com.appdev.split.Model.Data.Friend
import com.appdev.split.Model.Data.FriendContact
import com.appdev.split.Model.Data.UiState
import com.appdev.split.Model.ViewModel.MainViewModel
import com.appdev.split.R
import com.appdev.split.UI.Activity.EntryActivity
import com.appdev.split.Utils.Utils
import com.appdev.split.databinding.DialogMemberListBinding
import com.appdev.split.databinding.FragmentAddGrpExpenseBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar

@AndroidEntryPoint
class AddGrpExpenseFragment : Fragment() {
    private var _binding: FragmentAddGrpExpenseBinding? = null
    private val binding get() = _binding!!
    private val selectedMembers = mutableSetOf<String>()
    val mainViewModel by activityViewModels<MainViewModel>()

    private lateinit var adapter: MyFriendSelectionAdapter
    private var friendsList = mutableListOf<FriendContact>()
    val selectedFriends = mutableSetOf<FriendContact>()
    val args: AddGrpExpenseFragmentArgs by navArgs()
    lateinit var dialog: Dialog
    var selectedId = R.id.youPaidSplit

    var selectedDate = Utils.getCurrentDate()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentAddGrpExpenseBinding.inflate(layoutInflater, container, false)
        setupShimmer()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog = Dialog(requireContext())


        if (args.expenseRecord == null) {
            mainViewModel.fetchAllContacts()
            observeContacts()
        }

        binding.currencySpinner.selectItemByIndex(0)
        binding.categorySpinner.selectItemByIndex(0)


        binding.doneTextView.setOnClickListener {
//            if (validateAndSave(isGroupExpense)) {
//
//            }
        }
        binding.Split.setOnClickListener {
            if (validateAndSave()) {
                val action = binding.amount.editText?.let { it1 ->
                    AddGrpExpenseFragmentDirections.actionAddGrpExpenseFragmentToSplitAmountFragment(
                        selectedFriends.toList().toTypedArray(),
                        it1.text.toString().toFloat(),null,R.id.youPaidSplit
                    )
                }
                if (action != null) {
                    findNavController().navigate(action)
                }
            }
        }

        binding.closeIcon.setOnClickListener {
            findNavController().navigateUp()
        }

    }

//    private fun showSplitTypeBottomSheet() {
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
//        iOwnedPaidRadioButton.text = "You paid and split amount\n each member owes you $halfAmount"
//        iOwnedFullRadioButton.text = "You owned full amount\n each member owes you $enteredAmount"
//        friendPaidSplitRadioButton.text =
//            "$friendName paid and split amount \n each member owes $halfAmount"
//        friendOwnedFullRadioButton.text =
//            "$friendName owned full amount \n each member owes $enteredAmount"
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


    private fun observeContacts() {
        viewLifecycleOwner.lifecycleScope.launch {
            mainViewModel.contactsState.collect { contactsState ->
                when (contactsState) {
                    is UiState.Loading -> showShimmer()
                    is UiState.Success -> {
                        hideShimmer()
                        updateFriendsList(contactsState.data)
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

    private fun updateFriendsList(friends: List<FriendContact>) {
        friendsList = friends.toMutableList()
        if (friendsList.isNotEmpty()) {
            binding.selectedFrisRecyclerView.visibility = View.VISIBLE
            binding.noFriends.visibility = View.GONE
            adapter = MyFriendSelectionAdapter(friendsList, true) { selectedFriends ->
                this.selectedFriends.clear()
                this.selectedFriends.addAll(selectedFriends)
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
        Log.d("CHKERR",message)
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

            selectedMembers.isEmpty() -> {
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
        _binding=null
    }
}