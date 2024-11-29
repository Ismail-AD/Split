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
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.appdev.split.Adapters.PaymentDistributeAdapter
import com.appdev.split.Model.Data.ExpenseRecord
import com.appdev.split.Model.Data.Friend
import com.appdev.split.Model.Data.PaymentDistribute
import com.appdev.split.Model.Data.UiState
import com.appdev.split.Model.ViewModel.MainViewModel
import com.appdev.split.R
import com.appdev.split.databinding.FragmentAmounUntEquallyBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
@AndroidEntryPoint
class AmountUnEquallyFragment(friendsList: List<Friend>, totalAmount: Float) : Fragment() {

    private var _binding: FragmentAmounUntEquallyBinding? = null
    val binding get() = _binding!!
    private val totalTarget = totalAmount // Use the passed target amount
    val friends = friendsList
    private val payments = friendsList.map {
        // Create an initial payment record for each friend
        PaymentDistribute(id = it.contact, name =  it.name, 0f) // Start with 0 payment, will be updated by user
    }
    val mainViewModel by viewModels<MainViewModel>()
    lateinit var dialog: Dialog


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAmounUntEquallyBinding.inflate(layoutInflater, container, false)
        dialog = Dialog(requireContext())
        setupRecyclerView()
        updateTotalAmount(0f)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

    private fun setupRecyclerView() {
        binding.memberRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = PaymentDistributeAdapter(payments) { totalAmount ->
                updateTotalAmount(totalAmount)
            }
        }
    }

    private fun updateTotalAmount(totalAmount: Float) {
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
                difference == 0f -> {
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

    private fun saveExpenses(friendsList: List<Friend>) {
        friendsList.forEach { friend ->
            val payment = payments.find { it.id == friend.contact }
            payment?.let {
                val expenseRecord = ExpenseRecord(
                    paidAmount = totalTarget,
                    lentAmount = it.amount,
                    date = getCurrentDate() // Record the date of this transaction
                )
                friend.expenseRecords.add(expenseRecord)
            }
        }

        // Update friends' data with their expense records
        mainViewModel.updateContacts(friendsList)
    }

    private fun getCurrentDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}