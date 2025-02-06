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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.appdev.split.Adapters.SplitMembersAdapter
import com.appdev.split.Model.Data.ExpenseRecord
import com.appdev.split.Model.Data.FriendContact
import com.appdev.split.Model.Data.Member
import com.appdev.split.Model.Data.NameId
import com.appdev.split.Model.Data.SplitType
import com.appdev.split.Model.ViewModel.MainViewModel
import com.appdev.split.R
import com.appdev.split.Utils.Utils
import com.appdev.split.databinding.FragmentAmountEquallyBinding
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class AmountEquallyFragment(
    val friends: List<FriendContact>,
    val totalAmount: Double,
    val myUserId: String,
    val currency: String
) : Fragment() {
    private var _binding: FragmentAmountEquallyBinding? = null
    val binding get() = _binding!!
    private lateinit var myadapter: SplitMembersAdapter
    var persons: List<Member> = friends.map { friend ->
        Member(
            id = friend.friendId,
            name = friend.name,
            imageUrl = friend.profileImageUrl
        )
    }


    val mainViewModel by activityViewModels<MainViewModel>()

    lateinit var dialog: Dialog


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAmountEquallyBinding.inflate(layoutInflater, container, false)
        checkViewModelData()
        setupRecyclerView()
        setupSelectAll()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog = Dialog(requireContext())

        binding.fabAddMembers.setOnClickListener {
            val selectedMembers = persons.filter { it.isSelected }
            if (selectedMembers.find { it.id == myUserId } != null && selectedMembers.size == 1) {
                Toast.makeText(
                    requireContext(),
                    "Cannot create an expense with just yourself - select friends to split with",
                    Toast.LENGTH_LONG
                ).show()
            } else if (selectedMembers.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Please select at least one friend to split expenses with",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                saveExpenses()
            }
        }

    }


    private fun checkViewModelData() {
        // Get the current expense record from ViewModel
        val currentExpense = mainViewModel.getExpenseObject()
        Log.d("AmountEqually", "${currentExpense.splits}")

        if (currentExpense != null && currentExpense.splits.isNotEmpty() && currentExpense.splitType == SplitType.EQUAL.name) {
            // Get the IDs of members who are part of the split
            val splitMemberIds = currentExpense.splits.map { it.userId }.toSet()

            // Update the persons list based on split data
            persons = persons.map { person ->
                person.copy(isSelected = person.id in splitMemberIds)
            }

            Log.d("AmountEqually", "Loaded existing split data: ${splitMemberIds.size} members")
        } else {
            persons = persons.map { it.copy(isSelected = true) }
        }
    }

    private fun setupRecyclerView() {
        myadapter = SplitMembersAdapter { selectedPersons ->
            Log.d("CHKME", "ARRIVED SELECTED PERSONS: $selectedPersons")
            updateUI(selectedPersons)
            persons = selectedPersons
        }

        binding.memberRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = myadapter
        }

        // Initialize with all members selected
        myadapter.updatePersons(persons)
        updateUI(persons)
    }

    private fun setupSelectAll() {
        binding.selectAllCheckBox.setOnCheckedChangeListener { _, isChecked ->
            myadapter.selectAll(isChecked)
        }
    }

    private fun updateUI(selectedPersons: List<Member>) {
        val selectedCount = selectedPersons.count { it.isSelected }
        Log.d("CHKME", "COUNT: $selectedCount")

        when {
            selectedCount == 0 -> {
                binding.pricePerPerson.text = "Select at least one person"
                binding.countPerson.visibility = View.GONE
            }

            else -> {
                val amountPerPerson = totalAmount / selectedCount
                binding.countPerson.visibility = View.VISIBLE
                binding.pricePerPerson.text =
                    "${currency}${String.format("%.2f", amountPerPerson)}/person"
                binding.countPerson.text = "($selectedCount people)"
            }
        }

        binding.selectAllCheckBox.setOnCheckedChangeListener(null)
        binding.selectAllCheckBox.isChecked = selectedPersons.all { it.isSelected }
        setupSelectAll()
    }


    private fun saveExpenses() {
        val selectedMembers = persons.filter { it.isSelected }
        var nameIdList: List<NameId> = selectedMembers.map { friend ->
            NameId(id = friend.id, name = friend.name)
        }
        val amountPerPerson = totalAmount / selectedMembers.size

        val distributionList = Utils.createEqualSplits(nameIdList, amountPerPerson)

        val expenseRecord = ExpenseRecord(
            totalAmount = totalAmount,
            splitType = SplitType.EQUAL.name,
            splits = distributionList
        )

        mainViewModel.updateFriendExpense(expenseRecord)
        findNavController().navigateUp()
    }

    private fun getCurrentDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}