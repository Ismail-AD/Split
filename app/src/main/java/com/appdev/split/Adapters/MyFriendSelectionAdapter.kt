package com.appdev.split.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.appdev.split.Model.Data.Friend
import com.appdev.split.databinding.MyfriendsLayoutItemBinding

class MyFriendSelectionAdapter(
    private val friendsList: List<Friend>,
    private val selectedFriends: MutableSet<Friend>, // Define the type explicitly
    private val onFriendClick: (Friend) -> Unit
) : RecyclerView.Adapter<MyFriendSelectionAdapter.FriendViewHolder>() {


    inner class FriendViewHolder(private val binding: MyfriendsLayoutItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(friend: Friend, isSelected: Boolean) {
            binding.friendName.text = friend.name
//            binding.profileImage.setImageResource(friend.profileImage)
            binding.selected.visibility = if (isSelected) View.VISIBLE else View.INVISIBLE

            binding.root.setOnClickListener {
                if (selectedFriends.contains(friend)) {
                    selectedFriends.remove(friend)
                } else {
                    selectedFriends.add(friend)
                }
                notifyItemChanged(bindingAdapterPosition)
                onFriendClick(friend)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val binding = MyfriendsLayoutItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FriendViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        val friend = friendsList[position]
        val isSelected = selectedFriends.contains(friend)
        holder.bind(friend, isSelected)
    }


    override fun getItemCount() = friendsList.size
}
