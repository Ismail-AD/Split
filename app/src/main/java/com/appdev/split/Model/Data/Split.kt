package com.appdev.split.Model.Data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Split(
    val userId: String,
    val username:String,
    val amount: Double = 0.0,  // Used for EQUAL and UNEQUAL
    val percentage: Double = 0.0,  // Used for PERCENTAGE
): Parcelable