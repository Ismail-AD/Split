package com.appdev.split

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
import com.appdev.split.Model.Data.UiState
import com.appdev.split.Model.ViewModel.MainViewModel
import com.appdev.split.UI.Activity.EntryActivity
import com.appdev.split.Utils.Utils
import com.appdev.split.databinding.FragmentFriendsAllExpensesBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
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

    @Inject
    lateinit var firebaseDatabase: FirebaseDatabase

    @Inject
    lateinit var firebaseAuth: FirebaseAuth
    var eventListener: ValueEventListener? = null
    lateinit var expenseRef: DatabaseReference
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFriendsAllExpensesBinding.inflate(layoutInflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog = Dialog(requireContext())
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
        Log.d("CHKIAMG", "I am at list")
        mainViewModel.updateStateToStable()
        (activity as? EntryActivity)?.hideBottomBar()
        var billList = args.bilList.toList()
        updateRecyclerView(billList)

        firebaseAuth.currentUser?.email?.let { mail ->
            val sanitizedMyEmail = Utils.sanitizeEmailForFirebase(mail)
            val friendMail = Utils.sanitizeEmailForFirebase(args.nameOfFriend)
            expenseRef = firebaseDatabase.reference
                .child("expenses")
                .child(sanitizedMyEmail).child(friendMail)

            // Create a value event listener
            eventListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // List to store all expenses
                    val newExpenses = mutableListOf<ExpenseRecord>()

                    snapshot.children.forEach {
                        Log.d("CHKSS", it.toString()) // Log the raw snapshot to debug

                        // Attempt to map the child to an ExpenseRecord
                        val expenseRecord = it.getValue(ExpenseRecord::class.java)
                        if (expenseRecord != null) {
                            newExpenses.add(expenseRecord)
                        } else {
                            Log.e("CHKSS", "Failed to parse ExpenseRecord: ${it.value}")
                        }
                    }


                    // Compare with the existing list
                    if (billList != newExpenses) {
                        billList = newExpenses
                        updateRecyclerView(billList)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle any potential error here
                }
            }

            eventListener?.let {
                expenseRef.addValueEventListener(it)
            }
        }

        mainViewModel.getFriendNameById(args.nameOfFriend)
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.FriendState.collect { state ->
                    when (state) {
                        is UiState.Loading -> showLoadingIndicator()
                        is UiState.Success -> {
                            hideLoadingIndicator()
                            binding.contact.text = state.data.name
                        }

                        is UiState.Error -> showError(state.message)
                        UiState.Stable -> {

                        }
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

    private fun updateRecyclerView(expenses: List<ExpenseRecord>) {
        if (expenses.isEmpty()) {
            binding.nobill.visibility = View.VISIBLE
            binding.recyclerViewTransactionItems.visibility = View.GONE
        } else {
            binding.nobill.visibility = View.GONE
            binding.recyclerViewTransactionItems.visibility = View.VISIBLE

            adapter = AllFriendExpenseAdapter(expenses, ::goToDetails)
            binding.recyclerViewTransactionItems.adapter = adapter
            binding.recyclerViewTransactionItems.layoutManager =
                LinearLayoutManager(requireContext())
        }
    }

    fun goToDetails(expenseList: ExpenseRecord) {
        Log.d("CHKIAMG", "I am going in")
        val action = FriendsAllExpensesDirections.actionFriendsAllExpensesToBillDetails(
            expenseList, args.nameOfFriend,
            binding.contact.text.toString()
        )
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        eventListener?.let {
            expenseRef.removeEventListener(it)
        }
        _binding = null
    }
}