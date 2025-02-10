package com.appdev.split.Model.Data

data class MySpending(
    val id: String,
    val month: String,
    val year: String,
    val totalAmountSpend: Double,
) {
    constructor() : this("", "", "", 0.0)
}
