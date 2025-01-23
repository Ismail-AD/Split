package com.appdev.split.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.appdev.split.Model.Data.Split
import com.appdev.split.Model.Data.SplitType
import com.appdev.split.R
import com.appdev.split.databinding.BillDetailItemLayoutBinding

class SplitDetailsAdapter : RecyclerView.Adapter<SplitDetailsAdapter.SplitViewHolder>() {
    private var splits: List<Split> = emptyList()
    private var splitType: String = SplitType.EQUAL.name
    private var totalAmount: Double = 0.0
    private var Currency: String = ""

    fun updateData(newSplits: List<Split>, type: String, total: Double, curr: String) {
        splits = newSplits
        splitType = type
        totalAmount = total
        Currency = curr
        notifyDataSetChanged()
    }

    inner class SplitViewHolder(private val binding: BillDetailItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(split: Split) {
            binding.apply {
                username.text = split.username
                splitDetails.text = "paid"
                currency.text = Currency
                amount.text = split.amount.toString()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SplitViewHolder {
        val binding = BillDetailItemLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SplitViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SplitViewHolder, position: Int) {
        holder.bind(splits[position])
    }

    override fun getItemCount(): Int = splits.size
}