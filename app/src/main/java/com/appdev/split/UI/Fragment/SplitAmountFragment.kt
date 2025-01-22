package com.appdev.split.UI.Fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import com.appdev.split.Adapters.EQUAL_SPLIT_POSITION
import com.appdev.split.Adapters.MyPagerAdapter
import com.appdev.split.Adapters.PERCENTAGE_SPLIT_POSITION
import com.appdev.split.Adapters.UNEQUAL_SPLIT_POSITION
import com.appdev.split.Model.Data.FriendContact
import com.appdev.split.Model.ViewModel.MainViewModel
import com.appdev.split.databinding.FragmentSplitAmountBinding
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class SplitAmountFragment : Fragment() {
    private var _binding: FragmentSplitAmountBinding? = null
    private val binding get() = _binding!!
    val args: SplitAmountFragmentArgs by navArgs()
    val mainViewModel by activityViewModels<MainViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSplitAmountBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentUser = FirebaseAuth.getInstance().currentUser
        val list = args.friendsList?.takeIf { it.isNotEmpty() }?.toMutableList() ?: mutableListOf()
        val friend = args.myFriend
        val amount = args.totalAmount.toDouble()
        if (list.isEmpty() && friend != null) {
            list.add(friend)
        }

        if (currentUser != null && mainViewModel.userData.value != null) {
            val userId = currentUser.uid // my entry in list for division of amount
            Log.d("CHKKI", "BEFORE UPDATE: $list")

            list.add(
                FriendContact(
                    friendId = userId,
                    name = mainViewModel.userData.value!!.name,
                    contact = mainViewModel.userData.value!!.email, profileImageUrl = mainViewModel.userData.value!!.imageUrl
                )
            )
            Log.d("CHKKI", "AFTER UPDATE: $list")


        }



        val pagerAdapter = MyPagerAdapter(
            this,
            list.toList(),
            amount,
            currentUser?.uid ?: "", currency = args.currency,args.splitType
        )


        binding.viewPager.apply {
            adapter = pagerAdapter
            // Disable swipe if you want to prevent manual navigation
            // isUserInputEnabled = false

            // Set initial position based on split type
            setCurrentItem(pagerAdapter.initialPosition, false)
        }
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                EQUAL_SPLIT_POSITION -> "Equal"
                UNEQUAL_SPLIT_POSITION -> "Unequal"
                PERCENTAGE_SPLIT_POSITION -> "Percentage"
                else -> throw IllegalArgumentException("Invalid tab position")
            }
        }.attach()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}