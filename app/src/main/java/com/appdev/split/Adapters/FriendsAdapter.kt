package com.appdev.split.Adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.appdev.split.Model.Data.Contact
import com.appdev.split.Model.Data.Friend
import com.appdev.split.Model.Data.FriendContact
import com.appdev.split.R
import com.appdev.split.databinding.FriendsLayoutBinding

class FriendsAdapter(
    private val enableSelection: Boolean = false,
    private val onFriendSelected: ((FriendContact, Boolean) -> Unit)? = null
) : ListAdapter<FriendContact, FriendsAdapter.FriendViewHolder>(FriendDiffCallback()) {

    private val selectedFriends = mutableSetOf<String>() // Store friendId instead of whole object
    private val existingMembers = mutableSetOf<String>() // Store IDs of existing members


    fun setExistingMembers(members: List<FriendContact>) {
        existingMembers.clear()
        existingMembers.addAll(members.map { it.friendId })
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val binding = FriendsLayoutBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return FriendViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        val friend = getItem(position)
        val isMember = existingMembers.contains(friend.friendId)
        holder.bind(friend, selectedFriends.contains(friend.friendId), isMember)
    }

    inner class FriendViewHolder(
        private val binding: FriendsLayoutBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(friend: FriendContact, isSelected: Boolean, isMember: Boolean) {
            binding.apply {
                friendName.text = friend.name

                // Apply green background for existing members
                friendName.setTextColor(
                    if (isMember) Color.parseColor("#4CAF50") // Material Green
                    else Color.parseColor("#1C1B1F")
                )

                if (enableSelection) {
                    selected.visibility = if (isSelected) View.VISIBLE else View.GONE

                    root.setOnClickListener {
                        if (isMember) {
                            // Show toast for existing members
                            Toast.makeText(
                                root.context,
                                "${friend.name} is already a group member",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@setOnClickListener
                        }

                        val wasSelected = selectedFriends.contains(friend.friendId)
                        if (wasSelected) {
                            selectedFriends.remove(friend.friendId)
                        } else {
                            selectedFriends.add(friend.friendId)
                        }
                        selected.visibility = if (!wasSelected) View.VISIBLE else View.GONE
                        onFriendSelected?.invoke(friend, !wasSelected)
                    }
                } else {
                    selected.visibility = View.GONE
                    root.setOnClickListener(null)
                    root.isClickable = false
                }
            }
        }
    }

    fun toggleFriendSelection(friend: FriendContact, isSelected: Boolean) {
        if (!enableSelection) return

        if (isSelected) {
            selectedFriends.add(friend.friendId)
        } else {
            selectedFriends.remove(friend.friendId)
        }

        // Find the position of the friend in the current list
        val position = currentList.indexOfFirst { it.friendId == friend.friendId }
        if (position != -1) {
            notifyItemChanged(position)
        }
    }

    class FriendDiffCallback : DiffUtil.ItemCallback<FriendContact>() {
        override fun areItemsTheSame(oldItem: FriendContact, newItem: FriendContact): Boolean {
            return oldItem.friendId == newItem.friendId
        }

        override fun areContentsTheSame(oldItem: FriendContact, newItem: FriendContact): Boolean {
            return oldItem == newItem
        }
    }
}