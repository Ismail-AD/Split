package com.appdev.split.UI.Fragment

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.appdev.split.Adapters.AllFriendExpenseAdapter
import com.appdev.split.Model.Data.ExpenseRecord
import com.appdev.split.Model.Data.GroupMetaData
import com.appdev.split.Model.Data.UiState
import com.appdev.split.Model.ViewModel.MainViewModel
import com.appdev.split.R
import com.appdev.split.databinding.FragmentGroupDetailBinding
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class GroupDetailFragment : Fragment() {
    private var _binding: FragmentGroupDetailBinding? = null
    private val binding get() = _binding!!
    private val mainViewModel by activityViewModels<MainViewModel>()
    private val args: GroupDetailFragmentArgs by navArgs()
    private lateinit var dialog: Dialog
    private lateinit var adapter: AllFriendExpenseAdapter

    @Inject
    lateinit var firestore: FirebaseFirestore

    @Inject
    lateinit var firebaseAuth: FirebaseAuth


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentGroupDetailBinding.inflate(layoutInflater, container, false)
        setupShimmer()
        mainViewModel.updateToEmpty(ExpenseRecord())
        mainViewModel.updateExpenseCategory("")
        mainViewModel.clearSelectedFriends()
        mainViewModel.updateStateToStable()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val groupData = args.groupMetaData
        Log.d("AMZ", groupData.toString())
        groupData.groupId?.let { mainViewModel.getAllGroupExpenses(groupId = it) }
        setupBasicUI(groupData)
        setupListeners(groupData)
        observeGroupState()
    }

    private fun setupBasicUI(groupData: GroupMetaData) {
        binding.nameOfGroup.text = groupData.title
        if (!groupData.image.isNullOrEmpty()) {
            binding.ImageOfExpense.visibility = View.VISIBLE
            Glide.with(requireContext()).load(groupData.image).placeholder(R.drawable.group)
                .error(R.drawable.group).into(binding.ImageOfExpense)
        }
    }


    private fun setupListeners(groupData: GroupMetaData) {
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.addContact.setOnClickListener {
            val action = GroupDetailFragmentDirections
                .actionGroupDetailFragmentToAddMembersFragment(true, groupData.groupId!!)
            findNavController().navigate(action)
        }

        binding.addExp.setOnClickListener {
            val action = GroupDetailFragmentDirections
                .actionGroupDetailFragmentToAddGrpExpenseFragment(groupData.groupId, null)
            findNavController().navigate(action)
        }

    }

    private fun observeGroupState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.groupExpensesState.collect { state ->
                    when (state) {
                        is UiState.Loading -> showShimmer()
                        is UiState.Success -> {
                            hideShimmer()
                            updateRecyclerView(state.data)
                        }

                        is UiState.Error -> {
                            hideShimmer()
//                            showError(state.message)
                        }

                        UiState.Stable -> {
                            hideShimmer()
                        }
                    }
                }
            }
        }
    }

    private fun updateRecyclerView(expenses: List<ExpenseRecord>) {
        val safeBinding = _binding ?: return

        if (expenses.isEmpty()) {
            safeBinding.nobill.visibility = View.VISIBLE
            safeBinding.recyclerViewGroupExpenses.visibility = View.GONE
        } else {
            safeBinding.nobill.visibility = View.GONE
            safeBinding.recyclerViewGroupExpenses.visibility = View.VISIBLE

            adapter = AllFriendExpenseAdapter(expenses, ::goToDetails)
            safeBinding.recyclerViewGroupExpenses.adapter = adapter
            safeBinding.recyclerViewGroupExpenses.layoutManager =
                LinearLayoutManager(requireContext())
        }
    }

    fun goToDetails(expenseList: ExpenseRecord) {
        val action = GroupDetailFragmentDirections.actionGroupDetailFragmentToBillDetails(
            expenseList, null, args.groupMetaData.groupId
        )
        findNavController().navigate(action)
//        val action = GroupDetailFragmentDirections.actionGroupDetailFragmentToAddGrpExpenseFragment(
//            args.groupMetaData.groupId,expenseList
//        )
//        findNavController().navigate(action)
    }

    private fun setupShimmer() {
        // Set the layout for the ViewStub
        binding.shimmerViewFriendExpenses.layoutResource = R.layout.group_all_expenses_shimmer
        binding.shimmerViewFriendExpenses.inflate()

        binding.shimmerViewContainer.startShimmer()
    }


    private fun showShimmer() {
        binding.shimmerViewContainer.visibility = View.VISIBLE
        binding.shimmerViewContainer.startShimmer()

        // Hide actual content while shimmer is showing
        binding.topBar.visibility = View.GONE
        binding.BottomAllContent.visibility = View.GONE
    }

    private fun hideShimmer() {
        binding.shimmerViewContainer.stopShimmer()
        binding.shimmerViewContainer.visibility = View.GONE
        // Hide actual content while shimmer is showing
        binding.topBar.visibility = View.VISIBLE
        binding.BottomAllContent.visibility = View.VISIBLE

    }

    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }


    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

}