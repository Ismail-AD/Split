package com.appdev.split.Adapters

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.appdev.split.Model.Data.FriendContact
import com.appdev.split.UI.Fragment.AmountEquallyFragment
import com.appdev.split.UI.Fragment.AmountPercentFragment
import com.appdev.split.UI.Fragment.AmountUnEquallyFragment

class MyPagerAdapter(
    fragment: Fragment, private val friendsList: List<FriendContact>,
    private val totalAmount: Double,
    val myId: String
) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 3


    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> AmountEquallyFragment(friendsList,totalAmount,myId)
            1 -> AmountUnEquallyFragment(friendsList,totalAmount,myId)
            2 -> AmountPercentFragment(friendsList,totalAmount,myId)
            else -> throw IllegalArgumentException("Invalid tab position")
        }
    }
}
