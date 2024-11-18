package com.appdev.split.Model.Data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class TransactionItem(
    val name: String, val price: Double,
    val quantity: Int
) : Parcelable {
    val total: Double
        get() = price * quantity
}

