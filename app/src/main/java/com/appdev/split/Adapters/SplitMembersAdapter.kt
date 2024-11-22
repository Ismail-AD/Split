package com.appdev.split.Adapters

import android.app.Person
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.appdev.split.Model.Data.Member
import com.appdev.split.databinding.ItemMemberBinding
import com.appdev.split.databinding.ItemPersonInvolveBinding

class SplitMembersAdapter(
    private val onPersonSelected: (List<Member>) -> Unit
) : RecyclerView.Adapter<SplitMembersAdapter.MemberViewHolder>() {

    private var members = listOf<Member>()
    private var isSelectAllTriggered = false  // Flag to track if change is from "Select All"

    fun updatePersons(newMembers: List<Member>) {
        members = newMembers
        notifyDataSetChanged()
    }

    fun selectAll(isSelected: Boolean) {
        isSelectAllTriggered = true
        members.forEach { it.isSelected = isSelected }
        notifyDataSetChanged()
        onPersonSelected(members)
        isSelectAllTriggered = false
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val binding = ItemPersonInvolveBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MemberViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        holder.bind(members[position])
    }

    override fun getItemCount() = members.size

    inner class MemberViewHolder(
        private val binding: ItemPersonInvolveBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(member: Member) {
            binding.nameText.text = member.name

            // Remove old listener before setting new one to prevent callback loops
            binding.memberCheckBox.setOnCheckedChangeListener(null)
            binding.memberCheckBox.isChecked = member.isSelected

            binding.memberCheckBox.setOnCheckedChangeListener { _, isChecked ->
                if (!isSelectAllTriggered) {
                    member.isSelected = isChecked
                    onPersonSelected(members)
                }
            }

            itemView.setOnClickListener {
                if (!isSelectAllTriggered) {
                    binding.memberCheckBox.isChecked = !binding.memberCheckBox.isChecked
                }
            }
        }
    }
}