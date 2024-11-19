package com.appdev.split

import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.appdev.split.Adapters.ContactsAdapter
import com.appdev.split.Adapters.FriendsAdapter
import com.appdev.split.Model.Data.Contact
import com.appdev.split.Model.Friend
import com.appdev.split.databinding.FragmentAddGroupBinding
import com.appdev.split.databinding.FragmentAddMembersBinding


class AddMembersFragment : Fragment() {
    private var contactsList = listOf<Contact>()
    private var _binding: FragmentAddMembersBinding? = null
    private val binding get() = _binding!!
    private lateinit var contactsAdapter: ContactsAdapter
    private lateinit var splitwiseFriendsAdapter: FriendsAdapter
    val sampleFriends = listOf(
        Friend("1", "Mubeen Fivver")
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAddMembersBinding.inflate(layoutInflater, container, false)
        checkPermissionsAndLoadContacts()
        contactsAdapter = ContactsAdapter()
        contactsAdapter.submitList(contactsList)
        binding.contactsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = contactsAdapter
        }

        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
        splitwiseFriendsAdapter = FriendsAdapter()
        splitwiseFriendsAdapter.submitList(sampleFriends)
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


    }

    private fun checkPermissionsAndLoadContacts() {
        if (checkSelfPermission(requireContext(), android.Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            contactsList = getDeviceContacts()
        } else {
            requestPermission()
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
                        contacts.add(Contact(name, normalizedNumber))
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
        val permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    contactsList = getDeviceContacts()
                }
            }
        permissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        (activity as? EntryActivity)?.showBottomBar()

        _binding = null
    }
}