package com.appdev.split.Model.Data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Bill(
    val name: String,
    val date: String,
    val amount: Double,
    val personCount: String,
    val tax:Double=0.0,
    val listOfTransaction:List<TransactionItem>
): Parcelable
