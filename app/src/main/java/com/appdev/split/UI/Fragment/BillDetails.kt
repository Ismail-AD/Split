package com.appdev.split.UI.Fragment

import android.app.Dialog
import android.os.Bundle
import android.util.Log
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
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.appdev.split.Adapters.TransactionItemAdapter
import com.appdev.split.Model.Data.Bill
import com.appdev.split.Model.Data.ExpenseRecord
import com.appdev.split.Model.Data.Friend
import com.appdev.split.Model.Data.FriendContact
import com.appdev.split.Model.Data.UiState
import com.appdev.split.Model.ViewModel.MainViewModel
import com.appdev.split.R
import com.appdev.split.Utils.Utils
import com.appdev.split.databinding.FragmentBillDetailsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BillDetails : Fragment() {

    private var _binding: FragmentBillDetailsBinding? = null
    private val binding get() = _binding!!
    lateinit var dialog: Dialog
    private val args: BillDetailsArgs by navArgs()
    val mainViewModel by activityViewModels<MainViewModel>()
    var friendContact: Friend? = null
    var bill: ExpenseRecord = ExpenseRecord()

    @Inject
    lateinit var firestore: FirebaseFirestore

    @Inject
    lateinit var firebaseAuth: FirebaseAuth
    var eventListener: ValueEventListener? = null
    lateinit var expenseRef: DatabaseReference
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentBillDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainViewModel.updateStateToStable()

        dialog = Dialog(requireContext())
        binding.backBtn.setOnClickListener {
            findNavController().navigateUp()
        }

        bill = args.billData
        friendContact = Friend(contact = args.friendContact, name = args.friendName)

        firebaseAuth.currentUser?.email?.let { mail ->
            val sanitizedMyEmail = Utils.sanitizeEmailForFirebase(mail)
            val friendMail = Utils.sanitizeEmailForFirebase(args.friendContact)

            // Use Firestore instead of Realtime Database
            val expenseDocRef = firestore.collection("expenses")
                .document(sanitizedMyEmail)
                .collection(friendMail)
                .document(bill.expenseId)

            // Create a snapshot listener for the specific expense
            expenseDocRef.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("CHKSS", "Listen failed.", error)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val expenseRecord = snapshot.toObject(ExpenseRecord::class.java)

                    if (expenseRecord != null && expenseRecord != bill) {
                        bill = expenseRecord
                        updateData()
                    }
                }
            }
        }
        updateData()

        binding.edit.setOnClickListener {
            val action = BillDetailsDirections.actionBillDetailsToPersonalExpenseFragment(
                args.billData,
                args.friendContact,
                args.friendName
            )
            findNavController().navigate(action)

        }
        binding.delete.setOnClickListener {
            observeOperationState()
            mainViewModel.deleteFriendExpenseDetail(bill.expenseId, args.friendContact)
        }



        binding.title.text = bill.title
        binding.date.text = "Added by you on " + Utils.formatDate(bill.date)
        binding.totalAmount.text = Utils.extractCurrencyCode(bill.currency) + bill.amount.toString()



        binding.description.text = bill.description


    }

    fun updateData() {
        if (bill.lentAmount > 0f) {
            val youPaid = bill.paidAmount - bill.lentAmount
            val amountText = if (youPaid == 0f) {
                bill.paidAmount.toString()
            } else {
                youPaid.toString()
            }
            if (bill.paidAmount == bill.lentAmount) {
                binding.OwnerpaidStatement.text =
                    "You paid " + Utils.extractCurrencyCode(bill.currency) + amountText
            } else {
                binding.OwnerpaidStatement.text =
                    "You owes " + Utils.extractCurrencyCode(bill.currency) + amountText
            }
            friendContact?.let { friend ->
                binding.friendSplitStatement.text =
                    friend.name + " owes " + Utils.extractCurrencyCode(bill.currency) + bill.lentAmount.toString()
            }


        } else if (bill.borrowedAmount > 0f) {
            val youPaid = bill.paidAmount - bill.borrowedAmount
            friendContact?.let { friend ->
                if (bill.paidAmount == bill.borrowedAmount) {
                    binding.friendSplitStatement.text = friend.name + " paid " + youPaid.toString()
                } else {
                    binding.OwnerpaidStatement.text = friend.name + " owes " + youPaid.toString()
                }
            }
            binding.OwnerpaidStatement.text =
                "You owes " + Utils.extractCurrencyCode(bill.currency) + bill.borrowedAmount.toString()

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

    override fun onDestroy() {
        super.onDestroy()
        eventListener?.let {
            expenseRef.removeEventListener(it)
        }
    }

    private fun observeOperationState() {
        viewLifecycleOwner.lifecycleScope.launch {
            mainViewModel.operationState.collect { state ->
                when (state) {
                    is UiState.Error -> showError(state.message)
                    UiState.Loading -> showLoadingIndicator()
                    is UiState.Success -> {
                        hideLoadingIndicator()
                        findNavController().navigateUp()
                    }

                    UiState.Stable -> {

                    }
                }
            }
        }
    }

}