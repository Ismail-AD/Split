package com.appdev.split.UI.Fragment

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.appdev.split.Model.ViewModel.MainViewModel
import com.appdev.split.R
import com.appdev.split.UI.Activity.EntryActivity
import com.appdev.split.UI.Activity.Login
import com.appdev.split.Utils.ThemeUtils
import com.appdev.split.Utils.Utils
import com.appdev.split.databinding.AccountdeletedialogBinding
import com.appdev.split.databinding.FragmentProfileBinding
import com.appdev.split.databinding.LogoutdialogBinding
import com.appdev.split.databinding.ReauthdialogBinding
import com.appdev.split.databinding.ThemedialogBinding
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    lateinit var dialog: Dialog

    @Inject
    lateinit var firebaseAuth: FirebaseAuth
    val mainViewModel by activityViewModels<MainViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        firebaseAuth.currentUser?.uid?.let {
            mainViewModel.fetchUserData(it)
        }
        dialog = Dialog(requireContext())
        setupListeners()
        bindObserver()
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

    private fun bindObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.userData.collect { user ->
                    user?.let {
                        binding.name.text = "${it.name}"
                        Glide.with(requireContext()).load(user.imageUrl)
                            .placeholder(R.drawable.profile_imaage)
                            .error(R.drawable.profile_imaage).into(binding.imageprofile)
                    }
                }
            }
        }
    }


    private fun setupListeners() {
        binding.switchTheme.setOnClickListener {
            showThemeDialog()
        }
        binding.logout.setOnClickListener {
            showLogoutDialog()
        }
        binding.accountDelete.setOnClickListener {
            showDeleteAccountDialog()
        }
    }

    private fun showLogoutDialog() {
        val dialogBinding = LogoutdialogBinding.inflate(layoutInflater)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.logoutButton.setOnClickListener {
            firebaseAuth.signOut()
            dialog.dismiss()
            val intent = Intent(requireContext(), Login::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()

        }

        dialog.show()
    }

    private fun showDeleteAccountDialog() {
        val dialogBinding = AccountdeletedialogBinding.inflate(layoutInflater)

        val deleteDialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()

        dialogBinding.cancelButton.setOnClickListener {
            deleteDialog.dismiss()
        }

        dialogBinding.deleteButton.setOnClickListener {
            // Show re-authentication dialog
            deleteDialog.dismiss()
            showReAuthDialog()
        }

        deleteDialog.show()
    }

    private fun showReAuthDialog() {
        val dialogBinding = ReauthdialogBinding.inflate(layoutInflater)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Confirm Identity")
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()

        dialogBinding.cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.confirmButton.setOnClickListener {
            val email = dialogBinding.emailInput.text.toString()
            val password = dialogBinding.passwordInput.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            // Show loading state
            dialogBinding.confirmButton.isEnabled = false
            dialogBinding.cancelButton.isEnabled = false
            dialogBinding.confirmButton.text = "Verifying..."

            val credential = EmailAuthProvider.getCredential(email, password)

            firebaseAuth.currentUser?.reauthenticate(credential)?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    showLoadingIndicator()
                    deleteAccount()
                } else {
                    // Re-authentication failed
                    dialogBinding.confirmButton.isEnabled = true
                    dialogBinding.cancelButton.isEnabled = true
                    dialogBinding.confirmButton.text = "Confirm"
                    Toast.makeText(
                        requireContext(),
                        "Authentication failed: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        dialog.show()
    }

    private fun deleteAccount() {
        firebaseAuth.currentUser?.delete()?.addOnCompleteListener { task ->
            hideLoadingIndicator()
            if (task.isSuccessful) {
                Toast.makeText(
                    requireContext(),
                    "Account deleted successfully",
                    Toast.LENGTH_SHORT
                ).show()
                val intent = Intent(requireContext(), Login::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finish()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Failed to delete account: ${task.exception?.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    private fun showThemeDialog() {
        val dialogBinding = ThemedialogBinding.inflate(layoutInflater)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .create()

        // Set initial selection
        when (ThemeUtils.getCurrentThemeMode(requireContext())) {
            ThemeUtils.ThemeMode.SYSTEM -> dialogBinding.systemDefaultTheme.isChecked = true
            ThemeUtils.ThemeMode.LIGHT -> dialogBinding.lightTheme.isChecked = true
            ThemeUtils.ThemeMode.DARK -> dialogBinding.darkTheme.isChecked = true
        }

        dialogBinding.cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.saveButton.setOnClickListener {
            val selectedTheme = when (dialogBinding.themeRadioGroup.checkedRadioButtonId) {
                dialogBinding.systemDefaultTheme.id -> ThemeUtils.ThemeMode.SYSTEM
                dialogBinding.lightTheme.id -> ThemeUtils.ThemeMode.LIGHT
                dialogBinding.darkTheme.id -> ThemeUtils.ThemeMode.DARK
                else -> ThemeUtils.ThemeMode.SYSTEM
            }

            ThemeUtils.setThemeMode(requireContext(), selectedTheme)
            dialog.dismiss()
        }

        dialog.show()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}