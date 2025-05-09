package com.appdev.split.Utils

import android.content.Context
import android.content.SharedPreferences
import com.appdev.split.Adapters.EQUAL_SPLIT_POSITION
import com.appdev.split.Adapters.PERCENTAGE_SPLIT_POSITION
import com.appdev.split.Adapters.UNEQUAL_SPLIT_POSITION
import com.appdev.split.Model.Data.NameId
import com.appdev.split.Model.Data.PaymentDistribute
import com.appdev.split.Model.Data.Percentage
import com.appdev.split.Model.Data.Split
import com.appdev.split.Model.Data.SplitType
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

object Utils {
    private const val KEY_ONBOARDING_PASSED = "onboarding_passed"
    private const val PREFS_NAME = "app_prefs"
    private const val KEY_CURRENCY = "selected_currency"
    suspend fun isInternetAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            val command = "ping -c 1 google.com"
            Runtime.getRuntime().exec(command).waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }


    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun setOnboardingPassed(context: Context, passed: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_ONBOARDING_PASSED, passed).apply()
    }

    fun isOnboardingPassed(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_ONBOARDING_PASSED, false)
    }

    fun parseDate(dateString: String): Calendar {
        val calendar = Calendar.getInstance()
        val parts = dateString.split("-")
        if (parts.size == 3) {
            calendar.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
        }
        return calendar
    }

    fun sanitizeEmailForFirebase(email: String): String {
        return email.replace("@", "_at_").replace(".", "_dot_")
    }

    fun getCurrentDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    fun getYearMonth(): String {
        return SimpleDateFormat("yyyy-M", Locale.getDefault()).format(Date())
    }

    fun getDay(): String {
        return SimpleDateFormat("dd", Locale.getDefault()).format(Date())
    }


    fun getCurrentDay(input: String?): String {
        return if (!input.isNullOrEmpty()) {
            // Parse the input date and extract the day
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = dateFormat.parse(input)
            SimpleDateFormat("dd", Locale.getDefault()).format(date!!)
        } else {
            // Get the current day
            SimpleDateFormat("dd", Locale.getDefault()).format(Date())
        }
    }

    fun formatDate(inputDate: String): String {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault()) // Changed format

        return try {
            val date = inputFormat.parse(inputDate)
            outputFormat.format(date!!)
        } catch (e: ParseException) {
            inputDate // Return the original string if parsing fails
        }
    }



    fun extractCurrencyCode(input: String): String {
        // Find the substring within parentheses
        return input.substringAfter("(").substringBefore(")")
    }

    fun getSplitType(selectedType: String): SplitType {
        return when (selectedType) {
            SplitType.EQUAL.name -> SplitType.EQUAL
            SplitType.UNEQUAL.name -> SplitType.UNEQUAL
            SplitType.PERCENTAGE.name -> SplitType.PERCENTAGE
            else -> SplitType.EQUAL
        }
    }

    fun createEqualSplits(
        nameIdList: List<NameId>,
        amountPerPerson: Double
    ): List<Split> {
        return nameIdList.map { nameId ->
            Split(
                userId = nameId.id,
                username = nameId.name,
                amount = amountPerPerson,
                percentage = 100.0 / nameIdList.size
            )
        }
    }


    fun updateEqualSplits(
        splitList: List<Split>,
        amountPerPerson: Double
    ): List<Split> {
        val percentage = (100.0 / splitList.size).round2Decimals()
        return splitList.map { split ->
            split.copy(
                amount = amountPerPerson.round2Decimals(),
                percentage = percentage
            )
        }
    }

    fun getCurrentUserId(): String {
        return FirebaseAuth.getInstance().currentUser?.uid ?: ""
    }

    fun createUnequalSplitsFromPayments(
        selectedPayments: List<PaymentDistribute>
    ): List<Split> {
        val totalAmount = selectedPayments.sumOf { it.amount }

        return selectedPayments.map { payment ->
            Split(
                userId = payment.id,
                username = payment.name,
                amount = payment.amount.round2Decimals(),
                percentage = ((payment.amount / totalAmount) * 100).round2Decimals()
            )
        }
    }

    fun getParticipantIds(splits: List<Split>): List<String> {
        return splits.map { it.userId }.distinct()
    }

    fun createPercentageSplitsFromPayments(
        selectedPayments: List<Percentage>,
        totalAmount: Double
    ): List<Split> {
        return selectedPayments.map { payment ->
            Split(
                userId = payment.id,
                username = payment.name,
                percentage = payment.percentage.round2Decimals(),
                amount = ((totalAmount * payment.percentage) / 100).round2Decimals()
            )
        }
    }

    private fun Double.round2Decimals() = (this * 100).roundToInt() / 100.0
}