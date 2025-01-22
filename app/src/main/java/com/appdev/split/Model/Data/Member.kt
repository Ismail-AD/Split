package com.appdev.split.Model.Data

data class Member(
    val id: String,
    val name: String,
    var isSelected: Boolean = false,
    var imageUrl: String? = ""
)
