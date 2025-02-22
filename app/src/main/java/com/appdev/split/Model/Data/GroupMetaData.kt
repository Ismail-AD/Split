package com.appdev.split.Model.Data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class GroupMetaData(
    val groupId: String? = "",
    val image: String? = "",
    val title: String,
    val groupType: String,
    val createdBy:String,
    val members: List<FriendContact> = emptyList(),
    val memberIds: List<String> = emptyList(),
): Parcelable {
    constructor() : this("","","","","", emptyList(), emptyList())
}
