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

    enum class ThemeMode(val value: String) {
        SYSTEM("system"),
        LIGHT("light"),
        DARK("dark")
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

        applyTheme(themeMode)
    }

    fun getCurrentThemeMode(context: Context): ThemeMode {
        val savedMode = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(PREF_THEME_MODE, ThemeMode.SYSTEM.value)
        return ThemeMode.entries.find { it.value == savedMode } ?: ThemeMode.SYSTEM
    }

    private fun applyTheme(themeMode: ThemeMode) {
        when (themeMode) {
            ThemeMode.LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            ThemeMode.DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            ThemeMode.SYSTEM -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
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