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
import com.appdev.split.Model.Data.UiState
import com.appdev.split.Model.ViewModel.MainViewModel
import com.appdev.split.R
import com.appdev.split.UI.Activity.EntryActivity
import com.appdev.split.databinding.FragmentAddMembersBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddMembersFragment : Fragment() {
    private var contactsList = listOf<Contact>()
    private var savedFriendsList = listOf<Friend>()
    private var _binding: FragmentAddMembersBinding? = null
    private val binding get() = _binding!!
    private lateinit var contactsAdapter: ContactsAdapter
    private lateinit var splitwiseFriendsAdapter: FriendsAdapter
    private val args: AddMembersFragmentArgs by navArgs()

    lateinit var dialog: Dialog
    val mainViewModel by activityViewModels<MainViewModel>()

    private val selectedContacts = mutableListOf<Contact>()
    private lateinit var selectedContactsAdapter: SelectedContactsAdapter
    private lateinit var permissionLauncher: ActivityResultLauncher<String>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    contactsList = getDeviceContacts()
                    val updatedContacts = contactsList.map { contact ->
                        contact.copy(isFriend = savedFriendsList.any { it.name == contact.name })
                    }
                    contactsAdapter.setContacts(updatedContacts)
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Permission denied. Cannot access contacts.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAddMembersBinding.inflate(layoutInflater, container, false)
        Log.d("CHKERR",mainViewModel.userData.value.toString() + "At add member")

        contactsAdapter = ContactsAdapter { contact, isSelected ->
            if (isSelected) {
                selectedContacts.add(contact)
            } else {
                selectedContacts.remove(contact)
            }
            selectedContactsAdapter.updateContacts(selectedContacts.toList())
            updateFabVisibility()
        }


        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.contactsState.collect { state ->
                    when (state) {
                        is UiState.Loading -> showLoadingIndicator()
                        is UiState.Success -> {
                            hideLoadingIndicator()
                            savedFriendsList = state.data
                            checkPermissionsAndLoadContacts()
                        }

                        is UiState.Error -> showError(state.message)
                    }
                }
            }
        }


        selectedContactsAdapter = SelectedContactsAdapter(
            contacts = emptyList(),
            onRemoveClick = { contact ->
                selectedContacts.remove(contact)
                selectedContactsAdapter.updateContacts(selectedContacts.toList())
                contactsAdapter.toggleContactSelection(contact, isSelected = false)
            }
        )
        binding.contactsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = contactsAdapter
        }

        binding.selectedContactsRecyclerView.apply {
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = selectedContactsAdapter
        }


        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
        splitwiseFriendsAdapter = FriendsAdapter()
        binding.splitwiseFriendsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = splitwiseFriendsAdapter
        }

        binding.addNewContactCard.setOnClickListener {
            findNavController().navigate(R.id.action_addMembersFragment_to_addContactFragment)
        }
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? EntryActivity)?.hideBottomBar()
        val isGroupContact = args.isGroupContact
        if (isGroupContact) {
            binding.friendsTitle.visibility = View.VISIBLE
            binding.splitwiseFriendsRecyclerView.visibility = View.VISIBLE
        } else {
            binding.friendsTitle.visibility = View.GONE
            binding.splitwiseFriendsRecyclerView.visibility = View.GONE
        }
        dialog = Dialog(requireContext())
        binding.fabAddMembers.setOnClickListener {
            if (selectedContacts.isNotEmpty()) {
                val friendsList = convertContactsToFriends(selectedContacts)
                mainViewModel.addContacts(friendsList)
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
                            }
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

    private fun updateFabVisibility() {
        binding.fabAddMembers.visibility =
            if (selectedContacts.isNotEmpty()) View.VISIBLE else View.GONE
    }


    private fun checkPermissionsAndLoadContacts() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_CONTACTS
            ) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            contactsList = getDeviceContacts()
            val updatedContacts = contactsList.map { contact ->
                contact.copy(isFriend = savedFriendsList.any { it.name == contact.name })
            }
            contactsAdapter.setContacts(updatedContacts)

        } else {
            requestPermission()
        }
    }


    fun convertContactsToFriends(contacts: List<Contact>): List<Friend> {
        return contacts.map { contact ->
            Friend(
                name = contact.name,
                profileImageUrl = null, contact = contact.number
            )
        }
    }

    private fun getDeviceContacts(): List<Contact> {
        val contacts = mutableListOf<Contact>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        requireContext().contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )?.use { cursor ->
            val nameIndex =
                cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex =
                cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (cursor.moveToNext()) {
                try {
                    val name = cursor.getString(nameIndex) ?: continue
                    val number = cursor.getString(numberIndex) ?: continue

                    // Normalize phone number to remove any formatting
                    val normalizedNumber = number.replace(Regex("[^0-9+]"), "")

                    // Only add if we have both name and number
                    if (name.isNotBlank() && normalizedNumber.isNotBlank()) {
                        contacts.add(Contact(name = name, number = normalizedNumber))
                    }
                } catch (e: Exception) {
                    continue
                }
            }
        }

        // Remove duplicates based on phone number
        return contacts.distinctBy { it.number }
    }

    private fun requestPermission() {
        permissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        (activity as? EntryActivity)?.showBottomBar()

        _binding = null
    }
}