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
import com.appdev.split.Adapters.SplitDetailsAdapter
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
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BillDetails : Fragment() {

    private var _binding: FragmentBillDetailsBinding? = null
    private val binding get() = _binding!!
    private lateinit var dialog: Dialog
    private val args: BillDetailsArgs by navArgs()
    private val mainViewModel by activityViewModels<MainViewModel>()
    private lateinit var splitDetailsAdapter: SplitDetailsAdapter
    private var expenseRecord: ExpenseRecord = ExpenseRecord()
    private var expensesListener: ListenerRegistration? = null

    @Inject
    lateinit var firestore: FirebaseFirestore

    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBillDetailsBinding.inflate(inflater, container, false)
        mainViewModel.updateFriendExpense(ExpenseRecord())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainViewModel.updateStateToStable()

        setupDialog()
        setupRecyclerView()
        setupClickListeners()
        setupSingleExpenseListener(args.billData.id, args.friendData.friendId)
        updateUIWithExpenseRecord(args.billData)
    }

    private fun setupDialog() {
        dialog = Dialog(requireContext())
    }

    private fun setupRecyclerView() {
        splitDetailsAdapter = SplitDetailsAdapter()
        binding.splitDetailsRecyclerView.apply {
            adapter = splitDetailsAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupClickListeners() {
        binding.apply {
            backBtn.setOnClickListener {
                findNavController().navigateUp()
            }

            edit.setOnClickListener {
                val action = BillDetailsDirections.actionBillDetailsToPersonalExpenseFragment(
                    expenseRecord,
                    args.friendData
                )
                findNavController().navigate(action)
            }

            delete.setOnClickListener {
                observeOperationState()
                mainViewModel.deleteFriendExpenseDetail(expenseRecord.id, args.friendData.friendId)
            }
        }
    }

    private fun setupSingleExpenseListener(expenseId: String, friendId: String) {
        val currentUser = firebaseAuth.currentUser ?: return
        val userId = currentUser.uid

        expensesListener?.remove()
        expensesListener = firestore.collection("expenses")
            .document(userId)
            .collection(friendId)
            .document(expenseId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Handle error
                    return@addSnapshotListener
                }

                snapshot?.let { documentSnapshot ->
                    val updatedRecord = documentSnapshot.toObject(ExpenseRecord::class.java)
                    Log.d("CHKUPA", "UPDATED ONE: ${updatedRecord}")
                    Log.d("CHKUPA", "RECEIVED ONE: ${expenseRecord}")
                    if (expenseRecord != updatedRecord) {
                        updatedRecord?.let { record ->
                            updateUIWithExpenseRecord(record)
                        }
                    }
                }
            }
    }


    private fun updateUIWithExpenseRecord(record: ExpenseRecord) {
        expenseRecord = record

        _binding?.let { binding ->
            binding.title.text = record.title
            binding.date.text = "Added on ${Utils.formatDate(record.date)}"
            binding.totalAmount.text = "${Utils.extractCurrencyCode(record.currency)}${record.totalAmount}"
            binding.description.text = record.description
            Glide.with(binding.root.context).load(args.friendData.profileImageUrl).error(R.drawable.profile_imaage)
                .placeholder(R.drawable.profile_imaage)
                .into(binding.circularImage)

            splitDetailsAdapter.updateData(
                record.splits,
                record.splitType,
                record.totalAmount,
                record.currency
            )
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

    private fun observeOperationState() {
        viewLifecycleOwner.lifecycleScope.launch {
            mainViewModel.operationState.collect { state ->
                when (state) {
                    is UiState.Error -> showError(state.message)
                    UiState.Loading -> showLoadingIndicator()
                    is UiState.Success -> {
                        hideLoadingIndicator()
                        showError(state.data)
                        findNavController().navigateUp()
                    }

                    UiState.Stable -> { /* no-op */
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