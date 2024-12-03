package com.appdev.split.Model.Data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


data class ExpenseUserInput(
    var title: String = "",
    var description: String = "",
    var amount: Float = 0f
)
