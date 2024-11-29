package com.appdev.split.Model.Data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ExpenseRecord(
    val paidAmount: Float,
    val lentAmount: Float,
    val date: String
) : Parcelable