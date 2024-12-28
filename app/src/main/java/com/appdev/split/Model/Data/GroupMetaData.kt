package com.appdev.split.Model.Data

data class GroupMetaData(
    val groupId: String? = "",
    val image: String? = "",
    val title: String,
    val groupType: String
){
    constructor() : this("","","","")
}
