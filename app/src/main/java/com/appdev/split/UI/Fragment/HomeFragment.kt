package com.appdev.split.UI.Fragment

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.appdev.split.Adapters.BillAdapter
import com.appdev.split.Model.Data.Bill
import com.appdev.split.Model.Data.ExpenseRecord
import com.appdev.split.Model.Data.TransactionItem
import com.appdev.split.Model.Data.UiState
import com.appdev.split.Model.ViewModel.MainViewModel
import com.appdev.split.R
import com.appdev.split.databinding.FragmentHomeBinding
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch


@AndroidEntryPoint
class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: BillAdapter
    val mainViewModel by activityViewModels<MainViewModel>()
    var expenses: Map<String, List<ExpenseRecord>> = mapOf()

    lateinit var dialog: Dialog
    private var isExpanded = false

    private val fromBottomFabAnim: Animation by lazy {
        AnimationUtils.loadAnimation(requireContext(), R.anim.from_bottom_fab)
    }
    private val toBottomFabAnim: Animation by lazy {
        AnimationUtils.loadAnimation(requireContext(), R.anim.to_bottom_fab)
    }
    private val rotateClockWiseFabAnim: Animation by lazy {
        AnimationUtils.loadAnimation(requireContext(), R.anim.rotate_clock_wise)
    }
    private val rotateAntiClockWiseFabAnim: Animation by lazy {
        AnimationUtils.loadAnimation(requireContext(), R.anim.rotate_anti_clock_wise)
    }
    private val fromBottomBgAnim: Animation by lazy {
        AnimationUtils.loadAnimation(requireContext(), R.anim.from_bottom_anim)
    }
    private val toBottomBgAnim: Animation by lazy {
        AnimationUtils.loadAnimation(requireContext(), R.anim.to_bottom_anim)
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog = Dialog(requireContext())
        binding.mainFab.setOnClickListener {
            if (isExpanded) shrinkFab() else expandFab()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.allExpensesState.collect { state ->
                    when (state) {
                        is UiState.Loading -> showLoadingIndicator()
                        is UiState.Success -> {
                            hideLoadingIndicator()
                            expenses = state.data
                            expenses.entries.groupBy { it.key }
                                .mapValues { entry ->
                                    entry.value.lastOrNull()
                                }
                                .values
                                .filterNotNull() // Remove nulls, in case there are contacts with no expenses
                                .toList()
                            updateRecyclerView(expenses)
                        }

                        is UiState.Error -> showError(state.message)
                    }
                }
            }
        }

        binding.contactFab.setOnClickListener { onContactClicked() }
        binding.expenseFab.setOnClickListener { onExpenseClicked() }
        val mail = FirebaseAuth.getInstance().currentUser?.email

        mail?.let { mainViewModel.fetchUserData(it) }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.userData.collect { user ->
                    user?.let {
                        binding.name.text = "Hello, ${it.name} ✌\uFE0F"
                        binding.progres.visibility = View.GONE
                        binding.name.visibility = View.VISIBLE
                    }
                }
            }
        }

    }

    private fun updateRecyclerView(expenses: Map<String, List<ExpenseRecord>>) {


        if (expenses.isEmpty()) {
            binding.noBill.visibility = View.VISIBLE
            binding.recyclerViewRecentBills.visibility = View.GONE
        } else {
            binding.noBill.visibility = View.GONE
            binding.recyclerViewRecentBills.visibility = View.VISIBLE
            // Update adapter with the new data
            adapter = BillAdapter(expenses, ::goToDetails)
            binding.recyclerViewRecentBills.adapter = adapter
            binding.recyclerViewRecentBills.layoutManager = LinearLayoutManager(requireContext())
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

    private fun onContactClicked() {
        Log.d("CHKERR",mainViewModel.userData.value.toString() + "At home add member")

        val action = HomeFragmentDirections.actionHomePageToAddMembersFragment(false)
        findNavController().navigate(action)
    }

    private fun onExpenseClicked() {
        val action = HomeFragmentDirections.actionHomePageToPersonalExpenseFragment()
        findNavController().navigate(action)
    }

    private fun shrinkFab() {
        binding.mainFab.startAnimation(rotateAntiClockWiseFabAnim)
        binding.contactFab.startAnimation(toBottomFabAnim)
        binding.expenseFab.startAnimation(toBottomFabAnim)
        isExpanded = false
    }

    private fun expandFab() {
        binding.mainFab.startAnimation(rotateClockWiseFabAnim)
        binding.contactFab.startAnimation(fromBottomFabAnim)
        binding.expenseFab.startAnimation(fromBottomFabAnim)
        isExpanded = true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun goToDetails(expenseList: List<ExpenseRecord> ,email:String) {
        val action = HomeFragmentDirections.actionHomePageToFriendsAllExpenses(expenseList.toTypedArray(),email)
        findNavController().navigate(action)
    }
}