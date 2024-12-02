package com.appdev.split

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
import com.appdev.split.Model.Data.Friend
import com.appdev.split.Model.Data.UiState
import com.appdev.split.Model.ViewModel.MainViewModel
import com.appdev.split.UI.Activity.EntryActivity
import com.appdev.split.UI.Fragment.AddGrpExpenseFragmentArgs
import com.appdev.split.UI.Fragment.AddGrpExpenseFragmentDirections
import com.appdev.split.databinding.FragmentAddGrpExpenseBinding
import com.appdev.split.databinding.FragmentPersonalExpenseBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch


class PersonalExpenseFragment : Fragment() {

    private var _binding: FragmentPersonalExpenseBinding? = null
    private val binding get() = _binding!!
    val mainViewModel by activityViewModels<MainViewModel>()

    private lateinit var adapter: MyFriendSelectionAdapter
    private var friendsList = mutableListOf<Friend>()
    private var selectedFriend: Friend? = null
    var selectedId = R.id.youPaidSplit

    lateinit var dialog: Dialog

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

        if (mainViewModel._newSelectedId != -1) {
            selectedId = mainViewModel._newSelectedId
            binding.splitTypeText.text = getSelectedRadioButtonText(selectedId)
            Log.d("CHKFRIE", "$selectedId at main")
            mainViewModel._newSelectedId = -1
        } else {
            binding.splitTypeText.text = getSelectedRadioButtonText(selectedId)
        }


        binding.currencySpinner.selectItemByIndex(0)
        binding.categorySpinner.selectItemByIndex(0)

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    mainViewModel.contactsState,
                    mainViewModel.expenseInput
                ) { contactsState, expenseInput ->
                    // Handle both states together
                    when (contactsState) {
                        is UiState.Loading -> showLoadingIndicator()
                        is UiState.Success -> {
                            hideLoadingIndicator()
                            friendsList = contactsState.data.toMutableList()
                            Log.d("CHKMYFRI", "$friendsList")

                            // Update friends list UI
                            if (friendsList.isNotEmpty()) {
                                binding.selectedFrisRecyclerView.visibility = View.VISIBLE
                                binding.noFriends.visibility = View.GONE
                                adapter = MyFriendSelectionAdapter(
                                    friendsList,
                                    selectedFriend
                                ) { friend ->
                                    selectedFriend = friend
                                }
                                binding.selectedFrisRecyclerView.adapter = adapter
                                binding.selectedFrisRecyclerView.layoutManager =
                                    LinearLayoutManager(
                                        requireContext(),
                                        LinearLayoutManager.HORIZONTAL,
                                        false
                                    )
                                adapter.notifyDataSetChanged()
                            } else {
                                binding.selectedFrisRecyclerView.visibility = View.GONE
                                binding.noFriends.visibility = View.VISIBLE
                            }
                        }
                        is UiState.Error -> showError(contactsState.message)
                        else -> {}
                    }

                    // Update input fields
                    Log.d("CHKITMVM", expenseInput.toString())
                    binding.apply {
                        if (title.editText?.text.isNullOrEmpty()) {
                            title.editText?.setText(expenseInput.title)
                        }
                        if (description.editText?.text.isNullOrEmpty()) {
                            description.editText?.setText(expenseInput.description)
                        }
                        if (amount.editText?.text.isNullOrEmpty() && expenseInput.amount!=0f) {
                            amount.editText?.setText(expenseInput.amount.toString())
                        }
                    }
                }.collect{

                }
            }
        }
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

        binding.Split.setOnClickListener {
            if (validateAndSave()) {
                Log.d("CHKITMVM",mainViewModel.expenseInput.value.toString())
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

            selectedFriend == null -> {
                showToast("Please select at least one friend.")
                return false
            }

            else -> {
                mainViewModel.updateTitle(title)
                mainViewModel.updateDescription(description)
                mainViewModel.updateAmount(amount.toFloat())
                return true
            }
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
        val save = view.findViewById<CardView>(R.id.Save)
        radioGroup.check(selectedId)

        val friendPaidSplitRadioButton = view.findViewById<RadioButton>(R.id.friendPaidSplit)
        val friendOwnedFullRadioButton = view.findViewById<RadioButton>(R.id.friendOwnedFull)

        friendPaidSplitRadioButton.text = "$friendName paid and split amount"
        friendOwnedFullRadioButton.text = "$friendName owned full amount"

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            // Don't update splitTypeText immediately
            new_id = checkedId // Update the selectedId for saving
        }


        save.setOnClickListener {
            // Update the splitTypeText with the selected option only when Save is clicked
            val selectedOption = when (new_id) {
                R.id.youPaidSplit -> "You paid and split amount"
                R.id.youOwnedFull -> "You owned full amount"
                R.id.friendPaidSplit -> "$friendName paid and split amount"
                R.id.friendOwnedFull -> "$friendName owned full amount"
                else -> ""
            }
            selectedId = new_id
            binding.splitTypeText.text = selectedOption // Now update the text when Save is clicked
            bottomSheetDialog.dismiss() // Dismiss the bottom sheet after saving
        }

        bottomSheetDialog.show()
    }

    private fun showError(message: String) {
        Log.d("CHKERR", message)
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

}

