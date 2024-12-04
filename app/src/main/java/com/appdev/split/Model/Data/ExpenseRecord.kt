package com.appdev.split.Model.Data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ExpenseRecord(
    val paidAmount: Float = 0f,
    val lentAmount: Float = 0f,
    val borrowedAmount: Float = 0f,
    val date: String = "",
    var title: String = "",
    var description: String = "",
    var amount: Float = 0f,
    val expenseFor:String="",
    val currency:String="",
) : Parcelable