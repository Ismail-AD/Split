package com.appdev.split

import android.os.Bundle
import android.text.TextUtils
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.appdev.split.Adapters.MemberAdapter
import com.appdev.split.databinding.DialogMemberListBinding
import com.appdev.split.databinding.FragmentAddGroupBinding
import com.appdev.split.databinding.FragmentAddGrpExpenseBinding

class AddGrpExpenseFragment : Fragment() {
    private var _binding: FragmentAddGrpExpenseBinding? = null
    private val binding get() = _binding!!
    private val selectedMembers = mutableSetOf<String>()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentAddGrpExpenseBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? EntryActivity)?.hideBottomBar()

        binding.currencySpinner.selectItemByIndex(0)
        binding.categorySpinner.selectItemByIndex(0)
        binding.memberSelect.setOnClickListener {
            showMemberDialog()
        }

        binding.doneTextView.setOnClickListener {
            validateAndSave()
        }
        binding.Split.setOnClickListener {
            findNavController().navigate(R.id.action_addGrpExpenseFragment_to_splitAmountFragment)
        }

    }
    private fun showMemberDialog() {
        val dialogBinding = DialogMemberListBinding.inflate(layoutInflater)

        val members = listOf("Alice", "Bob", "Charlie", "Diana")
        val adapter = MemberAdapter(members, selectedMembers)

        dialogBinding.memberRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        dialogBinding.memberRecyclerView.adapter = adapter

        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()
        dialog.setCanceledOnTouchOutside(false)

        dialogBinding.apply {
            memberRecyclerView.layoutManager = LinearLayoutManager(requireContext())
            memberRecyclerView.adapter = adapter

            // Initial visibility state
            updateViewsVisibility(allMembersRadioButton.isChecked, dialogBinding)

            // Radio group selection listener
            radioGroup.setOnCheckedChangeListener { _, checkedId ->
                updateViewsVisibility(checkedId == R.id.allMembersRadioButton, dialogBinding)
            }

            saveButton.setOnClickListener {
                // Get selected members from the adapter
                val selectedMembers = adapter.getSelectedMembers()

                // Check if no individual members are selected
                if (individualSelectionRadioButton.isChecked && selectedMembers.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        "Please select at least one member",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    // Proceed with your saving logic
                    // If selectedMembers is not empty, save the selections
                }
            }

            cancelButton.setOnClickListener {
                dialog.dismiss()
            }

            searchField.addTextChangedListener { text ->
                adapter.filterData(text.toString())
            }

        }

        dialog.show()
    }
    private fun updateViewsVisibility(isAllMembersSelected: Boolean, binding: DialogMemberListBinding) {
        binding.apply {
            if (isAllMembersSelected) {
                searchField.visibility = View.GONE
                memberRecyclerView.visibility = View.GONE
            } else {
                searchField.visibility = View.VISIBLE
                memberRecyclerView.visibility = View.VISIBLE
            }
        }
    }

    private fun validateAndSave() {
        val title = binding.title.editText?.text.toString()
        val description = binding.description.editText?.text.toString()
        val amount = binding.amount.editText?.text.toString()

        when {
            TextUtils.isEmpty(title) -> {
                showToast("Title cannot be empty")
            }
            TextUtils.isEmpty(description) -> {
                showToast("Description cannot be empty")
            }
            TextUtils.isEmpty(amount) -> {
                showToast("Amount cannot be empty")
            }
            selectedMembers.isEmpty() -> {
                showToast("Please select at least one member.")
            }
            else -> {
                showToast("Expense saved successfully!")
                // Perform save operation here
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        (activity as? EntryActivity)?.showBottomBar()

    }
}