package com.appdev.split.Adapters

import android.app.Person
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.appdev.split.Model.Data.Member
import com.appdev.split.R
import com.appdev.split.databinding.ItemMemberBinding
import com.appdev.split.databinding.ItemPersonInvolveBinding
import com.bumptech.glide.Glide

class SplitMembersAdapter(
    private val onPersonSelected: (List<Member>) -> Unit
) : RecyclerView.Adapter<SplitMembersAdapter.MemberViewHolder>() {

    private var members = listOf<Member>()
    fun updatePersons(newMembers: List<Member>) {
        members = newMembers
        notifyDataSetChanged()
    }

    fun selectAll(isSelected: Boolean) {
        val updatedMembers = members.map { it.copy(isSelected = isSelected) }
        members = updatedMembers
        notifyDataSetChanged()
        onPersonSelected(updatedMembers)
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
            binding.memberCheckBox.setOnCheckedChangeListener(null)
            binding.memberCheckBox.isChecked = member.isSelected
            Glide.with(binding.root.context).load(member.imageUrl).error(R.drawable.profile_imaage)
                .placeholder(R.drawable.profile_imaage)
                .into(binding.avatarImage)

            binding.memberCheckBox.setOnCheckedChangeListener { _, isChecked ->
                val updatedMembers = members.map {
                    if (it.id == member.id) it.copy(isSelected = isChecked)
                    else it
                }
                members = updatedMembers
                onPersonSelected(updatedMembers)
            }

            itemView.setOnClickListener {
                binding.memberCheckBox.isChecked = !binding.memberCheckBox.isChecked
            }
        }
    }
}