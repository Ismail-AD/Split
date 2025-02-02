package com.appdev.split.UI.Activity

import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.appdev.split.R
import com.appdev.split.databinding.ActivityEntryBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EntryActivity : AppCompatActivity() {
    lateinit var binding: ActivityEntryBinding
    lateinit var navController: NavController
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val hostFragment =
            supportFragmentManager.findFragmentById(binding.navHostFragment.id) as NavHostFragment

        if (hostFragment != null) {
            navController = hostFragment.navController
            binding.bottomBar.setItemSelected(R.id.home_page, true)

            // Navigate to the home page initially
            navController.navigate(R.id.home_page)

            binding.bottomBar.setOnItemSelectedListener { id ->
                when (id) {
                    R.id.home_page -> {
                        navController.navigate(R.id.home_page)
                    }

                    R.id.group -> {
                        navController.navigate(R.id.group)
                    }

                    R.id.history -> {
                        navController.navigate(R.id.history)
                    }

                    R.id.profile -> {
                        navController.navigate(R.id.profile)
                    }

                    else -> {
                        navController.navigate(R.id.home_page)
                    }
                }
            }

            val destinationsWithHiddenBottomBar = setOf(
                R.id.addContactFragment,
                R.id.addGrpExpenseFragment,
                R.id.addMembersFragment,
                R.id.friendsAllExpenses,
                R.id.addGroupFragment,
                R.id.billDetails,
                R.id.personalExpenseFragment,
                R.id.splitAmountFragment,
                R.id.groupDetailFragment,

                // Add other fragment IDs where you want to hide the bottom bar
            )

            navController.addOnDestinationChangedListener { _, destination, _ ->
                if (destination.id in destinationsWithHiddenBottomBar) {
                    hideBottomBar()
                } else {
                    showBottomBar()
                }
            }
        }
    }

    private fun isNightModeEnabled(): Boolean {
        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES
    }

    // Function to show bottom bar
    fun showBottomBar() {
        binding.bottomBar.visibility = View.VISIBLE
    }

    // Function to hide bottom bar
    fun hideBottomBar() {
        binding.bottomBar.visibility = View.GONE
    }
}