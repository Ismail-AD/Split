package com.appdev.split.UI.Fragment

import android.app.Dialog
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.appdev.split.Model.Data.Contact
import com.appdev.split.Model.Data.Friend
import com.appdev.split.Model.Data.UiState
import com.appdev.split.Model.ViewModel.MainViewModel
import com.appdev.split.R
import com.appdev.split.databinding.FragmentAddContactBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddContactFragment : Fragment() {
    private var _binding: FragmentAddContactBinding? = null
    private val binding get() = _binding!!
    val mainViewModel by viewModels<MainViewModel>()
    lateinit var dialog: Dialog
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAddContactBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog = Dialog(requireContext())
        binding.doneTextView.setOnClickListener {
            if (validateFields()) {
                mainViewModel.addContact(Friend(name = binding.name.text.toString(), contact = binding.email.text.toString()))
                viewLifecycleOwner.lifecycleScope.launch {
                    repeatOnLifecycle(Lifecycle.State.STARTED) {
                        mainViewModel.operationState.collect { state ->
                            when (state) {
                                is UiState.Loading -> showLoadingIndicator()
                                is UiState.Success -> {
                                    hideLoadingIndicator()
                                    findNavController().navigateUp()
                                }

                                is UiState.Error -> showError(state.message)
                            }
                        }
                    }
                }
                Toast.makeText(requireContext(), "Contact saved successfully!", Toast.LENGTH_SHORT)
                    .show()

            }
        }
        binding.closeIcon.setOnClickListener {
            findNavController().navigateUp()
        }
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
    private fun validateFields(): Boolean {
        val name = binding.name.text.toString().trim()
        val contactInfo = binding.email.text.toString().trim()

        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show()
            return false
        }

        if (contactInfo.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "Email or Phone number cannot be empty",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }

        if (!isValidEmailOrPhone(contactInfo)) {
            Toast.makeText(
                requireContext(),
                "Enter a valid Email or Phone number",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }

        return true
    }

    private fun isValidEmailOrPhone(input: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(input).matches() || Patterns.PHONE.matcher(input)
            .matches()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}