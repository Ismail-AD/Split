package com.appdev.split.Adapters

import android.annotation.SuppressLint
import android.icu.util.Currency
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
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

    inner class PaymentViewHolder(val binding: PaymentItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("ClickableViewAccessibility")
        fun bind(payment: PaymentDistribute) {

            binding.apply {
                tvName.text = payment.name

                if (payment.amount > 0) {
                    etAmount.setText(payment.amount.toString())
                } else {
                    etAmount.hint = "0.00"
                }
                curr.text = currency
                Glide.with(binding.root.context).load(payment.imageUrl)
                    .error(R.drawable.profile_imaage)
                    .placeholder(R.drawable.profile_imaage)
                    .into(binding.ivProfile)
//                etAmount.requestFocus()
                etAmount.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) {
                        Log.d(
                            "PaymentDistributeAdapter",
                            "etAmount gained focus for: ${payment.name}"
                        )
                    } else {
                        Log.d(
                            "PaymentDistributeAdapter",
                            "etAmount lost focus for: ${payment.name}"
                        )
                    }
                }

                // Log touch events
                etAmount.setOnTouchListener { _, event ->
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        Log.d("PaymentDistributeAdapter", "etAmount touched for: ${payment.name}")
                    }
                    false // Allow the touch event to proceed as usual
                }

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


