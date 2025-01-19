package com.appdev.split.Adapters

import android.view.LayoutInflater
import android.widget.EditText
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.appdev.split.Model.Data.PaymentDistribute
import com.appdev.split.databinding.PaymentItemBinding
import android.view.ViewGroup



class PaymentDistributeAdapter(
    private val payments: List<PaymentDistribute>,
    private val onAmountChanged: (Double) -> Unit
) : RecyclerView.Adapter<PaymentDistributeAdapter.PaymentViewHolder>() {

    inner class PaymentViewHolder(private val binding: PaymentItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(payment: PaymentDistribute) {
            binding.apply {
                tvName.text = payment.name
                etAmount.hint = "0.00"
//                ivProfile.setBackgroundResource(R.drawable.circle_background)

                // Using custom extension function
                etAmount.onTextChanged { text ->
                    payment.amount = text.toDoubleOrNull() ?: 0.0
                    onAmountChanged(payments.sumOf { it.amount })
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


