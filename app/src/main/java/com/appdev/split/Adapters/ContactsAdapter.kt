package com.appdev.split.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.appdev.split.Model.Data.Contact
import com.appdev.split.databinding.ItemContactBinding

class ContactsAdapter(
    private val onContactSelected: (Contact, Boolean) -> Unit
) : RecyclerView.Adapter<ContactsAdapter.ContactViewHolder>() {

    private var contacts = listOf<Contact>()
    private val selectedContacts = mutableSetOf<Contact>()

    inner class ContactViewHolder(val binding: ItemContactBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding = ItemContactBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ContactViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = contacts[position]
        with(holder.binding) {
            contactName.text = contact.name
            selected.visibility =
                if (selectedContacts.contains(contact)) View.VISIBLE else View.GONE
            contactNumber.text = contact.number
            contactNumber.visibility = if (contact.isFriend) View.GONE else View.VISIBLE
            exists.visibility = if (contact.isFriend) View.VISIBLE else View.GONE


            myContact.alpha = if (contact.isFriend) 0.5f else 1f
            myContact.isEnabled = !contact.isFriend
            root.setOnClickListener {
                val isAlreadySelected = selectedContacts.contains(contact)

                if (isAlreadySelected) {
                    selectedContacts.remove(contact)
                } else {
                    selectedContacts.add(contact)
                }
                notifyItemChanged(position)

                onContactSelected(contact, !isAlreadySelected)
            }

        }
    }

    override fun getItemCount() = contacts.size

    fun setContacts(newContacts: List<Contact>) {
        contacts = newContacts
        notifyDataSetChanged()
    }

    fun toggleContactSelection(contact: Contact, isSelected: Boolean) {
        if (isSelected) {
            selectedContacts.add(contact)
        } else {
            selectedContacts.remove(contact)
        }
        notifyDataSetChanged()
    }
}

