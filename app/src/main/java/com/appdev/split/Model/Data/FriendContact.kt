package com.appdev.split.Model.Data

import android.os.Parcelable
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
data class FriendContact(
    val friendId:String,
    val contact: String,
    val name: String,
    val profileImageUrl: String? = null
) : Parcelable{
    constructor():this("","","",null)
}
