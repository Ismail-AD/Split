package com.appdev.split.Adapters

import android.icu.util.Currency
import android.view.LayoutInflater
import android.widget.EditText
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.appdev.split.Model.Data.PaymentDistribute
import com.appdev.split.databinding.PaymentItemBinding
import android.view.ViewGroup
import com.appdev.split.R
import com.bumptech.glide.Glide


class PaymentDistributeAdapter(
    private val currency: String,
    private val payments: List<PaymentDistribute>,
    private val onAmountChanged: (Double) -> Unit
) : RecyclerView.Adapter<PaymentDistributeAdapter.PaymentViewHolder>() {

    inner class PaymentViewHolder(private val binding: PaymentItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(payment: PaymentDistribute) {
            binding.apply {
                tvName.text = payment.name
                etAmount.hint = "0.00"
                curr.text = currency
                Glide.with(binding.root.context).load(payment.imageUrl).error(R.drawable.profile_imaage)
                    .placeholder(R.drawable.profile_imaage)
                    .into(binding.ivProfile)

//                ivProfile.setBackgroundResource(R.drawable.circle_background)

                // Using custom extension function
                etAmount.doAfterTextChanged { text ->
                    if (text != null) {
                        val newAmount = text.toString().toDoubleOrNull() ?: 0.0
                        if (payment.amount != newAmount) {
                            payment.amount = newAmount
                            onAmountChanged(payments.sumOf { it.amount })
                        }
                    }
                }
            }
        }
    }

    fun EditText.onTextChanged(afterTextChanged: (String) -> Unit) {
        this.doAfterTextChanged { editable ->
            afterTextChanged.invoke(editable.toString())
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentViewHolder {
        val binding = PaymentItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PaymentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PaymentViewHolder, position: Int) {
        holder.bind(payments[position])
    }

    override fun getItemCount() = payments.size
}


