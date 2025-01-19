package com.appdev.split.UI.Fragment

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.appdev.split.Adapters.SplitMembersAdapter
import com.appdev.split.Model.Data.ExpenseRecord
import com.appdev.split.Model.Data.FriendContact
import com.appdev.split.Model.Data.Member
import com.appdev.split.Model.Data.NameId
import com.appdev.split.Model.Data.Split
import com.appdev.split.Model.Data.SplitType
import com.appdev.split.Model.Data.UiState
import com.appdev.split.Model.ViewModel.MainViewModel
import com.appdev.split.R
import com.appdev.split.Utils.Utils
import com.appdev.split.databinding.FragmentAmountEquallyBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class AmountEquallyFragment(
    val friends: List<FriendContact>,
    val totalAmount: Double,
    val myUserId: String
) : Fragment() {
    private var _binding: FragmentAmountEquallyBinding? = null
    val binding get() = _binding!!
    private lateinit var myadapter: SplitMembersAdapter
    var persons: List<Member> = friends.map { friend ->
        Member(id = friend.friendId, name = friend.name, isSelected = true)
    }


    val mainViewModel by activityViewModels<MainViewModel>()

    lateinit var dialog: Dialog


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAmountEquallyBinding.inflate(layoutInflater, container, false)
        setupRecyclerView()
        setupSelectAll()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog = Dialog(requireContext())

        binding.fabAddMembers.setOnClickListener {
            val selectedMembers = persons.filter { it.isSelected }
            if (persons.find { it.id == myUserId } == null && selectedMembers.size == 1) {
                Toast.makeText(
                    requireContext(),
                    "You cannot add expense that only involve yourself !",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                saveExpenses()
                viewLifecycleOwner.lifecycleScope.launch {
                    repeatOnLifecycle(Lifecycle.State.STARTED) {
                        mainViewModel.operationState.collect { state ->
                            when (state) {
                                is UiState.Loading -> showLoadingIndicator()
                                is UiState.Success -> {
                                    hideLoadingIndicator()
                                    findNavController().navigateUp()
                                }

                                is UiState.Error -> showError(state.message)
                                UiState.Stable -> {

                                }
                            }
                        }
                    }
                }
            }
        }

    }

    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    private fun showLoadingIndicator() {
        dialog.setContentView(R.layout.progress_dialog)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCancelable(false)
        dialog.show()
    }

    private fun hideLoadingIndicator() {
        if (dialog.isShowing) {
            dialog.dismiss()
        }
    }

    private fun setupRecyclerView() {
        myadapter = SplitMembersAdapter { selectedPersons ->
            updateUI(selectedPersons.filter { it.isSelected })
            persons = selectedPersons
            Log.d("CHKME", selectedPersons.toString())
            // Update "All" checkbox - should be checked only if ALL members are selected
            binding.selectAllCheckBox.setOnCheckedChangeListener(null)
            binding.selectAllCheckBox.isChecked = selectedPersons.all { it.isSelected }
            setupSelectAll() // Reattach the listener
        }

        binding.memberRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = myadapter
        }

        // Initialize with all members selected
        val initialMembers = persons.map { it.copy(isSelected = true) }
        myadapter.updatePersons(initialMembers)
        updateUI(initialMembers)
    }

    private fun setupSelectAll() {
        binding.selectAllCheckBox.setOnCheckedChangeListener { _, isChecked ->
            myadapter.selectAll(isChecked)
        }
    }

    private fun updateUI(selectedPersons: List<Member>) {
        val selectedCount = selectedPersons.size

        when {
            selectedCount == 0 -> {
                binding.pricePerPerson.text = "Select at least one person"
                binding.countPerson.visibility = View.GONE
            }

            else -> {
                val amountPerPerson = totalAmount / selectedCount
                binding.countPerson.visibility = View.VISIBLE
                binding.pricePerPerson.text = "$${String.format("%.2f", amountPerPerson)}/person"
                binding.countPerson.text = "($selectedCount people)"
            }
        }
    }


    private fun saveExpenses() {
        val selectedMembers = persons.filter { it.isSelected }
        var nameIdList: List<NameId> = selectedMembers.map { friend ->
            NameId(id = friend.id, name = friend.name)
        }
        val amountPerPerson = totalAmount / selectedMembers.size

//        if (selectedMembers.size == 1 && foundPerson != null && selectedId != R.id.friendOwnedFull && selectedId != R.id.friendPaidSplit) {
//            val navOptions = NavOptions.Builder()
//                .setPopUpTo(R.id.home_page, inclusive = true)
//                .build()
//
//            findNavController().navigate(R.id.home_page, null, navOptions)
//        }

        // if i owe full amount i mean second id is selected friend is going to lent me full amount also uncheck my name from list
        // then nothing to change except unchecked any column by user
//        if (selectedId == R.id.youPaidSplit || selectedId == R.id.youOwnedFull) {
//
//            if (selectedId == R.id.youPaidSplit && amountPerPerson == totalAmount) {
//                newId = R.id.youOwnedFull
//            } else if (selectedId == R.id.youOwnedFull && amountPerPerson < totalAmount) {
//                newId = R.id.youPaidSplit
//            }
//            expenseRecord = ExpenseRecord(
//                paidAmount = totalAmount,
//                lentAmount = amountPerPerson
//            )
//        } else {
//            if (selectedId == R.id.friendOwnedFull && amountPerPerson < totalAmount) {
//                newId = R.id.friendPaidSplit
//            } else if(selectedId == R.id.friendPaidSplit && amountPerPerson == totalAmount) {
//                newId = R.id.friendOwnedFull
//            }
//            expenseRecord = ExpenseRecord(
//                paidAmount = totalAmount,

//                lentAmount = 0f,
//                borrowedAmount = amountPerPerson
//            )
//        }
        val distributionList = Utils.createEqualSplits(nameIdList,amountPerPerson)

        val expenseRecord = ExpenseRecord(
            totalAmount = totalAmount,
            splitType = SplitType.EQUAL,
            splits = distributionList
        )

        // if friend pay and split i borrow half amount then fill borrow and leave lent

        // if friend paid all then full borrow

        //store selected id based on user selection in VM
//        mainViewModel.updateContacts(friendsList)
        mainViewModel.updateFriendExpense(expenseRecord)
    }

    private fun getCurrentDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }



}