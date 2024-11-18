package com.appdev.split

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.findNavController
import com.appdev.split.databinding.FragmentAddGroupBinding


class AddGroupFragment : Fragment() {

    private var _binding: FragmentAddGroupBinding? = null
    private val binding get() = _binding!!
    private lateinit var pickMediaLauncher: ActivityResultLauncher<PickVisualMediaRequest>


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

        pickMediaLauncher =
            registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                if (uri != null) {
                    // If the user selected an image, display it
                    binding.addGroupImage.setImageURI(uri)
                } else {
                    Toast.makeText(requireContext(), "No image selected", Toast.LENGTH_SHORT).show()
                }
            }

        binding.addGroupImage.setOnClickListener {
            openGallery()
        }
    }

    private fun openGallery() {
        pickMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as? EntryActivity)?.showBottomBar()

        _binding = null
    }


}