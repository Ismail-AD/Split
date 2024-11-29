package com.appdev.split.Model.Data

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.appdev.split.Utils.Converters
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "friends")
data class Friend(
    @PrimaryKey val contact: String,  // Treat contact as the primary key
    val name: String,
    val profileImageUrl: String? = null,
    @TypeConverters(Converters::class) // Use the converter
    val expenseRecords: MutableList<ExpenseRecord> = mutableListOf(),
    var totalAmountOwedNoGroup: Float = 0f
) : Parcelable {
    constructor() : this("", "")
}
