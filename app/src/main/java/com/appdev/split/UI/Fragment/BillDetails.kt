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
    private lateinit var dialog: Dialog
    private val args: BillDetailsArgs by navArgs()
    private val mainViewModel by activityViewModels<MainViewModel>()
    private lateinit var splitDetailsAdapter: SplitDetailsAdapter
    private var expenseRecord: ExpenseRecord = ExpenseRecord()

    @Inject
    lateinit var firestore: FirebaseFirestore

    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBillDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainViewModel.updateStateToStable()

        setupDialog()
        setupRecyclerView()
        setupClickListeners()
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

//            edit.setOnClickListener {
//                val action = BillDetailsDirections.actionBillDetailsToPersonalExpenseFragment(
//                    expenseRecord,
//                    args.fr
//                )
//                findNavController().navigate(action)
//            }
//
//            delete.setOnClickListener {
//                observeOperationState()
//                mainViewModel.deleteFriendExpenseDetail(expenseRecord.id, args.friendContact)
//            }
        }
    }



    private fun updateUIWithExpenseRecord(record: ExpenseRecord) {
        expenseRecord = record

        binding.apply {
            title.text = record.title
            date.text = "Added on ${Utils.formatDate(record.date)}"
            totalAmount.text = "${record.currency}${record.totalAmount}"
            description.text = record.description
        }

        splitDetailsAdapter.updateData(
            record.splits,
            record.splitType,
            record.totalAmount,
            record.currency
        )
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
                        findNavController().navigateUp()
                    }
                    UiState.Stable -> { /* no-op */ }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}