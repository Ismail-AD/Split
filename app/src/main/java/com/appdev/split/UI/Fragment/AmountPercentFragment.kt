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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.appdev.split.Adapters.PercentageDistributeAdapter
import com.appdev.split.Model.Data.ExpenseRecord
import com.appdev.split.Model.Data.FriendContact
import com.appdev.split.Model.Data.Percentage
import com.appdev.split.Model.Data.SplitType
import com.appdev.split.Model.Data.UiState
import com.appdev.split.Model.ViewModel.MainViewModel
import com.appdev.split.R
import com.appdev.split.Utils.Utils
import com.appdev.split.databinding.FragmentAmountPercentBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class AmountPercentFragment(
    val friendsList: List<FriendContact>,
    val totalAmount: Double,
    val myUserId: String,
    val currency: String
) : Fragment() {

    private var _binding: FragmentAmountPercentBinding? = null
    private val binding get() = _binding!!
    val friends = friendsList
    private val totalPercentage = 100f
    private val payments = friendsList.map { friend ->
        Percentage(friend.contact, friend.name, imageUrl = friend.profileImageUrl)
    }
    private val mainViewModel by activityViewModels<MainViewModel>()
    private lateinit var dialog: Dialog

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAmountPercentBinding.inflate(layoutInflater, container, false)
        dialog = Dialog(requireContext())
        checkViewModelData()
        setupRecyclerView()
        return binding.root
    }
    private fun checkViewModelData() {
        val currentExpense = mainViewModel.getExpenseObject()

        if (currentExpense != null && currentExpense.splits.isNotEmpty() &&
            currentExpense.splitType == SplitType.PERCENTAGE) {
            // Update payments with existing percentages
            currentExpense.splits.forEach { split ->
                payments.find { it.id == split.userId }?.let { payment ->
                    // Convert amount back to percentage: (amount / total) * 100
                    payment.percentage = split.percentage
                }
            }

            // Update UI with total percentage
            val totalPercent = payments.sumOf { it.percentage }
            updateProgress(totalPercent)

        } else {
            updateProgress(0.0)
        }
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

                selectedMembers.find { it.id == myUserId } == null &&
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
                    saveExpenses()
                }
            }
        }
    }

    private fun setupRecyclerView() {
        binding.memberRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = PercentageDistributeAdapter(currency,payments, totalAmount) { percentage ->
                updateProgress(percentage)
            }
        }
    }

    private fun updateProgress(totalPercentageAllocated: Double) {
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
                totalPercentageAllocated == 100.0 -> {
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

    private fun saveExpenses() {
        val selectedPayments = payments.filter { it.percentage > 0 }

        // Similar logic for handling single selection
//        if (selectedPayments.size == 1 && foundPerson != null &&
//            selectedId != R.id.friendOwnedFull && selectedId != R.id.friendPaidSplit
//        ) {
//            val navOptions = NavOptions.Builder()
//                .setPopUpTo(R.id.home_page, inclusive = true)
//                .build()
//
//            findNavController().navigate(R.id.home_page, null, navOptions)
//        }
//
//        // Logic for handling different payment scenarios
//        if (selectedId == R.id.youPaidSplit || selectedId == R.id.youOwnedFull) {
//            if (selectedId == R.id.youPaidSplit && selectedFriends.size < 2) {
//                newId = R.id.youOwnedFull
//            } else if (selectedId == R.id.youOwnedFull && selectedFriends.size > 1) {
//                newId = R.id.youPaidSplit
//            }
//
//            val payment = selectedPayments.find { it.id != myEmail }
//            val calculatedAmount = payment?.let { (it.percentage / 100) * totalAmount } ?: 0f
//            expenseRecord = ExpenseRecord(
//                paidAmount = totalAmount,
//                lentAmount = calculatedAmount
//            )
//
//        } else {
//            if (selectedId == R.id.friendOwnedFull && selectedFriends.size > 1) {
//                newId = R.id.friendPaidSplit
//            } else if (selectedId == R.id.friendPaidSplit && selectedFriends.size < 2) {
//                newId = R.id.friendOwnedFull
//            }
//
//
//            val payment = selectedPayments.find { it.id == myEmail }
//            val calculatedAmount = payment?.let { (it.percentage / 100) * totalAmount } ?: 0f
//            expenseRecord = ExpenseRecord(
//                paidAmount = totalAmount,
//                lentAmount = 0f,
//                borrowedAmount = calculatedAmount
//            )
//        }
        val distributionList =
            Utils.createPercentageSplitsFromPayments(selectedPayments, totalAmount)
        var expenseRecord = ExpenseRecord(
            totalAmount = totalAmount,
            splitType = SplitType.PERCENTAGE,
            splits = distributionList
        )

        mainViewModel.updateFriendExpense(expenseRecord)
        findNavController().navigateUp()
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