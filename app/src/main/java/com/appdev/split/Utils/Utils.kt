package com.appdev.split.Utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
}