package com.appdev.split.UI.Activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.appdev.split.Adapters.OnboardingAdapter
import com.appdev.split.R
import com.appdev.split.Utils.ThemeUtils
import com.appdev.split.databinding.ActivityOnBoardingBinding
import com.xcode.onboarding.MaterialOnBoarding
import com.xcode.onboarding.OnBoardingPage

class OnBoardingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOnBoardingBinding
    private lateinit var adapter: OnboardingAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (ThemeUtils.isDarkMode(this)) {
            ThemeUtils.setStatusBarDark(this, R.color.darkBackground)
        } else {
            ThemeUtils.setStatusBarLight(this, R.color.screenBack)
        }
        binding = ActivityOnBoardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupOnboarding()
        setupButtons()

    }

    private fun setupOnboarding() {
        adapter = OnboardingAdapter()
        binding.viewPagerOnboarding.adapter = adapter

        // Connect dots indicator with ViewPager2
        binding.indicatorLayout.attachTo(binding.viewPagerOnboarding)

        // Optional: Disable swiping
        // binding.viewPagerOnboarding.isUserInputEnabled = false
    }

    private fun setupButtons() {
        binding.buttonNext.setOnClickListener {
            val currentItem = binding.viewPagerOnboarding.currentItem
            if (currentItem < adapter.itemCount - 1) {
                binding.viewPagerOnboarding.currentItem = currentItem + 1
            } else {
                startActivity(Intent(this@OnBoardingActivity, CurrencyType::class.java))
                finish()
            }
        }

        // Update button text based on page
        binding.viewPagerOnboarding.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    binding.buttonNext.text = if (position == adapter.itemCount - 1) {
                        "Get Started"
                    } else {
                        "Next"
                    }
                }
            }
        )
    }
}