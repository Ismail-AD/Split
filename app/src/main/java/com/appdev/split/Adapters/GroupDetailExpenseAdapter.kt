package com.appdev.split.Adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.appdev.split.Model.Data.ExpenseRecord
import com.appdev.split.Utils.Utils
import com.appdev.split.Utils.Utils.getCurrentUserId
import com.appdev.split.databinding.ItemRecentBillBinding
import kotlin.random.Random

class GroupDetailExpenseAdapter(
    private val expenses: List<ExpenseRecord>,
    val navigate: (ExpenseRecord) -> Unit
) : RecyclerView.Adapter<GroupDetailExpenseAdapter.BillViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BillViewHolder {
        val binding =
            ItemRecentBillBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BillViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BillViewHolder, position: Int) {
        val expense = expenses[position]
        holder.bind(expense)
    }

    override fun getItemCount(): Int = expenses.size

    inner class BillViewHolder(private val binding: ItemRecentBillBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(expense: ExpenseRecord) {
            binding.billName.text = expense.title
            binding.billDate.text = expense.startDate + "-" + expense.endDate
            binding.currency.text = Utils.extractCurrencyCode(expense.currency)

            val currentUserId = getCurrentUserId()
            val userSplit = expense.splits.find { it.userId == currentUserId }
            val isPayer = expense.paidBy == currentUserId

            // Check if all non-payers have settled
            val allNonPayersSettled = expense.splits
                .filter { it.userId != expense.paidBy }
                .all { expense.settledBy.contains(it.userId) }

            // Check if current user has settled
            val hasUserSettled = expense.settledBy.contains(currentUserId)

            // Display logic based on user role and settlement status
            when {
                // Case 1: All splits settled - everyone should see "Expense Settled"
                allNonPayersSettled -> {
                    binding.apply {
                        youBorrowOrLent.text = "Expense Settled"
                        amount.visibility = View.GONE
                        currency.visibility = View.GONE
                    }
                }
                // Case 2: Current user is payer but not all have settled
                isPayer && !allNonPayersSettled -> {
                    val totalOthersShouldPay = expense.splits
                        .filter { it.userId != currentUserId }
                        .sumOf { it.amount }

                    binding.apply {
                        youBorrowOrLent.text = "You lent "
                        amount.text = String.format("%.2f", totalOthersShouldPay)
                        amount.visibility = View.VISIBLE
                        currency.visibility = View.VISIBLE
                    }
                }
                // Case 3: Current user has settled their part
                hasUserSettled -> {
                    binding.apply {
                        youBorrowOrLent.text = "You settled up"
                        amount.visibility = View.GONE
                        currency.visibility = View.GONE
                    }
                }
                // Case 4: Current user is not payer and has not settled
                userSplit != null -> {
                    binding.apply {
                        youBorrowOrLent.text = "You borrowed "
                        amount.text = String.format("%.2f", userSplit.amount)
                        amount.visibility = View.VISIBLE
                        currency.visibility = View.VISIBLE
                    }
                }
                // Case 5: User is not involved in this expense
                else -> {
                    binding.apply {
                        youBorrowOrLent.text = "Not involved"
                        amount.visibility = View.GONE
                        currency.visibility = View.GONE
                    }
                }
            }

            val lightColor = getLightRandomColor()
            binding.cardView.setCardBackgroundColor(lightColor)

            // Generate a slightly darker version of the light color for the image tint
            val darkColor = getDarkerColor(lightColor)
            binding.imageView.setColorFilter(darkColor)
            binding.parent.setOnClickListener {
                navigate(expense)
            }
        }

        private fun getLightRandomColor(): Int {
            val red = Random.nextInt(150, 256)
            val green = Random.nextInt(150, 256)
            val blue = Random.nextInt(150, 256)
            return Color.rgb(red, green, blue)
        }

        // Generate a darker version of the given color
        private fun getDarkerColor(color: Int): Int {
            val factor = 0.8 // Adjust this factor to make the color darker or lighter
            val red = (Color.red(color) * factor).toInt().coerceAtLeast(0)
            val green = (Color.green(color) * factor).toInt().coerceAtLeast(0)
            val blue = (Color.blue(color) * factor).toInt().coerceAtLeast(0)
            return Color.rgb(red, green, blue)
        }
    }
}