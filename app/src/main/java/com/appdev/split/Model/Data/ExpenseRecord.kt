package com.appdev.split.Model.Data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ExpenseRecord(
    val amount: Float = 0f,
    val borrowedAmount: Float = 0f,
    val currency: String = "",
    val date: String = "",
    val description: String = "",
    val expenseFor: String = "",
    val expenseId: String = "",
    val lentAmount: Float = 0f,
    val paidAmount: Float = 0f,
    val timeStamp: Long = 0L,
    val title: String = ""
) : Parcelable