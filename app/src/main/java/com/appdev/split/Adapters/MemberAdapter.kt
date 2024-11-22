package com.appdev.split.Adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.appdev.split.R
import com.appdev.split.databinding.ItemMemberBinding
class MemberAdapter(
    private var members: List<String>,
    private val selectedItems: MutableSet<String>
) : RecyclerView.Adapter<MemberAdapter.MemberViewHolder>() {

    private var filteredMembers: List<String> = members

    inner class MemberViewHolder(private val binding: ItemMemberBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(member: String) {
            binding.apply {
                memberName.text = member

                // Remove the listener before setting the checked state to avoid unwanted triggers
                memberCheckBox.setOnCheckedChangeListener(null)

                // Set checkbox state based on selectedItems set
                memberCheckBox.isChecked = selectedItems.contains(member)

                // Re-add the listener after setting the state
                memberCheckBox.setOnCheckedChangeListener { _, isChecked ->
                    handleIndividualSelection(member, isChecked)
                }
            }
        }
    }

    private fun handleIndividualSelection(member: String, isChecked: Boolean) {
        if (isChecked) {
            selectedItems.add(member)
        } else {
            selectedItems.remove(member)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val binding = ItemMemberBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MemberViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        holder.bind(filteredMembers[position])
    }

    override fun getItemCount(): Int = filteredMembers.size

    fun updateData(newMembers: List<String>) {
        members = newMembers
        filteredMembers = newMembers
        notifyDataSetChanged()
    }

    fun filterData(query: String) {
        filteredMembers = if (query.isEmpty()) {
            // When query is empty, show all members but maintain selection state
            members
        } else {
            // Filter members but don't affect the selection state
            members.filter { it.contains(query, ignoreCase = true) }
        }
        notifyDataSetChanged()
    }

    fun getSelectedMembers(): Set<String> = selectedItems.toSet()
}