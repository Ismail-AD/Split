package com.appdev.split.UI.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.appdev.split.Adapters.PaymentDistributeAdapter
import com.appdev.split.Model.Data.PaymentDistribute
import com.appdev.split.R
import com.appdev.split.databinding.FragmentAmounUntEquallyBinding
import kotlin.math.abs

class AmountUnEquallyFragment : Fragment() {

    private var _binding: FragmentAmounUntEquallyBinding? = null
    val binding get() = _binding!!
    private val totalTarget = 100f
    private val payments = listOf(
        PaymentDistribute("1", "Muhammad Ismail Bin Asim"),
        PaymentDistribute("2", "Mubeen Fivver")
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAmounUntEquallyBinding.inflate(layoutInflater, container, false)
        setupRecyclerView()
        updateTotalAmount(0f)
        return binding.root
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}