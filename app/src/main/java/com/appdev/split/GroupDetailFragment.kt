package com.appdev.split

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.findNavController
import com.appdev.split.databinding.FragmentGroupDetailBinding

class GroupDetailFragment : Fragment() {
    var _binding: FragmentGroupDetailBinding? = null
    private val binding get() = _binding!!

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            navigateToNextScreen()
        } else {
            showPermissionGuidingDialog(true)
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentGroupDetailBinding.inflate(layoutInflater, container, false)
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.addContact.setOnClickListener {
            checkAndRequestPermission()
        }

        binding.addExp.setOnClickListener {
            findNavController().navigate(R.id.action_groupDetailFragment_to_addGrpExpenseFragment)
        }
    }

    private fun checkAndRequestPermission() {
        when {
            shouldShowRequestPermissionRationale(android.Manifest.permission.READ_CONTACTS) -> {
                showPermissionGuidingDialog(false) // Show the dialog for rationale
            }

            requireContext().checkSelfPermission(android.Manifest.permission.READ_CONTACTS) == android.content.pm.PackageManager.PERMISSION_DENIED -> {
                showPermissionGuidingDialog(true) // Inform user to manually enable the permission
            }

            else -> {
                requestPermissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
            }
        }
    }

    private fun showPermissionGuidingDialog(permissionPermanentlyDenied: Boolean) {
        AlertDialog.Builder(requireContext())
            .setTitle("Permission Required")
            .setMessage(
                if (permissionPermanentlyDenied) {
                    "You have permanently denied the contacts permission. Please enable it manually from app settings to use this feature."
                } else {
                    "This feature requires access to your contacts. Please grant the permission to proceed."
                }
            )
            .setPositiveButton(
                if (permissionPermanentlyDenied) "Go to Settings" else "OK"
            ) { _, _ ->
                if (permissionPermanentlyDenied) {
                    openAppSettings()
                } else {
                    requestPermissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openAppSettings() {
        val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = android.net.Uri.fromParts("package", requireContext().packageName, null)
        startActivity(intent)
    }

    private fun navigateToNextScreen() {
        findNavController().navigate(R.id.action_groupDetailFragment_to_addMembersFragment)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

}