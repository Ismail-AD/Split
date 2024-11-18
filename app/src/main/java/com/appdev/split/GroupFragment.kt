package com.appdev.split

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.appdev.split.Adapters.ExpenseAdapter
import com.appdev.split.Model.Data.GroupRecord
import com.appdev.split.databinding.FragmentGroupBinding


class GroupFragment : Fragment() {
    private var _binding: FragmentGroupBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGroupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.expensesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        val expenseList = listOf(
            GroupRecord(title = "Dobe", subTitle = "no expenses"),
            GroupRecord(
                title = "Hello",
                subTitle = "you are owed $611.50\nMubeen F. owes you $611.50"
            ),
            GroupRecord(title = "Jsnsnsn...", subTitle = "no expenses"),
            GroupRecord(title = "Non-group expenses", subTitle = "no expenses")
        )
        binding.expensesRecyclerView.adapter = ExpenseAdapter(expenseList,::move)

        binding.add.setOnClickListener {
           findNavController().navigate(R.id.action_groupFragment_to_addGroupFragment)
        }
    }

    fun move(){
        findNavController().navigate(R.id.action_group_to_groupDetailFragment)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}