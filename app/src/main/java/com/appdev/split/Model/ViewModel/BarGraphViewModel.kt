package com.appdev.split.Model.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.appdev.split.Graph.CustomBarGraph
import com.appdev.split.Repository.Repo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BarGraphViewModel @Inject constructor(
    var repo: Repo,
    val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : ViewModel() {
    private val _barData = MutableLiveData<List<CustomBarGraph.BarData>>()
    val barData: LiveData<List<CustomBarGraph.BarData>> = _barData

    fun setBarData(data: List<CustomBarGraph.BarData>) {
        _barData.value = data
    }

    fun getBarData(): List<CustomBarGraph.BarData> {
        return _barData.value ?: emptyList()
    }
}