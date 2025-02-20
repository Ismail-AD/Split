package com.appdev.split.Model.Data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MySpending(
    val id: String="",
    val month: String="",
    val year: String="",
    val totalAmountSpend: Double=0.0,
):Parcelable {
    constructor() : this("", "", "", 0.0)
}
