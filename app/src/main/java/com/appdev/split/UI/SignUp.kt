package com.appdev.split.UI

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.appdev.split.MainActivity
import com.appdev.split.Model.Data.UserEntity
import com.appdev.split.Model.MainViewModel
import com.appdev.split.databinding.ActivitySignUpBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignUp : AppCompatActivity() {
    lateinit var binding: ActivitySignUpBinding
    private lateinit var pickMediaLauncher: ActivityResultLauncher<PickVisualMediaRequest>

    val mainViewModel by viewModels<MainViewModel>()

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
            if (binding.etSignUpFullName.editText.toString().isNotEmpty() && binding.etSignUpEmail.editText?.text!!.isNotEmpty()
                && binding.etSignUpPassword.editText?.text!!.isNotEmpty()
            ) {

                val userEntity = UserEntity(
                    binding.etSignUpFullName.editText.toString(),
                    binding.etSignUpEmail.editText?.text!!.toString(),
                    binding.etSignUpPassword.editText?.text!!.toString(),
                )

                mainViewModel.startSignUp(userEntity) { message, success ->
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

       pickMediaLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                // If the user selected an image, display it
                binding.ivImageTakerSplitWise.setImageURI(uri)
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