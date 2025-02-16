package com.appdev.split.UI.Activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.appdev.split.Model.ViewModel.MainViewModel
import com.appdev.split.R
import com.appdev.split.Utils.ThemeUtils
import com.appdev.split.databinding.ActivityCurrencyTypeBinding
import com.appdev.split.databinding.ActivityMainBinding
import com.skydoves.powerspinner.OnSpinnerItemSelectedListener

class CurrencyType : AppCompatActivity() {
    lateinit var binding: ActivityCurrencyTypeBinding
    val mainViewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCurrencyTypeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (ThemeUtils.isDarkMode(this)) {
            ThemeUtils.setStatusBarDark(this, R.color.darkBackground)
        } else {
            ThemeUtils.setStatusBarLight(this, R.color.screenBack)
        }

        binding.currencySpinner.selectItemByIndex(0)

        binding.moveToMain.setOnClickListener {
            startActivity(Intent(this@CurrencyType, MainActivity::class.java))
            finish()
        }
    }
}