package com.appdev.split.Adapters

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.appdev.split.Model.Data.FriendContact
import com.appdev.split.Model.Data.SplitType
import com.appdev.split.UI.Fragment.AmountEquallyFragment
import com.appdev.split.UI.Fragment.AmountPercentFragment
import com.appdev.split.UI.Fragment.AmountUnEquallyFragment

const val EQUAL_SPLIT_POSITION = 0
const val UNEQUAL_SPLIT_POSITION = 1
const val PERCENTAGE_SPLIT_POSITION = 2
fun String.toPagerPosition(): Int {
    return when (this) {
        SplitType.EQUAL.name -> EQUAL_SPLIT_POSITION
        SplitType.UNEQUAL.name -> UNEQUAL_SPLIT_POSITION
        SplitType.PERCENTAGE.name -> PERCENTAGE_SPLIT_POSITION
        else -> {
            EQUAL_SPLIT_POSITION
        }
    }
}

class MyPagerAdapter(
    fragment: Fragment, private val friendsList: List<FriendContact>,
    private val totalAmount: Double,
    val myId: String, val currency: String, splitType: String
) : FragmentStateAdapter(fragment) {
    val initialPosition = splitType.toPagerPosition()

    override fun getItemCount(): Int = 3


    override fun createFragment(position: Int): Fragment {
        return when (position) {
            EQUAL_SPLIT_POSITION -> AmountEquallyFragment(friendsList, totalAmount, myId, currency)
            UNEQUAL_SPLIT_POSITION -> AmountUnEquallyFragment(friendsList, totalAmount, myId, currency)
            PERCENTAGE_SPLIT_POSITION -> AmountPercentFragment(friendsList, totalAmount, myId, currency)
            else -> throw IllegalArgumentException("Invalid tab position")
        }
    }
}
