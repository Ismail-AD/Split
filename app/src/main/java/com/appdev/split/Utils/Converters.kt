package com.appdev.split.Utils

import androidx.room.TypeConverter
import com.appdev.split.Model.Data.ExpenseRecord
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {

    @TypeConverter
    fun fromExpenseRecordList(expenseRecords: MutableList<ExpenseRecord>): String {
        val gson = Gson()
        return gson.toJson(expenseRecords) // Convert list to JSON string
    }

    @TypeConverter
    fun toExpenseRecordList(data: String): MutableList<ExpenseRecord> {
        val gson = Gson()
        val type = object : TypeToken<MutableList<ExpenseRecord>>() {}.type
        return gson.fromJson(data, type) // Convert JSON string back to list
    }
}