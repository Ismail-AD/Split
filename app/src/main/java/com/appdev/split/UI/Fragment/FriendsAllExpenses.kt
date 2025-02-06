package com.appdev.split.UI.Fragment

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.appdev.split.Adapters.AllFriendExpenseAdapter
import com.appdev.split.Model.Data.ExpenseRecord
import com.appdev.split.Model.Data.FriendContact
import com.appdev.split.Model.Data.UiState
import com.appdev.split.Model.ViewModel.MainViewModel
import com.appdev.split.R
import com.appdev.split.databinding.FragmentFriendsAllExpensesBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FriendsAllExpenses : Fragment() {

    private var _binding: FragmentFriendsAllExpensesBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: AllFriendExpenseAdapter
    val mainViewModel by activityViewModels<MainViewModel>()
    private val args: FriendsAllExpensesArgs by navArgs()
    lateinit var dialog: Dialog
    private var friendContact: FriendContact? = null
    @Inject
    lateinit var firestore: FirebaseFirestore

    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFriendsAllExpensesBinding.inflate(layoutInflater, container, false)
        setupShimmer()
        mainViewModel.updateToEmpty(ExpenseRecord())
        mainViewModel.updateExpenseCategory("")
        mainViewModel.updateStateToStable()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog = Dialog(requireContext())
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }

        if (args.bilList.isNotEmpty()) {
            firebaseAuth.currentUser?.uid?.let { myId ->
                val initialBillList = args.bilList.toList().toMutableList() // Make mutable

                updateRecyclerView(initialBillList)

                // Use Firestore instead of Realtime Database
                val expensesRef = firestore.collection("expenses")
                    .document(myId)
                    .collection(args.friendUserId)

                // Create a snapshot listener
                expensesRef.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("CHKSS", "Listen failed.", error)
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val newExpenses = mutableListOf<ExpenseRecord>()

                        for (doc in snapshot.documents) {
                            val expenseRecord = doc.toObject(ExpenseRecord::class.java)
                            if (expenseRecord != null) {
                                newExpenses.add(expenseRecord)
                            } else {
                                Log.e("CHKSS", "Failed to parse ExpenseRecord: ${doc.data}")
                            }
                        }

                        // Compare with the existing list
                        if (initialBillList != newExpenses) {
                            initialBillList.clear()
                            initialBillList.addAll(newExpenses)
                            updateRecyclerView(initialBillList)
                        }
                    }
                }
            }
        }
        mainViewModel.getFriendNameById(args.friendUserId)
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.FriendState.collect { state ->
                    when (state) {
                        is UiState.Loading -> showShimmer()
                        is UiState.Success -> {
                            hideShimmer()
                            binding.contact.text = state.data.name
                            friendContact = state.data
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

        binding.addExp.setOnClickListener {
            val action =
                FriendsAllExpensesDirections.actionFriendsAllExpensesToPersonalExpenseFragment(null, friendContact)
            findNavController().navigate(action)
        }
    }

    private fun setupShimmer() {
        // Set the layout for the ViewStub
        binding.shimmerViewFriendExpenses.layoutResource = R.layout.shimmer_friend_all_expense
        binding.shimmerViewFriendExpenses.inflate()

        binding.shimmerViewContainer.startShimmer()
    }



    private fun showShimmer() {
        binding.shimmerViewContainer.visibility = View.VISIBLE
        binding.shimmerViewContainer.startShimmer()

        // Hide actual content while shimmer is showing
        binding.topBar.visibility = View.GONE
        binding.ImageOfExpense.visibility = View.GONE
        binding.BottomAllContent.visibility = View.GONE
    }

    private fun hideShimmer() {
        binding.shimmerViewContainer.stopShimmer()
        binding.shimmerViewContainer.visibility = View.GONE
        // Hide actual content while shimmer is showing
        binding.topBar.visibility = View.VISIBLE
        binding.ImageOfExpense.visibility = View.VISIBLE
        binding.BottomAllContent.visibility = View.VISIBLE

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

    private fun updateRecyclerView(expenses: List<ExpenseRecord>) {
        // Check if binding is null before accessing
        val safeBinding = _binding ?: return

        if (expenses.isEmpty()) {
            safeBinding.nobill.visibility = View.VISIBLE
            safeBinding.recyclerViewTransactionItems.visibility = View.GONE
        } else {
            safeBinding.nobill.visibility = View.GONE
            safeBinding.recyclerViewTransactionItems.visibility = View.VISIBLE

            adapter = AllFriendExpenseAdapter(expenses, ::goToDetails)
            safeBinding.recyclerViewTransactionItems.adapter = adapter
            safeBinding.recyclerViewTransactionItems.layoutManager =
                LinearLayoutManager(requireContext())
        }
    }

    fun goToDetails(expenseList: ExpenseRecord) {
        Log.d("CHKIAMG", "I am going in")
        friendContact?.let {
            val action = FriendsAllExpensesDirections.actionFriendsAllExpensesToBillDetails(
                expenseList, it,null
            )
            findNavController().navigate(action)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }
}