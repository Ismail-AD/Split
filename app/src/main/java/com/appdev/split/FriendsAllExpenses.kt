package com.appdev.split

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.appdev.split.Adapters.AllFriendExpenseAdapter
import com.appdev.split.Adapters.BillAdapter
import com.appdev.split.Model.Data.ExpenseRecord
import com.appdev.split.Model.ViewModel.MainViewModel
import com.appdev.split.UI.Activity.EntryActivity
import com.appdev.split.UI.Fragment.BillDetailsArgs
import com.appdev.split.UI.Fragment.HomeFragmentDirections
import com.appdev.split.databinding.FragmentFriendsAllExpensesBinding
import com.appdev.split.databinding.FragmentHomeBinding
import com.appdev.split.databinding.FragmentPersonalExpenseBinding

class FriendsAllExpenses : Fragment() {

    private var _binding: FragmentFriendsAllExpensesBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: AllFriendExpenseAdapter
    val mainViewModel by activityViewModels<MainViewModel>()
    private val args: FriendsAllExpensesArgs by navArgs()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFriendsAllExpensesBinding.inflate(layoutInflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as? EntryActivity)?.hideBottomBar()
        val billList = args.bilList.toList()
        updateRecyclerView(billList)
    }

    private fun updateRecyclerView(expenses: List<ExpenseRecord>) {
        if (expenses.isEmpty()) {
            binding.nobill.visibility = View.VISIBLE
            binding.recyclerViewTransactionItems.visibility = View.GONE
        } else {
            binding.nobill.visibility = View.GONE
            binding.recyclerViewTransactionItems.visibility = View.VISIBLE

            adapter = AllFriendExpenseAdapter(expenses, ::goToDetails)
            binding.recyclerViewTransactionItems.adapter = adapter
            binding.recyclerViewTransactionItems.layoutManager = LinearLayoutManager(requireContext())
        }
    }

    fun goToDetails(expenseList: ExpenseRecord) {
        val action = FriendsAllExpensesDirections.actionFriendsAllExpensesToBillDetails(expenseList)
        findNavController().navigate(action)
    }
}