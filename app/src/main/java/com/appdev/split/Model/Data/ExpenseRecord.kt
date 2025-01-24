package com.appdev.split.Model.Data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ExpenseRecord(
    val currency: String = "",
    val date: String = "",
    val description: String = "",
    val expenseCategory: String = "",
    val id: String = "", // Firestore document ID
    val paidBy: String = "", // User ID who paid
    val splitType: String = SplitType.EQUAL.name, // Updated to String to match Firebase
    val splits: List<Split> = listOf(),
    val timeStamp: Long = 0L,
    val title: String = "",
    val totalAmount: Double = 0.0
) : Parcelable {
    constructor() : this("", "", "", "", "", "", "", listOf(), 0L, "", 0.0)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ExpenseRecord

        return id == other.id &&
                title == other.title &&
                description == other.description &&
                totalAmount == other.totalAmount &&
                currency == other.currency &&
                date == other.date &&
                splits == other.splits &&
                splitType == other.splitType
    }

}

