package com.appdev.split.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.appdev.split.Model.Data.Friend
import com.appdev.split.Model.Data.FriendContact
import com.appdev.split.databinding.MyfriendsLayoutItemBinding
class MyFriendSelectionAdapter(
    private val friendsList: List<FriendContact>,
    private var selectedFriend: FriendContact?, // Track the single selected friend
    private val onFriendClick: (FriendContact?) -> Unit
) : RecyclerView.Adapter<MyFriendSelectionAdapter.FriendViewHolder>() {

    inner class FriendViewHolder(private val binding: MyfriendsLayoutItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(friend: FriendContact, isSelected: Boolean) {
            binding.friendName.text = friend.name
            binding.selected.visibility = if (isSelected) View.VISIBLE else View.INVISIBLE

            binding.root.setOnClickListener {
                if (selectedFriend == friend) {
                    selectedFriend = null // Deselect if already selected
                } else {
                    selectedFriend = friend // Update the selected friend
                }
                notifyDataSetChanged() // Refresh the list
                onFriendClick(selectedFriend)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val binding = MyfriendsLayoutItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FriendViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        val friend = friendsList[position]
        val isSelected = friend == selectedFriend
        holder.bind(friend, isSelected)
    }

    override fun getItemCount() = friendsList.size
}
