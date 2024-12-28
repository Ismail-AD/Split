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
    @PrimaryKey var contact: String,  // Changed to var
    var name: String,                 // Changed to var
    var profileImageUrl: String? = null,
    @TypeConverters(Converters::class)
    var expenseRecords: MutableList<ExpenseRecord> = mutableListOf()
) : Parcelable {
    // No-arg constructor
    constructor() : this("", "", null)
}
