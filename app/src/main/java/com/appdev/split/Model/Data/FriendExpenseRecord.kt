package com.appdev.split.Model.Data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class FriendExpenseRecord(
    val currency: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val description: String = "",
    val friendId: String = "",
    val expenseCategory: String = "",
    val id: String = "", // Firestore document ID
    val paidBy: String = "", // User ID who paid
    val splitType: String = SplitType.EQUAL.name, // Updated to String to match Firebase
    val splits: List<Split> = listOf(),
    val participantIds: List<String> = listOf(),
    val timeStamp: Long = 0L,
    val title: String = "",
    val totalAmount: Double = 0.0,
    val settledBy: List<String> = emptyList() // ✅ New field: list of user IDs who settled

) : Parcelable {
    constructor() : this("", "", "", "", "", "","", "", "", listOf(), listOf(), 0L, "", 0.0)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FriendExpenseRecord

        return id == other.id &&
                title == other.title &&
                description == other.description &&
                totalAmount == other.totalAmount &&
                currency == other.currency &&
                startDate == other.startDate &&
                endDate == other.endDate &&
                splits == other.splits &&
                splitType == other.splitType &&
                participantIds == other.participantIds &&
                settledBy == other.settledBy // ✅ Include in equality check
    }

    override fun hashCode(): Int {
        var result = currency.hashCode()
        result = 31 * result + startDate.hashCode()
        result = 31 * result + endDate.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + friendId.hashCode()
        result = 31 * result + expenseCategory.hashCode()
        result = 31 * result + id.hashCode()
        result = 31 * result + paidBy.hashCode()
        result = 31 * result + splitType.hashCode()
        result = 31 * result + splits.hashCode()
        result = 31 * result + participantIds.hashCode()
        result = 31 * result + timeStamp.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + totalAmount.hashCode()
        return result
    }

}

