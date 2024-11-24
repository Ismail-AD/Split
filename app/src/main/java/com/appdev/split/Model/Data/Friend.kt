package com.appdev.split.Model.Data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "friends")
data class Friend(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val profileImageUrl: String? = null
)
