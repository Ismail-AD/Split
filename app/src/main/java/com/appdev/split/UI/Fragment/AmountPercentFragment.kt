package com.appdev.split.UI.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.appdev.split.Adapters.PercentageDistributeAdapter
import com.appdev.split.Model.Data.Friend
import com.appdev.split.Model.Data.Percentage
import com.appdev.split.R
import com.appdev.split.databinding.FragmentAmountPercentBinding
import kotlin.math.abs

class AmountPercentFragment(friendsList: List<Friend>, totalAmount: Float) : Fragment() {

    private var _binding: FragmentAmountPercentBinding? = null
    private val binding get() = _binding!!
    private val totalAmount = 100f  // Static amount to be split
    private val totalPercentage = 100f
    private val payments = listOf(
        Percentage("1", "Muhammad Ismail Bin Asim"),
        Percentage("2", "Mubeen Fivver")
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAmountPercentBinding.inflate(layoutInflater, container, false)
        setupRecyclerView()
        updateProgress(0f)
        return binding.root
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}