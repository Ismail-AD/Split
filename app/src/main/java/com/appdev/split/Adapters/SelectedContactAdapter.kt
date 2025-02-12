package com.appdev.split.Adapters

import android.view.LayoutInflater
import androidx.recyclerview.widget.RecyclerView
import com.appdev.split.Model.Data.Contact
import com.appdev.split.databinding.ItemSelectedContactBinding
import android.view.ViewGroup
import com.appdev.split.R
import com.bumptech.glide.Glide

class SelectedContactsAdapter(
    private var contacts: List<Contact>,
    private val onRemoveClick: (Contact) -> Unit
) : RecyclerView.Adapter<SelectedContactsAdapter.ContactViewHolder>() {

    inner class ContactViewHolder(val binding: ItemSelectedContactBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding = ItemSelectedContactBinding.inflate(
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
            closeButton.setOnClickListener {
                onRemoveClick(contact)
            }
            Glide.with(holder.binding.root.context).load(contact.imageUrl)
                .error(R.drawable.profile_imaage)
                .placeholder(R.drawable.profile_imaage)
                .into(holder.binding.contactIcon)
        }
    }

    override fun getItemCount() = contacts.size

    fun updateContacts(newContacts: List<Contact>) {
        contacts = newContacts
        notifyDataSetChanged()
    }
}
