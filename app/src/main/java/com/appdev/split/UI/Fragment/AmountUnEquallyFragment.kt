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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.appdev.split.Adapters.PaymentDistributeAdapter
import com.appdev.split.Model.Data.ExpenseRecord
import com.appdev.split.Model.Data.FriendContact
import com.appdev.split.Model.Data.FriendExpenseRecord
import com.appdev.split.Model.Data.PaymentDistribute
import com.appdev.split.Model.Data.SplitType
import com.appdev.split.Model.ViewModel.MainViewModel
import com.appdev.split.R
import com.appdev.split.Utils.Utils
import com.appdev.split.databinding.FragmentAmounUntEquallyBinding
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@AndroidEntryPoint
class AmountUnEquallyFragment(
    val friendsList: List<FriendContact>,
    val totalAmount: Double,
    val myUserId: String,
    val currency: String,
    val isGroupData: Boolean
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

        return binding.root
    }

    private fun checkViewModelData() {
        val currentExpense = mainViewModel.getExpenseObject()
        val currentFriendExpense = mainViewModel.getFriendExpenseObject()

        Log.d("CHKMEA","$currentExpense")

        if (isGroupData && currentExpense != null && currentExpense.splits.isNotEmpty() &&
            currentExpense.splitType == SplitType.UNEQUAL.name) {
            Log.d("CHKMEA","WITHIN")

            // Update payments with existing amounts
            currentExpense.splits.forEach { split ->
                payments.find { it.id == split.userId }?.let { payment ->
                    payment.amount = split.amount
                }
            }
            Log.d("CHKMEA","$payments")

            // Update UI with total amount
            val totalAllocated = payments.sumOf { it.amount }
            updateTotalAmount(totalAllocated)

        }
        else if (!isGroupData && currentFriendExpense != null && currentFriendExpense.splits.isNotEmpty() &&
            currentFriendExpense.splitType == SplitType.UNEQUAL.name) {
            Log.d("CHKMEA","WITHIN")

            // Update payments with existing amounts
            currentFriendExpense.splits.forEach { split ->
                payments.find { it.id == split.userId }?.let { payment ->
                    payment.amount = split.amount
                }
            }
            Log.d("CHKMEA","$payments")

            // Update UI with total amount
            val totalAllocated = payments.sumOf { it.amount }
            updateTotalAmount(totalAllocated)

        }
        else {
            updateTotalAmount(0.0)
        }
        setupRecyclerView()
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
        val distributionList = Utils.createUnequalSplitsFromPayments(selectedPayments)

        when {
            isGroupData -> {
                val expenseRecord = ExpenseRecord(
                    totalAmount = totalAmount,
                    splitType = SplitType.UNEQUAL.name,
                    splits = distributionList
                )
                mainViewModel.updateGroupExpense(expenseRecord)
            }

            else -> {
                val friendExpenseRecord = FriendExpenseRecord(
                    totalAmount = totalAmount,
                    splitType = SplitType.UNEQUAL.name,
                    splits = distributionList
                )
                mainViewModel.updateFriendExpense(friendExpenseRecord)
            }
        }
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