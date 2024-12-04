package com.appdev.split.UI.Fragment

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.appdev.split.Adapters.PercentageDistributeAdapter
import com.appdev.split.Model.Data.ExpenseRecord
import com.appdev.split.Model.Data.Friend
import com.appdev.split.Model.Data.FriendContact
import com.appdev.split.Model.Data.Percentage
import com.appdev.split.Model.Data.UiState
import com.appdev.split.Model.ViewModel.MainViewModel
import com.appdev.split.R
import com.appdev.split.databinding.FragmentAmountPercentBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class AmountPercentFragment(
    val friendsList: List<FriendContact>,
    val totalAmount: Float,
    val selectedId: Int,
    val myEmail: String
) : Fragment() {

    private var _binding: FragmentAmountPercentBinding? = null
    private val binding get() = _binding!!
    val friends = friendsList
    private val totalPercentage = 100f
    private val payments = friendsList.map { friend ->
        Percentage(friend.contact, friend.name)
    }
    private val mainViewModel by activityViewModels<MainViewModel>()
    private lateinit var dialog: Dialog

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAmountPercentBinding.inflate(layoutInflater, container, false)
        dialog = Dialog(requireContext())
        setupRecyclerView()
        updateProgress(0f)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.fabAddMembers.setOnClickListener {
            val currentTotalPercentage = payments.sumOf { it.percentage.toDouble() }
            val selectedMembers = payments.filter { it.percentage > 0 }

            when {
                currentTotalPercentage < 100 -> {
                    Toast.makeText(
                        requireContext(),
                        "Total percentage is less than 100%",
                        Toast.LENGTH_LONG
                    ).show()
                }

                selectedMembers.find { it.id == myEmail } == null &&
                        (selectedId == R.id.friendPaidSplit || selectedId == R.id.friendOwnedFull) &&
                        selectedMembers.size == 1 -> {
                    Toast.makeText(
                        requireContext(),
                        "You cannot add expense that doesn't involve yourself!",
                        Toast.LENGTH_LONG
                    ).show()
                }

                currentTotalPercentage > 100 -> {
                    Toast.makeText(
                        requireContext(),
                        "Total percentage is greater than 100%",
                        Toast.LENGTH_LONG
                    ).show()
                }

                currentTotalPercentage == 0.0 -> {
                    Toast.makeText(
                        requireContext(),
                        "Please enter some percentages",
                        Toast.LENGTH_LONG
                    ).show()
                }

                else -> {
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
        }
    }

    private fun setupRecyclerView() {
        binding.memberRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = PercentageDistributeAdapter(payments, totalAmount) { percentage ->
                updateProgress(percentage)
            }
        }
    }

    private fun updateProgress(totalPercentageAllocated: Float) {
        binding.apply {
            tvPercentage.text = "${totalPercentageAllocated.toInt()}% of 100%"
            val remainingPercentage = totalPercentage - totalPercentageAllocated

            when {
                totalPercentageAllocated > 100 -> {
                    tvAmountLeft.apply {
                        text = "${abs(remainingPercentage).toInt()}% over"
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.error_red))
                    }
                }

                totalPercentageAllocated == 100f -> {
                    tvAmountLeft.apply {
                        text = "Perfect split!"
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.green))
                    }
                }

                else -> {
                    tvAmountLeft.apply {
                        text = "${remainingPercentage.toInt()}% left"
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.gray))
                    }
                }
            }
        }
    }

    private fun saveExpenses(friendsList: List<FriendContact>) {
        val selectedPayments = payments.filter { it.percentage > 0 }
        val selectedFriends = selectedPayments.mapNotNull { payment ->
            friendsList.find { it.contact == payment.id }
        }

        val foundPerson = selectedPayments.find { it.id == myEmail }
        var newId = selectedId
        var expenseRecord = ExpenseRecord()


        // Similar logic for handling single selection
        if (selectedPayments.size == 1 && foundPerson != null &&
            selectedId != R.id.friendOwnedFull && selectedId != R.id.friendPaidSplit
        ) {
            val navOptions = NavOptions.Builder()
                .setPopUpTo(R.id.home_page, inclusive = true)
                .build()

            findNavController().navigate(R.id.home_page, null, navOptions)
        }

        // Logic for handling different payment scenarios
        if (selectedId == R.id.youPaidSplit || selectedId == R.id.youOwnedFull) {
            if (selectedId == R.id.youPaidSplit && selectedFriends.size < 2) {
                newId = R.id.youOwnedFull
            } else if (selectedId == R.id.youOwnedFull && selectedFriends.size > 1) {
                newId = R.id.youPaidSplit
            }

            val payment = selectedPayments.find { it.id != myEmail }
            val calculatedAmount = payment?.let { (it.percentage / 100) * totalAmount } ?: 0f
            expenseRecord = ExpenseRecord(
                paidAmount = totalAmount,
                lentAmount = calculatedAmount,
                date = getCurrentDate()
            )

        } else {
            if (selectedId == R.id.friendOwnedFull && selectedFriends.size > 1) {
                newId = R.id.friendPaidSplit
            } else if (selectedId == R.id.friendPaidSplit && selectedFriends.size < 2) {
                newId = R.id.friendOwnedFull
            }


            val payment = selectedPayments.find { it.id == myEmail }
            val calculatedAmount = payment?.let { (it.percentage / 100) * totalAmount } ?: 0f
            expenseRecord = ExpenseRecord(
                paidAmount = totalAmount,
                lentAmount = 0f,
                borrowedAmount = calculatedAmount,
                date = getCurrentDate()
            )
        }

        mainViewModel.updateFriendExpense(expenseRecord, newId)
    }

//    private fun saveExpenses(friendsList: List<Friend>) {
//        friendsList.forEach { friend ->
//            val payment = payments.find { it.id == friend.contact }
//            payment?.let {
//                val calculatedAmount = (it.percentage / 100) * totalAmount
//                val expenseRecord = ExpenseRecord(
//                    paidAmount = totalAmount,
//                    lentAmount = calculatedAmount,
//                    date = getCurrentDate()
//                )
//                friend.expenseRecords.add(expenseRecord)
//            }
//        }
//        mainViewModel.updateContacts(friendsList)
//    }

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

    private fun getCurrentDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}