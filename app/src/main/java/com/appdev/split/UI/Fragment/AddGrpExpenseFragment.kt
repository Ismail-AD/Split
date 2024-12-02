package com.appdev.split.UI.Fragment

import android.app.Dialog
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import com.appdev.split.Model.Data.UiState
import com.appdev.split.Model.ViewModel.MainViewModel
import com.appdev.split.R
import com.appdev.split.UI.Activity.EntryActivity
import com.appdev.split.databinding.DialogMemberListBinding
import com.appdev.split.databinding.FragmentAddGrpExpenseBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddGrpExpenseFragment : Fragment() {
    private var _binding: FragmentAddGrpExpenseBinding? = null
    private val binding get() = _binding!!
    private val selectedMembers = mutableSetOf<String>()
    val mainViewModel by activityViewModels<MainViewModel>()

    private lateinit var adapter: MyFriendSelectionAdapter
    private var friendsList = mutableListOf<Friend>()
    val selectedFriends = mutableSetOf<Friend>()
    val args: AddGrpExpenseFragmentArgs by navArgs()
    lateinit var dialog: Dialog


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
        dialog = Dialog(requireContext())

        binding.currencySpinner.selectItemByIndex(0)
        binding.categorySpinner.selectItemByIndex(0)
        binding.memberSelect.setOnClickListener {
            showMemberDialog()
        }

        binding.doneTextView.setOnClickListener {
//            if (validateAndSave(isGroupExpense)) {
//
//            }
        }
        binding.Split.setOnClickListener {
//            if (validateAndSave(isGroupExpense)) {
//                val action = binding.amount.editText?.let { it1 ->
//                    AddGrpExpenseFragmentDirections.actionAddGrpExpenseFragmentToSplitAmountFragment(
//                        selectedFriends.toList().toTypedArray(),
//                        it1.text.toString().toFloat()
//                    )
//                }
//                if (action != null) {
//                    findNavController().navigate(action)
//                }
//            }
        }

        binding.closeIcon.setOnClickListener {
            findNavController().navigateUp()
        }

    }

    private fun showError(message: String) {
        Log.d("CHKERR",message)
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

    private fun updateViewsVisibility(
        isAllMembersSelected: Boolean,
        binding: DialogMemberListBinding
    ) {
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

    private fun validateAndSave(isGroupExpense: Boolean): Boolean {
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

            isGroupExpense && selectedMembers.isEmpty() -> {
                showToast("Please select at least one member.")
                return false
            }

            !isGroupExpense && selectedFriends.isEmpty() -> {
                showToast("Please select at least one friend.")
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
        (activity as? EntryActivity)?.showBottomBar()

    }
}