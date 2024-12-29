package com.appdev.split.UI.Fragment

import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.appdev.split.Model.Data.UiState
import com.appdev.split.Model.ViewModel.MainViewModel
import com.appdev.split.R
import com.appdev.split.UI.Activity.EntryActivity
import com.appdev.split.databinding.FragmentAddGroupBinding
import kotlinx.coroutines.launch

class AddGroupFragment : Fragment() {

    private var _binding: FragmentAddGroupBinding? = null
    private val binding get() = _binding!!
    private lateinit var pickMediaLauncher: ActivityResultLauncher<PickVisualMediaRequest>
    val mainViewModel by activityViewModels<MainViewModel>()
    var imageUri: Uri? = null
    lateinit var dialog: Dialog

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddGroupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog = Dialog(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.operationState.collect { state ->
                    when (state) {
                        is UiState.Loading -> showLoadingIndicator()
                        is UiState.Success -> {
                            hideLoadingIndicator()
                            findNavController().popBackStack()
                        }

                        is UiState.Error -> {
                            hideLoadingIndicator()
                            showError(state.message)
                        }

                        UiState.Stable -> {

                        }
                    }
                }
            }
        }


        val chips = listOf(binding.chipTrip, binding.chipHome, binding.chipCouple)
        chips.forEach { chip ->
            chip.setOnClickListener {
                chips.forEach { it.isSelected = false }
                chip.isSelected = true
            }
        }

        binding.saveGroup.setOnClickListener {
            val groupName = binding.etSignUpUsername.editText?.text?.toString()


            if (groupName != null) {
                if (groupName.trim().isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        "Group name cannot be empty",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                    return@setOnClickListener
                }
            }

            val selectedChip = chips.find { it.isSelected }
            if (selectedChip == null) {
                Toast.makeText(requireContext(), "Please select a group type", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }
            if (groupName != null) {
                viewLifecycleOwner.lifecycleScope.launch {
                    val imageBytes = imageUri?.let { uri ->
                        try {
                            requireContext().contentResolver.openInputStream(uri)?.use {
                                it.readBytes()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(
                                requireContext(),
                                "Failed to process image",
                                Toast.LENGTH_SHORT
                            ).show()
                            null
                        }
                    }
                    mainViewModel.saveNewGroup(
                        imageUri = imageUri,
                        imagebytes = imageBytes,
                        title = groupName,
                        groupType = selectedChip.text.toString()
                    )
                }
            }


            // Validation passed, proceed with saving the group
            Toast.makeText(
                requireContext(),
                "Group saved: $groupName (${selectedChip.text})",
                Toast.LENGTH_SHORT
            ).show()
            // Add your logic to save the group here
        }
        binding.closeIcon.setOnClickListener {
            findNavController().navigateUp()
        }

        pickMediaLauncher =
            registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                if (uri != null) {
                    imageUri = uri
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

    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    private fun showLoadingIndicator() {
        dialog.setContentView(R.layout.progress_dialog)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCancelable(false)
        dialog.show()
    }

    private fun hideLoadingIndicator() {
        if (dialog.isShowing) {
            dialog.dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }


}