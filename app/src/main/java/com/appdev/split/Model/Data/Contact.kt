package com.appdev.split.Model.Data

data class Contact(
    val name: String,
    val number: String,
    val imageUrl: String? = null,
    val isFriend: Boolean = false
)
