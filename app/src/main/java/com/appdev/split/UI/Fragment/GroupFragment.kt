package com.appdev.split.UI.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import androidx.recyclerview.widget.LinearLayoutManager
import com.appdev.split.Adapters.BillAdapter
import com.appdev.split.Adapters.ExpenseAdapter
import com.appdev.split.Model.Data.ExpenseRecord
import com.appdev.split.Model.Data.GroupMetaData
import com.appdev.split.Model.Data.UiState
import com.appdev.split.Model.ViewModel.MainViewModel
import com.appdev.split.R
import com.appdev.split.databinding.FragmentGroupBinding
import kotlinx.coroutines.launch

class GroupFragment : Fragment() {
    private var _binding: FragmentGroupBinding? = null
    private val binding get() = _binding!!
    val mainViewModel by activityViewModels<MainViewModel>()
    lateinit var expenseAdapter: ExpenseAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGroupBinding.inflate(inflater, container, false)
        setupShimmer()
        mainViewModel.updateStateToStable()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.expensesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.addNewGroup.setOnClickListener {
            findNavController().navigate(
                R.id.action_groupFragment_to_addGroupFragment,
                null,
                navOptions {
                    launchSingleTop = true
                }
            )
        }
        mainViewModel.getAllGroups()
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.GroupsState.collect { state ->
                    when (state) {
                        is UiState.Loading -> showShimmer()
                        is UiState.Success -> {
                            hideShimmer()
                            updateRecyclerView(state.data)
                        }

                        is UiState.Error -> {
                            hideShimmer()
                            showError(state.message)
                        }

                        UiState.Stable -> {
                            hideShimmer()
                        }
                    }
                }
            }
        }

    }

    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    private fun updateRecyclerView(groups: List<GroupMetaData>) {
        binding.addNewGroup.visibility = View.VISIBLE
        if (groups.isEmpty()) {
            binding.expensesRecyclerView.visibility = View.GONE
            binding.noData.visibility = View.VISIBLE
        } else {
            binding.noData.visibility = View.GONE
//            binding.overallStatus.visibility = View.VISIBLE
            binding.expensesRecyclerView.visibility = View.VISIBLE
            // Update adapter with the new data
            expenseAdapter = ExpenseAdapter(groups, ::move)
            binding.expensesRecyclerView.adapter = expenseAdapter
        }
    }

    private fun setupShimmer() {
        binding.shimmerViewStub.layoutResource = R.layout.shimmer_group_layout
        binding.shimmerViewStub.inflate()
    }


    private fun showShimmer() {
        binding.shimmerViewContainer.visibility = View.VISIBLE
        binding.shimmerViewContainer.startShimmer()
    }


    private fun hideShimmer() {
        binding.shimmerViewContainer.visibility = View.GONE
        binding.shimmerViewContainer.stopShimmer()
    }


    fun move(groupMetaData: GroupMetaData) {
        val action = GroupFragmentDirections.actionGroupToGroupDetailFragment(groupMetaData)
        findNavController().navigate(action)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}