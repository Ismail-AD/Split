package com.appdev.split.UI.Activity

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContentProviderCompat.requireContext
import com.appdev.split.Model.Data.UserEntity
import com.appdev.split.Model.ViewModel.MainViewModel
import com.appdev.split.databinding.ActivitySignUpBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignUp : AppCompatActivity() {
    lateinit var binding: ActivitySignUpBinding
    private lateinit var pickMediaLauncher: ActivityResultLauncher<PickVisualMediaRequest>

    val mainViewModel by viewModels<MainViewModel>()
    var uri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.btnBackSign.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            this.finish()
        }

        binding.btnDoneSign.setOnClickListener {
            if (binding.etSignUpFullName.editText.toString()
                    .isNotEmpty() && binding.etSignUpEmail.editText?.text!!.isNotEmpty()
                && binding.etSignUpPassword.editText?.text!!.isNotEmpty()
            ) {

                val imageBytes = uri?.let { uri ->
                    try {
                        contentResolver.openInputStream(uri)?.use {
                            it.readBytes()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(
                            this,
                            "Failed to process image",
                            Toast.LENGTH_SHORT
                        ).show()
                        null
                    }
                }

                val userEntity = UserEntity(
                    binding.etSignUpFullName.editText?.text.toString(),
                    binding.etSignUpEmail.editText?.text!!.toString(),
                    binding.etSignUpPassword.editText?.text!!.toString(),
                )

                mainViewModel.startSignUp(userEntity = userEntity,uri=uri, imageBytes = imageBytes) { message, success ->
                    if (success) {
                        val intent2 = Intent(this, Login::class.java)
                        intent2.putExtra("email", userEntity.email)
                        startActivity(intent2)
                        this.finish()
                    } else {
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "fill all the fields", Toast.LENGTH_SHORT).show()
            }
        }

        pickMediaLauncher =
            registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uriget ->
                if (uriget != null) {
                    // If the user selected an image, display it
                    binding.ivImageTakerSplitWise.setImageURI(uriget)
                    uri = uriget
                } else {
                    Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
                }
            }

        binding.ivCameraSplitWise.setOnClickListener {
            openGallery()
        }


    }

    private fun openGallery() {
        pickMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
}