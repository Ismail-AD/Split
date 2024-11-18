package com.appdev.split

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.appdev.split.databinding.FragmentAddGroupBinding


class AddGroupFragment : Fragment() {

    private var _binding: FragmentAddGroupBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddGroupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? EntryActivity)?.hideBottomBar()
        val chips = listOf(binding.chipTrip, binding.chipHome, binding.chipCouple)
        chips.forEach { chip ->
            chip.setOnClickListener {
                chips.forEach { it.isSelected = false }
                chip.isSelected = true
            }
        }
        binding.closeIcon.setOnClickListener {
            findNavController().navigateUp()
        }


    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as? EntryActivity)?.showBottomBar()

        _binding = null
    }


}