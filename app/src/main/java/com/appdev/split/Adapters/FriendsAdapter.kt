package com.appdev.split.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.appdev.split.Model.Data.Contact
import com.appdev.split.Model.Data.Friend
import com.appdev.split.Model.Data.FriendContact
import com.appdev.split.databinding.FriendsLayoutBinding
class FriendsAdapter(
    private val enableSelection: Boolean = false,
    private val onFriendSelected: ((FriendContact, Boolean) -> Unit)? = null
) : ListAdapter<FriendContact, FriendsAdapter.FriendViewHolder>(FriendDiffCallback()) {

    private val selectedFriends = mutableSetOf<FriendContact>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val binding = FriendsLayoutBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return FriendViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        val friend = getItem(position)
        holder.bind(friend, selectedFriends.contains(friend))
    }

    inner class FriendViewHolder(
        private val binding: FriendsLayoutBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(friend: FriendContact, isSelected: Boolean) {
            binding.apply {
                friendName.text = friend.name

                // Only show selection UI if selection is enabled
                if (enableSelection) {
                    selected.visibility = if (isSelected) View.VISIBLE else View.GONE
                } else {
                    selected.visibility = View.GONE
                }

                // Only enable click listener if selection is enabled
                if (enableSelection) {
                    root.setOnClickListener {
                        val wasSelected = selectedFriends.contains(friend)
                        if (wasSelected) {
                            selectedFriends.remove(friend)
                        } else {
                            selectedFriends.add(friend)
                        }
                        notifyItemChanged(position)
                        onFriendSelected?.invoke(friend, !wasSelected)
                    }
                } else {
                    root.setOnClickListener(null)
                    root.isClickable = false
                }
            }
        }
    }

    fun toggleFriendSelection(friend: FriendContact, isSelected: Boolean) {
        if (!enableSelection) return

        if (isSelected) {
            selectedFriends.add(friend)
        } else {
            selectedFriends.remove(friend)
        }
        notifyDataSetChanged()
    }

    class FriendDiffCallback : DiffUtil.ItemCallback<FriendContact>() {
        override fun areItemsTheSame(oldItem: FriendContact, newItem: FriendContact): Boolean {
            return oldItem.contact == newItem.contact
        }

        override fun areContentsTheSame(oldItem: FriendContact, newItem: FriendContact): Boolean {
            return oldItem == newItem
        }
    }
}