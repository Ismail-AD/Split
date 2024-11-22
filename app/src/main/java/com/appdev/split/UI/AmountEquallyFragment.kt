package com.appdev.split.UI

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.appdev.split.Adapters.MemberAdapter
import com.appdev.split.Adapters.SplitMembersAdapter
import com.appdev.split.Model.Data.Member
import com.appdev.split.R
import com.appdev.split.databinding.FragmentAmountEquallyBinding

class AmountEquallyFragment : Fragment() {
    private var _binding: FragmentAmountEquallyBinding? = null
    val binding get() = _binding!!
    private lateinit var myadapter: SplitMembersAdapter
    private val totalAmount = 50.00
    private val persons = listOf(
        Member("1", "Muhammad Ismail Bin Asim"),
        Member("2", "Mubeen Fivver")
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAmountEquallyBinding.inflate(layoutInflater, container, false)
        setupRecyclerView()
        setupSelectAll()
        return binding.root
    }

    private fun setupRecyclerView() {
        myadapter = SplitMembersAdapter { selectedPersons ->
            updateUI(selectedPersons.filter { it.isSelected })

            // Update "All" checkbox - should be checked only if ALL members are selected
            binding.selectAllCheckBox.setOnCheckedChangeListener(null)
            binding.selectAllCheckBox.isChecked = selectedPersons.all { it.isSelected }
            setupSelectAll() // Reattach the listener
        }

        binding.memberRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = myadapter
        }

        // Initialize with all members selected
        val initialMembers = persons.map { it.copy(isSelected = true) }
        myadapter.updatePersons(initialMembers)
        updateUI(initialMembers)
    }
    private fun setupSelectAll() {
        binding.selectAllCheckBox.setOnCheckedChangeListener { _, isChecked ->
            myadapter.selectAll(isChecked)
        }
    }

    private fun updateUI(selectedPersons: List<Member>) {
        val selectedCount = selectedPersons.size

        when {
            selectedCount == 0 -> {
                binding.pricePerPerson.text = "Select at least one person"
                binding.countPerson.visibility = View.GONE
            }
            else -> {
                val amountPerPerson = totalAmount / selectedCount
                binding.countPerson.visibility = View.VISIBLE
                binding.pricePerPerson.text = "$${String.format("%.2f", amountPerPerson)}/person"
                binding.countPerson.text = "($selectedCount people)"
            }
        }
    }
}