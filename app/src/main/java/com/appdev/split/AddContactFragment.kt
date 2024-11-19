package com.appdev.split

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.appdev.split.databinding.FragmentAddContactBinding

class AddContactFragment : Fragment() {
    private var _binding: FragmentAddContactBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAddContactBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.doneTextView.setOnClickListener {
            if (validateFields()) {
                Toast.makeText(requireContext(), "Contact saved successfully!", Toast.LENGTH_SHORT)
                    .show()

            }
        }
        binding.closeIcon.setOnClickListener {
            findNavController().navigateUp()
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
