package com.appdev.split.Adapters

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.appdev.split.Model.Data.Friend
import com.appdev.split.UI.Fragment.AmountEquallyFragment
import com.appdev.split.UI.Fragment.AmountPercentFragment
import com.appdev.split.UI.Fragment.AmountUnEquallyFragment

class MyPagerAdapter(
    fragment: Fragment, private val friendsList: List<Friend>,
    private val totalAmount: Float,
    val selectedId: Int,
    val email: String
) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 3


    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> AmountEquallyFragment(friendsList,totalAmount,selectedId,email)
            1 -> AmountUnEquallyFragment(friendsList,totalAmount,selectedId,email)
            2 -> AmountPercentFragment(friendsList,totalAmount,selectedId,email)
            else -> throw IllegalArgumentException("Invalid tab position")
        }
    }
}
