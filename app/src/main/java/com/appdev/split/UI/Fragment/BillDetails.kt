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
import com.appdev.split.Model.Data.FriendExpenseRecord
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
    private var expenseRecord: ExpenseRecord? = ExpenseRecord()
    private var friendExpense: FriendExpenseRecord? = FriendExpenseRecord()
    private var expensesListener: ListenerRegistration? = null

    @Inject
    lateinit var firestore: FirebaseFirestore

    @Inject
    lateinit var firebaseAuth: FirebaseAuth
    private var friendData: FriendContact? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBillDetailsBinding.inflate(inflater, container, false)
        setupShimmer()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainViewModel.updateStateToStable()

        setupDialog()
        setupRecyclerView()
        setupClickListeners()
        args.friendId?.let { friendId ->
            observeFriendData()
            mainViewModel.getFriendNameById(friendId)
        }


        when {
            args.groupId != null -> {
                mainViewModel.updateGroupExpense(ExpenseRecord())
                args.billData?.let { setupGroupExpenseListener(it.id, args.groupId!!) }
            }

            args.friendData != null -> {
                mainViewModel.updateFriendExpense(FriendExpenseRecord())

                args.friendsExpense?.let {
                    setupSingleExpenseListener(
                        it.id,
                        args.friendData!!.friendId
                    )
                }
            }
        }

        if (args.billData != null) {
            updateUIWithExpenseRecord(args.billData, null)
        } else {
            updateUIWithExpenseRecord(null, args.friendsExpense)
        }
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

    private fun setupShimmer() {
        binding.shimmerViewBar.layoutResource = R.layout.billdetails_shimmer
        binding.shimmerViewBar.inflate()
    }

    private fun showShimmer() {
        binding.main.visibility = View.GONE
        binding.shimmerViewTop.startShimmer()
    }

    private fun hideShimmer() {
        binding.shimmerViewTop.stopShimmer()
        binding.main.visibility = View.VISIBLE
    }

    private fun observeFriendData() {
        viewLifecycleOwner.lifecycleScope.launch {
            mainViewModel.FriendState.collect { state ->
                when (state) {
                    is UiState.Loading -> {
                        showShimmer()
                    }

                    is UiState.Success -> {
                        friendData = state.data
                        hideShimmer()
                        updateUIWithExpenseRecord(null, args.friendsExpense)
                    }

                    is UiState.Error -> {
                        hideShimmer()
                        showError(state.message)
                    }

                    is UiState.Stable -> {
                        // No-op
                    }
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.apply {
            backBtn.setOnClickListener {
                findNavController().navigateUp()
            }

            edit.setOnClickListener {
                when {
                    args.groupId != null -> {
                        val action =
                            BillDetailsDirections.actionBillDetailsToAddGrpExpenseFragment(
                                args.groupId, expenseRecord
                            )
                        findNavController().navigate(action)
                    }

                    args.friendData != null -> {
                        val action =
                            BillDetailsDirections.actionBillDetailsToPersonalExpenseFragment(
                                friendExpense,
                                args.friendData
                            )
                        findNavController().navigate(action)
                    }
                }
            }

            delete.setOnClickListener {
                observeOperationState()
                when {
                    args.groupId != null -> {
                        expenseRecord?.let { it1 ->
                            mainViewModel.deleteGroupExpense(
                                it1.id,
                                args.groupId!!
                            )
                        }
                    }

                    args.friendData != null -> {
                        friendExpense?.let { it2 ->
                            mainViewModel.deleteFriendExpenseDetail(
                                it2.id
                            )
                        }
                    }
                }
            }
        }
    }

    private fun setupSingleExpenseListener(expenseId: String, friendId: String) {
        val currentUser = firebaseAuth.currentUser ?: return
        val userId = currentUser.uid

        expensesListener?.remove()
        expensesListener = firestore.collection("expenses")
            .document(userId)
            .collection("friendsExpenses")
            .document(expenseId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Handle error
                    return@addSnapshotListener
                }

                snapshot?.let { documentSnapshot ->
                    val updatedRecord = documentSnapshot.toObject(FriendExpenseRecord::class.java)
                    Log.d("CHKUPA", "UPDATED ONE: ${updatedRecord}")
                    Log.d("CHKUPA", "RECEIVED ONE: ${expenseRecord}")
                    if (friendExpense != updatedRecord) {
                        updatedRecord?.let { record ->
                            updateUIWithExpenseRecord(null, record)
                        }
                    }
                }
            }
    }

    private fun setupGroupExpenseListener(expenseId: String, groupId: String) {
        expensesListener?.remove()
        expensesListener = firestore.collection("groupExpenses")
            .document(groupId)
            .collection("expenses")
            .document(expenseId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("GroupExpense", "Error listening to group expense: ${error.message}")
                    return@addSnapshotListener
                }

                snapshot?.let { documentSnapshot ->
                    val updatedRecord = documentSnapshot.toObject(ExpenseRecord::class.java)
                    Log.d("GroupExpense", "Updated group expense: $updatedRecord")
                    Log.d("GroupExpense", "Current expense record: $expenseRecord")

                    if (expenseRecord != updatedRecord) {
                        updatedRecord?.let { record ->
                            updateUIWithExpenseRecord(record, null)
                        }
                    }
                }
            }
    }


    private fun updateUIWithExpenseRecord(
        record: ExpenseRecord?,
        friendExpenseRecord: FriendExpenseRecord?
    ) {
        _binding?.let { binding ->
            when {
                record != null -> {
                    expenseRecord = record
                    binding.title.text = record.title
                    binding.date.text =
                        "Added on ${record.startDate + "-" + record.endDate}"
                    binding.totalAmount.text =
                        "${Utils.extractCurrencyCode(record.currency)}${record.totalAmount}"
                    binding.description.text = record.description

                    splitDetailsAdapter.updateData(
                        record.splits,
                        record.splitType,
                        record.totalAmount,
                        record.currency
                    )
                    binding.circularImage.visibility = View.GONE
                    binding.groupIcon.visibility = View.VISIBLE
                    when (args.groupImageUrl) {
                        "Couple" -> binding.groupIcon.setImageResource(R.drawable.love)
                        "Home" -> binding.groupIcon.setImageResource(R.drawable.home)
                        "Trip" -> binding.groupIcon.setImageResource(R.drawable.airplane)
                        else -> {
                            binding.groupIcon.setImageResource(R.drawable.airplane)
                        }
                    }
                }

                friendExpenseRecord != null -> {
                    friendExpense = friendExpenseRecord
                    binding.title.text = friendExpenseRecord.title
                    binding.date.text =
                        "Added on ${friendExpenseRecord.startDate + "-" + friendExpenseRecord.endDate}"
                    binding.totalAmount.text =
                        "${Utils.extractCurrencyCode(friendExpenseRecord.currency)}${friendExpenseRecord.totalAmount}"
                    binding.description.text = friendExpenseRecord.description

                    splitDetailsAdapter.updateData(
                        friendExpenseRecord.splits,
                        friendExpenseRecord.splitType,
                        friendExpenseRecord.totalAmount,
                        friendExpenseRecord.currency
                    )
                    binding.groupIcon.visibility = View.GONE
                    binding.circularImage.visibility = View.VISIBLE
                    if (args.friendData != null) {
                        Glide.with(binding.root.context)
                            .load(args.friendData?.profileImageUrl)
                            .error(R.drawable.profile_imaage)
                            .placeholder(R.drawable.profile_imaage)
                            .into(binding.circularImage)
                    } else {
                        friendData?.let {
                            Glide.with(binding.root.context)
                                .load(it.profileImageUrl)
                                .error(R.drawable.profile_imaage)
                                .placeholder(R.drawable.profile_imaage)
                                .into(binding.circularImage)
                        }
                    }
                }

                else -> {}
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