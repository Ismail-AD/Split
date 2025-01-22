package com.appdev.split.UI.Fragment

import android.app.Dialog
import android.os.Bundle
import android.util.Log
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
import com.appdev.split.Adapters.PaymentDistributeAdapter
import com.appdev.split.Model.Data.ExpenseRecord
import com.appdev.split.Model.Data.FriendContact
import com.appdev.split.Model.Data.PaymentDistribute
import com.appdev.split.Model.Data.SplitType
import com.appdev.split.Model.Data.UiState
import com.appdev.split.Model.ViewModel.MainViewModel
import com.appdev.split.R
import com.appdev.split.Utils.Utils
import com.appdev.split.databinding.FragmentAmounUntEquallyBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@AndroidEntryPoint
class AmountUnEquallyFragment(
    val friendsList: List<FriendContact>,
    val totalAmount: Double,
    val myUserId: String,
    val currency: String
) : Fragment() {

    private var _binding: FragmentAmounUntEquallyBinding? = null
    val binding get() = _binding!!
    private val totalTarget = totalAmount // Use the passed target amount
    val friends = friendsList
    private val payments = friendsList.map {
        // Create an initial payment record for each friend
        PaymentDistribute(
            id = it.friendId,
            name = it.name,
            0.0, imageUrl = it.profileImageUrl
        ) // Start with 0 payment, will be updated by user
    }
    val mainViewModel by activityViewModels<MainViewModel>()
    lateinit var dialog: Dialog


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAmounUntEquallyBinding.inflate(layoutInflater, container, false)
        dialog = Dialog(requireContext())
        checkViewModelData()
        setupRecyclerView()
        return binding.root
    }

    private fun checkViewModelData() {
        val currentExpense = mainViewModel.getExpenseObject()
        Log.d("CHKME", "${currentExpense}")

        if (currentExpense != null && currentExpense.splits.isNotEmpty() &&
            currentExpense.splitType == SplitType.UNEQUAL) {
            Log.d("CHKME", "in if")

            // Update payments with existing amounts
            currentExpense.splits.forEach { split ->
                payments.find { it.id == split.userId }?.let { payment ->
                    payment.amount = split.amount
                }
            }

            // Update UI with total amount
            val totalAllocated = payments.sumOf { it.amount }
            updateTotalAmount(totalAllocated)

        } else {
            updateTotalAmount(0.0)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.fabAddMembers.setOnClickListener {
            val currentTotalAmount = payments.sumOf { it.amount }
            val selectedMembers = payments.filter { it.amount > 0 }


            when {
                currentTotalAmount < totalAmount -> {
                    Toast.makeText(
                        requireContext(),
                        "Total amount is less than ${String.format("%.2f", totalAmount)}",
                        Toast.LENGTH_LONG
                    ).show()
                }

                selectedMembers.find { it.id == myUserId } == null && selectedMembers.size == 1 -> {
                    Toast.makeText(
                        requireContext(),
                        "You cannot add expense that only involve yourself !",
                        Toast.LENGTH_LONG
                    ).show()
                }

                currentTotalAmount > totalAmount -> {
                    Toast.makeText(
                        requireContext(),
                        "Total amount is greater than ${String.format("%.2f", totalAmount)}",
                        Toast.LENGTH_LONG
                    ).show()
                }

                currentTotalAmount == 0.0 -> {
                    Toast.makeText(
                        requireContext(),
                        "Please split the amount",
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
            adapter = PaymentDistributeAdapter(currency,payments) { totalAmount ->
                updateTotalAmount(totalAmount)
            }
        }
    }

    private fun updateTotalAmount(totalAmount: Double) {
        binding.apply {
            tvTotalAmount.text = "$${totalAmount} of $${totalTarget}"

            val difference = totalTarget - totalAmount
            when {
                difference < 0 -> {
                    // Over the total
                    tvAmountLeft.apply {
                        text = "$${abs(difference)} over"
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.error_red))
                    }
                }

                difference == 0.0 -> {
                    // Exactly matches
                    tvAmountLeft.apply {
                        text = "Perfect split!"
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.green))
                    }
                }

                else -> {
                    // Under the total
                    tvAmountLeft.apply {
                        text = "$${difference} left"
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.gray))
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

    private fun saveExpenses() {
        val selectedPayments = payments.filter { it.amount > 0 }

        // Similar logic as in equally split version for handling single selection


//        if (selectedPayments.size == 1 && foundPerson != null &&
//            selectedId != R.id.friendOwnedFull && selectedId != R.id.friendPaidSplit
//        ) {
//            val navOptions = NavOptions.Builder()
//                .setPopUpTo(R.id.home_page, inclusive = true)
//                .build()
//
//            findNavController().navigate(R.id.home_page, null, navOptions)
//        }

        // Logic for handling different payment scenarios similar to equally split version
//        if (selectedId == R.id.youPaidSplit || selectedId == R.id.youOwnedFull) {
//            if (selectedId == R.id.youPaidSplit && selectedFriends.size < 2) {
//                newId = R.id.youOwnedFull
//            } else if (selectedId == R.id.youOwnedFull && selectedFriends.size > 1) {
//                newId = R.id.youPaidSplit
//            }
//
//            val payment = selectedPayments.find { it.id != myEmail }
//            expenseRecord = ExpenseRecord(
//                paidAmount = totalAmount,
//                lentAmount = payment?.amount ?: 0f
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
//                val payment = selectedPayments.find { it.id == myEmail }
//                expenseRecord = ExpenseRecord(
//                    paidAmount = totalAmount,
//                    lentAmount = 0f,
//                    borrowedAmount = payment?.amount ?: 0f
//                )
//        }
        val distributionList = Utils.createUnequalSplitsFromPayments(selectedPayments)

        var expenseRecord = ExpenseRecord(
            totalAmount = totalAmount, splitType = SplitType.UNEQUAL,
            splits = distributionList
        )
        mainViewModel.updateFriendExpense(expenseRecord)
        findNavController().navigateUp()

    }
//    private fun saveExpenses(friendsList: List<Friend>) {
//        friendsList.forEach { friend ->
//            val payment = payments.find { it.id == friend.contact }
//            payment?.let {
//                val expenseRecord = ExpenseRecord(
//                    paidAmount = totalTarget,
//                    lentAmount = it.amount,
//                    date = getCurrentDate() // Record the date of this transaction
//                )
//                friend.expenseRecords.add(expenseRecord)
//            }
//        }
//
//        // Update friends' data with their expense records
//        mainViewModel.updateContacts(friendsList)
//    }

    private fun getCurrentDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}