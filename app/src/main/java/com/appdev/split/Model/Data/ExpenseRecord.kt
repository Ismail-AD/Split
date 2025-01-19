package com.appdev.split.Model.Data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ExpenseRecord(
    val id: String = "",  // Firestore document ID
    val totalAmount: Double = 0.0,
    val paidBy: String = "",  // User ID who paid
    val currency: String = "",
    val date: String = "",
    val description: String = "",
    val expenseCategory: String = "",
    val splitType: SplitType = SplitType.EQUAL,
    val splits: List<Split> = listOf(),
    val timeStamp: Long = 0L,
    val title: String = ""
) : Parcelable

