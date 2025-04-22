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
import com.appdev.split.Model.Data.Contact
import com.appdev.split.Model.Data.ExpenseRecord
import com.appdev.split.Model.Data.FriendContact
import com.appdev.split.Model.Data.FriendExpenseRecord
import com.appdev.split.Model.Data.UiState
import com.appdev.split.Model.ViewModel.MainViewModel
import com.appdev.split.R
import com.appdev.split.databinding.FragmentFriendsAllExpensesBinding
import com.bumptech.glide.Glide
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
        mainViewModel.updateGFriendExpenseToEmpty(FriendExpenseRecord())
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
        Log.d("CJKAZMX", "friendId: ${args.friendUserId}")

        if (args.bilList.isNotEmpty()) {
            firebaseAuth.currentUser?.uid?.let { myId ->
                val initialBillList = args.bilList.toList().toMutableList() // Make mutable

                updateRecyclerView(initialBillList)

                // Use Firestore instead of Realtime Database
                val expensesRef = firestore.collection("expenses")
                    .document(myId)
                    .collection("friendsExpenses")
                    .whereArrayContains("participantIds", args.friendUserId)

                // Create a snapshot listener
                expensesRef.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("CHKSS", "Listen failed.", error)
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val newExpenses = mutableListOf<FriendExpenseRecord>()

                        for (doc in snapshot.documents) {
                            val expenseRecord = doc.toObject(FriendExpenseRecord::class.java)
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
                            Log.d("CHJAZAZ", "DATA ${state.data}")
                            Log.d("CHJAZAZ", "DATA ID: ${args.friendUserId}")

                            Glide.with(requireContext())
                                .load(state.data.profileImageUrl)
                                .placeholder(R.drawable.profile_imaage) // optional placeholder
                                .error(R.drawable.profile_imaage) // optional fallback
                                .circleCrop()
                                .into(binding.ImageProfile)
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
            friendContact?.let {
                if (it.friendId.isEmpty()) {
                    // Show dialog to add as friend first
                    showAddFriendDialog(args.friendUserId)
                } else {
                    val action =
                        FriendsAllExpensesDirections.actionFriendsAllExpensesToPersonalExpenseFragment(
                            null,
                            friendContact
                        )
                    findNavController().navigate(action)

                }
            }
        }
    }

    private fun showAddFriendDialog(friendsId: String) {
        val dialogBuilder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
        dialogBuilder.setTitle("Add Friend First")
        dialogBuilder.setMessage("You need to save this person as a friend first before creating an expense. Would you like to add them to your friends list?")

        dialogBuilder.setPositiveButton("Yes, Add Friend") { dialog, _ ->
            // Step 1: Fetch profile first
            mainViewModel.fetchProfileById(friendsId)

            // Step 2: Observe once when profile is fetched
            lifecycleScope.launch {
                mainViewModel.personInfoState.collect { state ->
                    when (state) {
                        is UiState.Success -> {
                            val profile = state.data
                            friendContact = state.data
                            Log.d("CHKZMA","AT MAIN ${state.data}")
                            val newContact = Contact(
                                name = profile.name,
                                number = profile.contact,
                                imageUrl = profile.profileImageUrl ?: "",
                                isFriend = false,
                                friendId = profile.friendId // or profile.id
                            )
                            Log.d("CHKZMA","AT MAIN PARAM ${friendsId}")


                            val contactsList = mutableListOf(newContact)
                            mainViewModel.addContacts(contactsList)
                            viewLifecycleOwner.lifecycleScope.launch {
                                mainViewModel.operationState.collect { state ->
                                    when (state) {
                                        is UiState.Success -> {
                                            hideLoadingIndicator()
                                            Toast.makeText(
                                                requireContext(),
                                                "Friend added successfully!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }

                                        is UiState.Error -> {
                                            hideLoadingIndicator()
                                            Toast.makeText(
                                                requireContext(),
                                                "Failed to add friend: ${state.message}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }

                                        else -> { /* Handle other states if needed */
                                        }
                                    }
                                }
                            }
                        }

                        is UiState.Error -> {
                            hideLoadingIndicator()
                            Toast.makeText(
                                requireContext(),
                                "Failed to fetch profile: ${state.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        is UiState.Loading -> {
                            // Ensure loading indicator is shown
                            showLoadingIndicator()
                        }

                        else -> { /* Loading or Idle - optional to handle */
                        }
                    }
                }
            }

            dialog.dismiss()
        }

        dialogBuilder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = dialogBuilder.create()
        dialog.show()
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
        binding.ImageProfile.visibility = View.GONE
        binding.BottomAllContent.visibility = View.GONE
    }

    private fun hideShimmer() {
        binding.shimmerViewContainer.stopShimmer()
        binding.shimmerViewContainer.visibility = View.GONE
        // Hide actual content while shimmer is showing
        binding.topBar.visibility = View.VISIBLE
        binding.ImageProfile.visibility = View.VISIBLE
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

    private fun updateRecyclerView(expenses: List<FriendExpenseRecord>) {
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

    fun goToDetails(expenseList: FriendExpenseRecord) {
        Log.d("CHKIAMG", "I am going in")
        friendContact?.let {
            // Check if this is a friend (has a friendId) or just a contact
            if (it.friendId.isEmpty()) {
                // Show dialog to add as friend first
                showAddFriendDialog(args.friendUserId)
            } else {
                // Proceed with existing navigation
                val action = FriendsAllExpensesDirections.actionFriendsAllExpensesToBillDetails(
                    null, it, null, expenseList, null, null
                )
                findNavController().navigate(action)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }
}