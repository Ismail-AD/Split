package com.appdev.split.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.appdev.split.Model.Data.Friend
import com.appdev.split.Model.Data.FriendContact
import com.appdev.split.R
import com.appdev.split.databinding.MyfriendsLayoutItemBinding
import com.bumptech.glide.Glide

class MyFriendSelectionAdapter(
    private val friendsList: List<FriendContact>,
    private val isMultiSelect: Boolean = false,
    private val onSelectionChanged: (List<FriendContact>) -> Unit
) : RecyclerView.Adapter<MyFriendSelectionAdapter.FriendViewHolder>() {

    private var selectedFriend: FriendContact? = null
    private val selectedFriends = mutableSetOf<FriendContact>()
    private var preSelectedFriendIds: Set<String> = emptySet()

    inner class FriendViewHolder(private val binding: MyfriendsLayoutItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(friend: FriendContact) {
            binding.apply {
                friendName.text = friend.name
                Glide.with(profileImage.context)
                    .load(friend.profileImageUrl)
                    .placeholder(R.drawable.profile_imaage) // optional placeholder
                    .error(R.drawable.profile_imaage) // optional fallback
                    .circleCrop()
                    .into(profileImage)
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
    fun setPreSelectedFriends(friendIds: Set<String>) {
        preSelectedFriendIds = friendIds
        selectedFriends.clear()
        selectedFriends.addAll(friendsList.filter { it.friendId in preSelectedFriendIds })
        notifyDataSetChanged()
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