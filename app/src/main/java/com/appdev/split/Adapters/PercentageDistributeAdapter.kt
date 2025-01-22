package com.appdev.split.Adapters

import android.icu.util.Currency
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.appdev.split.Model.Data.PaymentDistribute
import com.appdev.split.Model.Data.Percentage
import com.appdev.split.R
import com.appdev.split.databinding.PercentageItemBinding
import com.bumptech.glide.Glide

class PercentageDistributeAdapter(
    val currency: String,
    private val payments: List<Percentage>,
    private val totalAmount: Double,
    private val onPercentageChanged: (Double) -> Unit
) : RecyclerView.Adapter<PercentageDistributeAdapter.PaymentViewHolder>() {

    inner class PaymentViewHolder(private val binding: PercentageItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(payment: Percentage) {
            binding.apply {
                tvName.text = payment.name
                etPercentage.hint = "0"
                Glide.with(binding.root.context).load(payment.imageUrl).error(R.drawable.profile_imaage)
                    .placeholder(R.drawable.profile_imaage)
                    .into(binding.ivProfile)
                // Show calculated amount based on percentage
                tvAmount.text = "${currency}${payment.amount.formatAmount()}"

                etPercentage.onTextChanged { text ->
                    val percentage = text.toDoubleOrNull() ?: 0.0
                    payment.percentage = percentage
                    // Calculate amount based on percentage
                    payment.amount = (percentage / 100.0) * totalAmount

                    // Update amount display
                    tvAmount.text = "${currency}${payment.amount.formatAmount()}"

                    // Notify fragment of total percentage
                    onPercentageChanged(payments.sumOf { it.percentage })
                }
            }
        }
    }

    private fun Double.formatAmount(): String {
        return String.format("%.2f", this)
    }

    private fun EditText.onTextChanged(afterTextChanged: (String) -> Unit) {
        this.doAfterTextChanged { editable ->
            afterTextChanged.invoke(editable.toString())
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentViewHolder {
        val binding = PercentageItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PaymentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PaymentViewHolder, position: Int) {
        holder.bind(payments[position])
    }

    override fun getItemCount() = payments.size
}