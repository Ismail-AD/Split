package com.appdev.split.Adapters

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.appdev.split.UI.Fragment.AmountEquallyFragment
import com.appdev.split.UI.Fragment.AmountPercentFragment
import com.appdev.split.UI.Fragment.AmountUnEquallyFragment

class MyPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> AmountEquallyFragment()
            1 -> AmountUnEquallyFragment()
            2 -> AmountPercentFragment()
            else -> throw IllegalArgumentException("Invalid tab position")
        }
    }
}
