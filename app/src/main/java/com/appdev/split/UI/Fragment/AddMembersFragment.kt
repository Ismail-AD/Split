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
import com.appdev.split.Adapters.ContactsAdapter
import com.appdev.split.Adapters.FriendsAdapter
import com.appdev.split.Adapters.SelectedContactsAdapter
import com.appdev.split.Model.Data.Contact
import com.appdev.split.Model.Data.FriendContact
import com.appdev.split.Model.Data.UiState
import com.appdev.split.Model.ViewModel.MainViewModel
import com.appdev.split.R
import com.appdev.split.databinding.FragmentAddMembersBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@AndroidEntryPoint
class AddMembersFragment : Fragment() {
    private var usersList = listOf<Contact>()
    private var savedFriendsList = listOf<FriendContact>()
    private var _binding: FragmentAddMembersBinding? = null
    private val binding get() = _binding!!
    private lateinit var contactsAdapter: ContactsAdapter
    private lateinit var splitwiseFriendsAdapter: FriendsAdapter
    private val args: AddMembersFragmentArgs by navArgs()
    private var originalViewStates: Map<View, Int> = mapOf()
    private var isSearchActive = false

    lateinit var dialog: Dialog
    val mainViewModel by activityViewModels<MainViewModel>()
    var isGroupContact = false
    private val selectedContacts = mutableListOf<Contact>()
    private var isTopDataReady = false
    private var isMainDataReady = false
    private lateinit var selectedContactsAdapter: SelectedContactsAdapter
    private var groupMembers = listOf<FriendContact>() // Add this property
    private var isGroupMembersReady = false // Add this property
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAddMembersBinding.inflate(layoutInflater, container, false)
        dialog = Dialog(requireContext())
        isGroupContact = args.isGroupContact
        initializeViewsBasedOnType()
        setupShimmer()
        setupAdapters()
        mainViewModel.fetchAllContacts()
        if (args.selectedGroupId != null && args.selectedGroupId.trim().isNotEmpty()) {
            Log.d("CHKJM", "GOING TO MAKE CALL ${args.selectedGroupId}")

            mainViewModel.fetchAllGroupMembers(args.selectedGroupId)
            observeGroupMembers()
        }
        observeContacts()
        handleAddMembersResponse()
        setupRecyclerViews()
        setupClickListeners()
        setupSearchListener()
        return binding.root
    }


    private fun initializeViewsBasedOnType() {
        if (isGroupContact) {
            // Hide contact-related views for group contacts
            binding.apply {
                contactsRecyclerView.visibility = View.GONE
                noBill.visibility = View.GONE
                splitusers.visibility = View.GONE
            }
        }
    }

    private fun observeGroupMembers() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.membersState.collect { state ->
                    when (state) {
                        is UiState.Loading -> showShimmer()
                        is UiState.Success -> {
                            isGroupMembersReady = true
                            groupMembers = state.data
                            if (isGroupContact) {
                                splitwiseFriendsAdapter.setExistingMembers(groupMembers)
                            }
                            hideShimmer()
                            handleEmptyState()
                        }

                        is UiState.Error -> {
                            hideShimmer()
                            showError(state.message)
                        }

                        UiState.Stable -> {}
                    }
                }
            }
        }
    }


    private fun setupSearchListener() {
        binding.searchEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: android.text.Editable?) {
                val searchText = s?.toString() ?: ""
                handleSearchState(searchText)
            }
        })
    }


    private fun handleSearchState(searchText: String) {
        if (isGroupContact) {
            // Handle search for group contacts (friends only)
            if (searchText.isNotEmpty() && !isSearchActive) {
                saveOriginalViewStates()
                isSearchActive = true
            } else if (searchText.isEmpty() && isSearchActive) {
                restoreOriginalViewStates()
                isSearchActive = false
            }

            val filteredFriends = savedFriendsList.filter { friend ->
                friend.name.contains(searchText, ignoreCase = true) ||
                        friend.contact.contains(searchText, ignoreCase = true)
            }
            splitwiseFriendsAdapter.submitList(filteredFriends)
            binding.noBill.visibility = if (filteredFriends.isEmpty()) View.VISIBLE else View.GONE
            binding.billwording.text =
                if (filteredFriends.isEmpty()) "No matching friends found" else ""
            binding.splitwiseFriendsRecyclerView.visibility =
                if (filteredFriends.isEmpty()) View.GONE else View.VISIBLE
        } else {
            // Handle search for non-group contacts
            if (searchText.isNotEmpty() && !isSearchActive) {
                saveOriginalViewStates()
                hideViewsForSearch()
                isSearchActive = true
            } else if (searchText.isEmpty() && isSearchActive) {
                restoreOriginalViewStates()
                isSearchActive = false
            }

            // Filter contacts
            if (searchText.isNotEmpty()) {
                val filteredUsers = usersList.filter { contact ->
                    contact.name.contains(searchText, ignoreCase = true) ||
                            contact.number.contains(searchText, ignoreCase = true)
                }
                contactsAdapter.setContacts(filteredUsers)
                updateEmptyStateForSearch(filteredUsers.isEmpty())
            } else {
                contactsAdapter.setContacts(usersList)
                handleEmptyState()
            }
        }
    }

    private fun saveOriginalViewStates() {
        originalViewStates = mapOf(
            binding.friendsTitle to binding.friendsTitle.visibility,
            binding.splitwiseFriendsRecyclerView to binding.splitwiseFriendsRecyclerView.visibility,
            binding.splitusers to binding.splitusers.visibility
        )
    }

    private fun hideViewsForSearch() {
        binding.friendsTitle.visibility = View.GONE
        binding.splitwiseFriendsRecyclerView.visibility = View.GONE
        binding.splitusers.visibility = View.GONE
    }

    private fun restoreOriginalViewStates() {
        originalViewStates.forEach { (view, state) ->
            view.visibility = state
        }
    }

    private fun updateEmptyStateForSearch(isEmpty: Boolean) {
        binding.noBill.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.contactsRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        if (isEmpty) {
            binding.billwording.text = "No matching users found"
        }
    }

    // Modify your existing handleEmptyState() function


    private fun setupShimmer() {
        // Set the layout for the ViewStub
        isGroupContact = args.isGroupContact
        binding.shimmerViewAddMembers.layoutResource =
            if (isGroupContact) R.layout.add_grp_member_shimmer else R.layout.add_member_shimmer
        binding.shimmerViewAddMembers.inflate()

    }

    private fun setupAdapters() {
        // Initialize contacts adapter with selection callback
        contactsAdapter = ContactsAdapter { contact, isSelected ->
            if (isSearchActive) {
                // Clear search and restore normal view when selecting during search
                binding.searchEditText.apply {
                    setText("")  // Clear search text
                    clearFocus() // Remove focus
                }
                // Hide keyboard
                val imm =
                    requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(binding.searchEditText.windowToken, 0)
            }
            handleContactSelection(contact, isSelected)
        }

        // Initialize selected contacts adapter
        selectedContactsAdapter = SelectedContactsAdapter(
            contacts = emptyList(),
            onRemoveClick = { contact ->
                handleContactRemoval(contact)
            }
        )

        splitwiseFriendsAdapter = if (args.isGroupContact) {
            FriendsAdapter(
                enableSelection = true,
                onFriendSelected = { friend, isSelected ->
                    handleFriendSelection(friend, isSelected)
                }
            )
        } else {
            FriendsAdapter()
        }
    }

    private fun handleFriendSelection(friend: FriendContact, isSelected: Boolean) {
        val contact = Contact(
            name = friend.name,
            number = friend.contact,
            isFriend = true, friendId = friend.friendId
        )

        if (isSelected) {
            if (!selectedContacts.contains(contact)) {
                selectedContacts.add(contact)
            }
        } else {
            selectedContacts.remove(contact)
        }
        updateSelectedContactsView()
        updateFabVisibility()
    }

    private fun handleContactSelection(contact: Contact, isSelected: Boolean) {
        if (isSelected) {
            if (!selectedContacts.contains(contact)) {
                selectedContacts.add(contact)
            }
        } else {
            selectedContacts.remove(contact)
        }
        updateSelectedContactsView()
        updateFabVisibility()
    }

    private fun handleContactRemoval(contact: Contact) {
        selectedContacts.remove(contact)
        if (args.isGroupContact) {
            // Create a FriendContact from the Contact
            val friendContact = FriendContact(
                friendId = contact.friendId,
                contact = contact.number,
                name = contact.name,
                profileImageUrl = contact.imageUrl
            )
            // Update the friends adapter selection state
            splitwiseFriendsAdapter.toggleFriendSelection(friendContact, false)
        } else {
            contactsAdapter.toggleContactSelection(contact, false)
        }
        // Update the selected contacts view
        updateSelectedContactsView()
        updateFabVisibility()
    }

    private fun updateSelectedContactsView() {
        selectedContactsAdapter.updateContacts(selectedContacts.toList())
        binding.selectedContactsRecyclerView.visibility =
            if (selectedContacts.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun observeContacts() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.contactsState.collect { state ->
                    when (state) {
                        is UiState.Loading -> showShimmer()
                        is UiState.Success -> {
                            isMainDataReady = true
                            hideShimmer()
                            savedFriendsList = state.data
                            if (savedFriendsList.isEmpty()) {
                                binding.friendsTitle.visibility = View.GONE
                                binding.splitwiseFriendsRecyclerView.visibility = View.GONE
                            }
                            if (isGroupContact) {
                                splitwiseFriendsAdapter.submitList(savedFriendsList)
                                hideShimmer()
                                handleEmptyState()
                            } else {
                                myFriends()
                            }
                        }

                        is UiState.Error -> {
                            hideShimmer()
                            showError(state.message)
                        }

                        UiState.Stable -> {}
                    }
                }
            }
        }
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

    private fun setupRecyclerViews() {
        // Setup main contacts RecyclerView
        binding.contactsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = contactsAdapter
        }

        // Setup selected contacts RecyclerView
        binding.selectedContactsRecyclerView.apply {
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = selectedContactsAdapter
            visibility = View.GONE  // Initially hidden
        }

        // Setup Splitwise friends RecyclerView
        binding.splitwiseFriendsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = splitwiseFriendsAdapter
        }
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }

//        binding.addNewContactCard.setOnClickListener {
//            findNavController().navigate(R.id.action_addMembersFragment_to_addContactFragment)
//        }

        binding.fabAddMembers.setOnClickListener {
            if (selectedContacts.isNotEmpty()) {
                if (args.isGroupContact) {
                    mainViewModel.addNewMembersToGroup(selectedContacts, args.selectedGroupId)
                } else {
                    mainViewModel.addContacts(selectedContacts)
                }
            }
        }
    }

    private fun myFriends() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                showShimmer()

                val firestore = FirebaseFirestore.getInstance()
                val usersSnapshot = firestore.collection("profiles").get().await()
                val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email

                usersList = usersSnapshot.documents.mapNotNull { doc ->
                    val email = doc.getString("email") ?: return@mapNotNull null
                    val name = doc.getString("name") ?: return@mapNotNull null
                    val profileImage = doc.getString("profileImage") ?: return@mapNotNull null
                    val userid = doc.getString("userId") ?: return@mapNotNull null

                    if (email != currentUserEmail && !savedFriendsList.any { it.contact == email }) {
                        Contact(
                            name = name,
                            number = email, imageUrl = profileImage,
                            isFriend = false, friendId = userid
                        )
                    } else null
                }
                splitwiseFriendsAdapter.submitList(savedFriendsList)

                isTopDataReady = true

                hideShimmer()
                handleEmptyState()
                if (usersList.isNotEmpty()) {
                    contactsAdapter.setContacts(usersList)
                }
            } catch (e: Exception) {
                hideShimmer()
                showError("Failed to fetch users: ${e.message}")
            }
        }
    }


    private fun handleAddMembersResponse() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.operationState.collect { state ->
                    when (state) {
                        is UiState.Loading -> showLoadingIndicator()
                        is UiState.Success -> {
                            hideLoadingIndicator()
                            showError(state.data)
                            findNavController().navigateUp()
                        }

                        is UiState.Error -> showError(state.message)
                        UiState.Stable -> {}
                    }
                }
            }
        }
    }


    private fun updateFabVisibility() {
        binding.fabAddMembers.visibility =
            if (selectedContacts.isNotEmpty()) View.VISIBLE else View.GONE
    }

    // Keep your existing helper methods for showing/hiding loading indicators and error handling


    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    private fun showShimmer() {
        binding.appBarLayout.visibility = View.GONE
        binding.selectedContactsRecyclerView.visibility = View.GONE
        binding.nestedScrollView.visibility = View.GONE
        binding.shimmerContainer.visibility = View.VISIBLE
        binding.shimmerViewContainer.visibility = View.VISIBLE
        binding.shimmerViewContainer.startShimmer()
    }

    private fun hideShimmer() {
        if ((isTopDataReady && isMainDataReady) || (isMainDataReady && isGroupMembersReady)) {
            binding.shimmerContainer.visibility = View.GONE
            binding.shimmerViewContainer.stopShimmer()
            binding.shimmerViewContainer.visibility = View.GONE

            binding.appBarLayout.visibility = View.VISIBLE
            binding.selectedContactsRecyclerView.visibility = View.VISIBLE
            binding.nestedScrollView.visibility = View.VISIBLE
        }
    }

    private fun handleEmptyState() {
        if (isGroupContact) {
            binding.noBill.visibility = if (savedFriendsList.isEmpty()) View.VISIBLE else View.GONE
            binding.billwording.text = if (savedFriendsList.isEmpty()) "No friends to add" else ""
        } else {
            if (usersList.isEmpty()) {
                binding.noBill.visibility = View.VISIBLE
                binding.contactsRecyclerView.visibility = View.GONE
                binding.billwording.text = if (savedFriendsList.isNotEmpty()) {
                    "All users are already friends"
                } else {
                    "No users yet"
                }
            } else {
                binding.noBill.visibility = View.GONE
                binding.contactsRecyclerView.visibility = View.VISIBLE
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}