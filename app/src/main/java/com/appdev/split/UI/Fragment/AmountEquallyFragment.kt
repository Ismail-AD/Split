package com.appdev.split.UI.Fragment

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.appdev.split.Adapters.SplitMembersAdapter
import com.appdev.split.Model.Data.ExpenseRecord
import com.appdev.split.Model.Data.Friend
import com.appdev.split.Model.Data.Member
import com.appdev.split.Model.Data.UiState
import com.appdev.split.Model.ViewModel.MainViewModel
import com.appdev.split.R
import com.appdev.split.databinding.FragmentAmountEquallyBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class AmountEquallyFragment(friendsList: List<Friend>, totalAmount: Float) : Fragment() {
    private var _binding: FragmentAmountEquallyBinding? = null
    val binding get() = _binding!!
    private lateinit var myadapter: SplitMembersAdapter
    val totalAmount = totalAmount
    val friends = friendsList
    val persons: List<Member> by lazy {
        friendsList.map { friend ->
            Member(id = friend.contact, name = friend.name, isSelected = true)
        }
    }
    val mainViewModel by viewModels<MainViewModel>()

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
            saveExpenses(friendsList = friends)
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



    private fun saveExpenses(friendsList: List<Friend>) {
        val selectedMembers = persons.filter { it.isSelected }
        val amountPerPerson = totalAmount / selectedMembers.size

        selectedMembers.forEach { member ->
            // Find the corresponding Friend object
            val friend = friendsList.find { it.contact == member.id }
            friend?.let {
                // Update the expense record for each friend
                val expenseRecord = ExpenseRecord(
                    paidAmount = totalAmount,
                    lentAmount = amountPerPerson,
                    date = getCurrentDate() // You can create a utility function for current date
                )
                it.expenseRecords.add(expenseRecord)
            }
        }
        mainViewModel.updateContacts(friendsList)


        // Now save the updated friend objects back to your database or wherever necessary
    }

    private fun getCurrentDate(): String {
        // Helper function to get current date
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

}