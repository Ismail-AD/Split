package com.appdev.split.Utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Utils {
    suspend fun isInternetAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            val command = "ping -c 1 google.com"
            Runtime.getRuntime().exec(command).waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }

    fun sanitizeEmailForFirebase(email: String): String {
        return email.replace("@", "_at_").replace(".", "_dot_")
    }

    fun getCurrentDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
    fun getCurrentDay(): String {
        return SimpleDateFormat("dd", Locale.getDefault()).format(Date())
    }
    fun formatDate(inputDate: String): String {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
        val date = inputFormat.parse(inputDate) // Parse input string to Date
        return outputFormat.format(date!!) // Format Date to desired string
    }

    fun extractCurrencyCode(input: String): String {
        // Find the substring within parentheses
        return input.substringAfter("(").substringBefore(")")
    }
}