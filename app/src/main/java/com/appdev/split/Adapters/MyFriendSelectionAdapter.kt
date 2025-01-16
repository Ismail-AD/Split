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
    private val isMultiSelect: Boolean = false,
    private val onSelectionChanged: (List<FriendContact>) -> Unit
) : RecyclerView.Adapter<MyFriendSelectionAdapter.FriendViewHolder>() {

    private var selectedFriend: FriendContact? = null
    private val selectedFriends = mutableSetOf<FriendContact>()

    inner class FriendViewHolder(private val binding: MyfriendsLayoutItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(friend: FriendContact) {
            binding.apply {
                friendName.text = friend.name
                selected.visibility = when {
                    isMultiSelect -> if (selectedFriends.contains(friend)) View.VISIBLE else View.INVISIBLE
                    else -> if (selectedFriend == friend) View.VISIBLE else View.INVISIBLE
                }

                root.setOnClickListener {
                    if (isMultiSelect) {
                        handleMultiSelection(friend)
                    } else {
                        handleSingleSelection(friend)
                    }
                    notifyDataSetChanged()
                }
            }
        }
    }

    private fun handleMultiSelection(friend: FriendContact) {
        if (selectedFriends.contains(friend)) {
            selectedFriends.remove(friend)
        } else {
            selectedFriends.add(friend)
        }
        onSelectionChanged(selectedFriends.toList())
    }

    private fun handleSingleSelection(friend: FriendContact) {
        selectedFriend = if (selectedFriend == friend) {
            null
        } else {
            friend
        }
        onSelectionChanged(listOfNotNull(selectedFriend))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val binding = MyfriendsLayoutItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FriendViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        holder.bind(friendsList[position])
    }

    override fun getItemCount() = friendsList.size

    fun getSelectedFriends(): List<FriendContact> {
        return if (isMultiSelect) {
            selectedFriends.toList()
        } else {
            listOfNotNull(selectedFriend)
        }
    }

    fun clearSelection() {
        if (isMultiSelect) {
            selectedFriends.clear()
        } else {
            selectedFriend = null
        }
        notifyDataSetChanged()
    }

    fun setSelectedFriends(friends: List<FriendContact>) {
        if (isMultiSelect) {
            selectedFriends.clear()
            selectedFriends.addAll(friends)
        } else if (friends.isNotEmpty()) {
            selectedFriend = friends.first()
        }
        notifyDataSetChanged()
    }
}