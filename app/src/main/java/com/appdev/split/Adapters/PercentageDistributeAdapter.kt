package com.appdev.split.Adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.appdev.split.Model.Data.PaymentDistribute
import com.appdev.split.Model.Data.Percentage
import com.appdev.split.databinding.PercentageItemBinding
class PercentageDistributeAdapter(
    private val payments: List<Percentage>,
    private val totalAmount: Float,
    private val onPercentageChanged: (Float) -> Unit
) : RecyclerView.Adapter<PercentageDistributeAdapter.PaymentViewHolder>() {

    inner class PaymentViewHolder(private val binding: PercentageItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(payment: Percentage) {
            binding.apply {
                tvName.text = payment.name
                etPercentage.hint = "0"

                // Show calculated amount based on percentage
                tvAmount.text = "$${payment.amount.formatAmount()}"

                etPercentage.onTextChanged { text ->
                    val percentage = text.toFloatOrNull() ?: 0f
                    payment.percentage = percentage
                    // Calculate amount based on percentage
                    payment.amount = (percentage / 100f) * totalAmount

                    // Update amount display
                    tvAmount.text = "$${payment.amount.formatAmount()}"

                    // Notify fragment of total percentage
                    onPercentageChanged(payments.sumOf { it.percentage.toDouble() }.toFloat())
                }
            }
        }
    }

    private fun Float.formatAmount(): String {
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