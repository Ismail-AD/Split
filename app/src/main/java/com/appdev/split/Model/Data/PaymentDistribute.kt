package com.appdev.split.Model.Data

data class PaymentDistribute(
    val id: String,
    val name: String,
    var amount: Double = 0.0,
    var imageUrl: String? = ""
)
