package com.appdev.split.UI.Fragment

import android.Manifest
import android.app.Dialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
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
import com.appdev.split.Model.Data.Friend
import com.appdev.split.Model.Data.FriendContact
import com.appdev.split.Model.Data.UiState
import com.appdev.split.Model.ViewModel.MainViewModel
import com.appdev.split.R
import com.appdev.split.UI.Activity.EntryActivity
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

    lateinit var dialog: Dialog
    val mainViewModel by activityViewModels<MainViewModel>()

    private val selectedContacts = mutableListOf<Contact>()
    private lateinit var selectedContactsAdapter: SelectedContactsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAddMembersBinding.inflate(layoutInflater, container, false)

        setupAdapters()
        mainViewModel.fetchAllContacts()

        observeContacts()
        setupRecyclerViews()
        setupClickListeners()

        return binding.root
    }

    private fun setupAdapters() {
        // Initialize contacts adapter with selection callback
        contactsAdapter = ContactsAdapter { contact, isSelected ->
            handleContactSelection(contact, isSelected)
        }

        // Initialize selected contacts adapter
        selectedContactsAdapter = SelectedContactsAdapter(
            contacts = emptyList(),
            onRemoveClick = { contact ->
                handleContactRemoval(contact)
            }
        )

        splitwiseFriendsAdapter = FriendsAdapter()
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
        contactsAdapter.toggleContactSelection(contact, false)
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
                        is UiState.Loading -> showLoadingIndicator()
                        is UiState.Success -> {
                            hideLoadingIndicator()
                            savedFriendsList = state.data
                            fetchFirebaseUsers()
                        }
                        is UiState.Error -> showError(state.message)
                        UiState.Stable -> {}
                    }
                }
            }
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
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
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

        binding.addNewContactCard.setOnClickListener {
            findNavController().navigate(R.id.action_addMembersFragment_to_addContactFragment)
        }

        binding.fabAddMembers.setOnClickListener {
            if (selectedContacts.isNotEmpty()) {
                val friendsList = convertContactsToFriends(selectedContacts)
                mainViewModel.addContacts(friendsList)
                handleAddMembersResponse()
            }
        }
    }

    private fun fetchFirebaseUsers() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                showLoadingIndicator()
                val firestore = FirebaseFirestore.getInstance()
                val usersSnapshot = firestore.collection("profiles").get().await()

                val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email

                usersList = usersSnapshot.documents.mapNotNull { doc ->
                    val email = doc.getString("email") ?: return@mapNotNull null
                    val name = doc.getString("name") ?: return@mapNotNull null

                    // Skip current user and already added friends
                    if (email != currentUserEmail && !savedFriendsList.any { it.contact == email }) {
                        Contact(
                            name = name,
                            number = email,
                            isFriend = false
                        )
                    } else null
                }

                contactsAdapter.setContacts(usersList)
                hideLoadingIndicator()
            } catch (e: Exception) {
                hideLoadingIndicator()
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
                            findNavController().navigateUp()
                        }
                        is UiState.Error -> showError(state.message)
                        UiState.Stable -> {}
                    }
                }
            }
        }
    }

    private fun convertContactsToFriends(contacts: List<Contact>): List<Friend> {
        return contacts.map { contact ->
            Friend(
                name = contact.name,
                profileImageUrl = null,
                contact = contact.number  // Using email instead of phone number
            )
        }
    }

    private fun updateFabVisibility() {
        binding.fabAddMembers.visibility =
            if (selectedContacts.isNotEmpty()) View.VISIBLE else View.GONE
    }

    // Keep your existing helper methods for showing/hiding loading indicators and error handling

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val isGroupContact = args.isGroupContact
        if (isGroupContact) {
            binding.friendsTitle.visibility = View.VISIBLE
            binding.splitwiseFriendsRecyclerView.visibility = View.VISIBLE
        } else {
            binding.friendsTitle.visibility = View.GONE
            binding.splitwiseFriendsRecyclerView.visibility = View.GONE
        }
        dialog = Dialog(requireContext())
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}