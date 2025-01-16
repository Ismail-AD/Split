package com.appdev.split.Model.Data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class GroupMetaData(
    val groupId: String? = "",
    val image: String? = "",
    val title: String,
    val groupType: String
): Parcelable {
    constructor() : this("","","","")
}
