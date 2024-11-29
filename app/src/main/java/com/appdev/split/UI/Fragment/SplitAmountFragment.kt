package com.appdev.split.UI.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.appdev.split.Adapters.MyPagerAdapter
import com.appdev.split.Model.Data.Friend
import com.appdev.split.Model.ViewModel.MainViewModel
import com.appdev.split.databinding.FragmentSplitAmountBinding
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.app
import dagger.hilt.android.AndroidEntryPoint

//
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
        val list = args.friendsList.toMutableList()
        val amount = args.totalAmount
        if (currentUser != null && mainViewModel.userData.value != null) {
            list.add(
                Friend(
                    name = mainViewModel.userData.value!!.name,
                    contact = mainViewModel.userData.value!!.email
                )
            )
        }

        val pagerAdapter = MyPagerAdapter(this, list.toList(), amount)
        binding.viewPager.adapter = pagerAdapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = "Equally"
                1 -> tab.text = "Unequally"
                2 -> tab.text = "By Percentage"
            }
        }.attach()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}