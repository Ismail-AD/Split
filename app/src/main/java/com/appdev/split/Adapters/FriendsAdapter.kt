package com.appdev.split.Adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.appdev.split.Model.Data.Contact
import com.appdev.split.Model.Data.Friend
import com.appdev.split.Model.Data.FriendContact
import com.appdev.split.databinding.FriendsLayoutBinding

class FriendsAdapter : ListAdapter<FriendContact, FriendsAdapter.FriendViewHolder>(FriendDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val binding = FriendsLayoutBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return FriendViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class FriendViewHolder(
        private val binding: FriendsLayoutBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(friend: FriendContact) {
            binding.friendName.text = friend.name
            // If you have profile images, load them here using Glide or Coil
        }
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