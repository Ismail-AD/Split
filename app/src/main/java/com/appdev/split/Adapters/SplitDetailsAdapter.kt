package com.appdev.split.Adapters

import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.appdev.split.Model.Data.Split
import com.appdev.split.Model.Data.SplitType
import com.appdev.split.R
import com.appdev.split.Utils.Utils
import com.appdev.split.databinding.BillDetailItemLayoutBinding

class SplitDetailsAdapter : RecyclerView.Adapter<SplitDetailsAdapter.SplitViewHolder>() {
    private var splits: List<Split> = emptyList()
    private var splitType: String = SplitType.EQUAL.name
    private var totalAmount: Double = 0.0
    private var Currency: String = ""
    private var currentUserId: String = ""
    private var paidBy: String = ""
    private var settledBy: List<String> = emptyList()
    private var allSettled: Boolean = false

    fun updateData(
        newSplits: List<Split>,
        type: String,
        total: Double,
        curr: String,
        userId: String = "",
        paidBy: String,
        settledBy: List<String>
    ) {
        splits = newSplits
        splitType = type
        totalAmount = total
        Currency = curr
        currentUserId = userId
        this.paidBy = paidBy
        this.settledBy = settledBy

        allSettled = newSplits
            .filter { it.userId != paidBy }
            .all { settledBy.contains(it.userId) }

        notifyDataSetChanged()
    }

    inner class SplitViewHolder(private val binding: BillDetailItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(split: Split) {
            val context = binding.root.context
            val builder = SpannableStringBuilder()

            // Format username - add "(You)" if current user
            val displayName = if (split.userId == currentUserId) {
                "${split.username} (You)"
            } else {
                split.username
            }

            // Append username with gray color
            val usernameStart = 0
            builder.append(displayName)
            val usernameEnd = builder.length

            val grayColor = ContextCompat.getColor(context, R.color.gray)
            builder.setSpan(
                ForegroundColorSpan(grayColor),
                usernameStart,
                usernameEnd,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            // Append status text (paid, owes, settled up)
            val isPayer = split.userId == paidBy
            val hasSettled = settledBy.contains(split.userId)

            val statusText = when {
                isPayer -> " paid "
                allSettled && !isPayer -> " settled up "
                hasSettled -> " settled up "
                else -> " owes "
            }

            // Set color for status text
            val statusStart = builder.length
            builder.append(statusText)
            val statusEnd = builder.length

            if (isPayer || hasSettled || (allSettled && !isPayer)) {
                val greenColor = ContextCompat.getColor(context, R.color.green)
                builder.setSpan(
                    ForegroundColorSpan(greenColor),
                    statusStart,
                    statusEnd,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            } else {
                builder.setSpan(
                    ForegroundColorSpan(grayColor),
                    statusStart,
                    statusEnd,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            // Append currency and amount
            val currencyStr = Utils.extractCurrencyCode(Currency)
            val amountStr = String.format("%.2f", split.amount)

            val amountStart = builder.length
            builder.append(currencyStr)
            builder.append(amountStr)
            val amountEnd = builder.length

            // Apply bold styling to currency and amount
            builder.setSpan(
                StyleSpan(Typeface.BOLD),
                amountStart,
                amountEnd,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            // Get text color from billWordColor style
            val billWordColor = getColorFromStyle(context, R.style.billWordColor)

            builder.setSpan(
                ForegroundColorSpan(billWordColor),
                amountStart,
                amountEnd,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            // Set the complete text to the TextView
            binding.billDetailText.text = builder
        }

        /**
         * Extract text color from a style
         * This works with day/night themes as it gets the current theme's color
         */
        private fun getColorFromStyle(context: Context, styleResId: Int): Int {
            val typedValue = TypedValue()

            // Create a themed context with the style applied
            val themedContext = context.obtainStyledAttributes(styleResId, intArrayOf(android.R.attr.textColor))
            val textColor = themedContext.getColor(0, ContextCompat.getColor(context, android.R.color.black))
            themedContext.recycle()

            return textColor
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