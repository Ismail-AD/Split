package com.appdev.split.Utils

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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

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
        val outputFormat = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
        val date = inputFormat.parse(inputDate) // Parse input string to Date
        return outputFormat.format(date!!) // Format Date to desired string
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