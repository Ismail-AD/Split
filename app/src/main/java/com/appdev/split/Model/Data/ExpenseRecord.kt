package com.appdev.split.Model.Data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ExpenseRecord(
    val paidAmount: Float,
    val lentAmount: Float,
    val borrowedAmount: Float = 0f,
    val date: String
) : Parcelable