package com.appdev.split.Utils

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.view.View
import android.view.WindowManager
import androidx.annotation.ColorRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat


object ThemeUtils {
    private const val PREF_NAME = "theme_prefs"
    private const val PREF_THEME_MODE = "theme_mode"
    private const val THEME_MODE_LIGHT = "light"
    private const val THEME_MODE_DARK = "dark"
    private const val THEME_MODE_SYSTEM = "system"

    enum class ThemeMode(val value: String) {
        SYSTEM(THEME_MODE_SYSTEM),
        LIGHT(THEME_MODE_LIGHT),
        DARK(THEME_MODE_DARK)
    }

    fun applyTheme(context: Context) {
        when (getCurrentThemeMode(context)) {
            ThemeMode.LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            ThemeMode.DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            ThemeMode.SYSTEM -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
    fun getTheme(context: Context): Int {
        return when (getCurrentThemeMode(context)) {
            ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
    }


    fun setStatusBarLight(activity: Activity, @ColorRes color: Int) {
        activity.window.apply {
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE or
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            statusBarColor = ContextCompat.getColor(activity, color)
        }
    }

    fun setStatusBarDark(activity: Activity, @ColorRes color: Int) {
        activity.window.apply {
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            statusBarColor = ContextCompat.getColor(activity, color)

            decorView.systemUiVisibility = decorView.systemUiVisibility and
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        }
    }

    fun setThemeMode(context: Context, themeMode: ThemeMode) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(PREF_THEME_MODE, themeMode.value)
            .apply()

        when (themeMode) {
            ThemeMode.LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            ThemeMode.DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            ThemeMode.SYSTEM -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    fun getCurrentThemeMode(context: Context): ThemeMode {
        val savedMode = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(PREF_THEME_MODE, THEME_MODE_SYSTEM) // Default to system theme
        return ThemeMode.entries.find { it.value == savedMode } ?: ThemeMode.SYSTEM
    }


    private fun applyTheme(context: Context, themeMode: ThemeMode) {
        when (themeMode) {
            ThemeMode.LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            ThemeMode.DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            ThemeMode.SYSTEM -> {
                // For system mode, check the current system theme
                val currentNightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                when (currentNightMode) {
                    Configuration.UI_MODE_NIGHT_YES -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    Configuration.UI_MODE_NIGHT_NO -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                }

            }
        }
    }
    fun isDarkModeActivated(context: Context): Boolean {
        return getCurrentThemeMode(context) == ThemeMode.DARK
    }

    fun isDarkMode(context: Context): Boolean {
        return when (getCurrentThemeMode(context)) {
            ThemeMode.SYSTEM -> context.resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
        }
    }
}