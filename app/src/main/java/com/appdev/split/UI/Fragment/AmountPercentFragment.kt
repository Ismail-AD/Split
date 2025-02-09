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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.appdev.split.Adapters.PercentageDistributeAdapter
import com.appdev.split.Model.Data.ExpenseRecord
import com.appdev.split.Model.Data.FriendContact
import com.appdev.split.Model.Data.FriendExpenseRecord
import com.appdev.split.Model.Data.Percentage
import com.appdev.split.Model.Data.SplitType
import com.appdev.split.Model.ViewModel.MainViewModel
import com.appdev.split.R
import com.appdev.split.Utils.Utils
import com.appdev.split.databinding.FragmentAmountPercentBinding
import kotlin.math.abs

class AmountPercentFragment(
    val friendsList: List<FriendContact>,
    val totalAmount: Double,
    val myUserId: String,
    val currency: String,
    val isGroupData: Boolean
) : Fragment() {

    private var _binding: FragmentAmountPercentBinding? = null
    private val binding get() = _binding!!
    val friends = friendsList
    private val totalPercentage = 100f
    private val payments = friendsList.map { friend ->
        Percentage(id = friend.friendId, friend.name, imageUrl = friend.profileImageUrl)
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

        return binding.root
    }

    private fun checkViewModelData() {
        val currentExpense = mainViewModel.getExpenseObject()
        val currentFriendExpense = mainViewModel.getFriendExpenseObject()


        if (isGroupData && currentExpense != null && currentExpense.splits.isNotEmpty() &&
            currentExpense.splitType == SplitType.PERCENTAGE.name
        ) {
            // Update payments with existing percentages
            currentExpense.splits.forEach { split ->
                payments.find { it.id == split.userId }?.let { payment ->
                    // Convert amount back to percentage: (amount / total) * 100
                    payment.percentage = split.percentage
                    payment.amount = split.amount
                }
            }

            // Update UI with total percentage
            val totalPercent = payments.sumOf { it.percentage }
            updateProgress(totalPercent)

        } else if (!isGroupData && currentFriendExpense != null && currentFriendExpense.splits.isNotEmpty() &&
            currentFriendExpense.splitType == SplitType.PERCENTAGE.name
        ) {
            // Update payments with existing percentages
            currentFriendExpense.splits.forEach { split ->
                payments.find { it.id == split.userId }?.let { payment ->
                    // Convert amount back to percentage: (amount / total) * 100
                    payment.percentage = split.percentage
                    payment.amount = split.amount
                }
            }

            // Update UI with total percentage
            val totalPercent = payments.sumOf { it.percentage }
            updateProgress(totalPercent)

        } else {
            updateProgress(0.0)
        }
        setupRecyclerView()
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
            adapter = PercentageDistributeAdapter(currency, payments, totalAmount) { percentage ->
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

        val distributionList =
            Utils.createPercentageSplitsFromPayments(selectedPayments, totalAmount)


        when {
            isGroupData -> {
                val expenseRecord = ExpenseRecord(
                    totalAmount = totalAmount,
                    splitType = SplitType.PERCENTAGE.name,
                    splits = distributionList
                )
                mainViewModel.updateGroupExpense(expenseRecord)
            }

            else -> {
                val friendExpenseRecord = FriendExpenseRecord(
                    totalAmount = totalAmount,
                    splitType = SplitType.PERCENTAGE.name,
                    splits = distributionList
                )
                mainViewModel.updateFriendExpense(friendExpenseRecord)
            }
        }
        findNavController().navigateUp()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}