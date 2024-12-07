package com.appdev.split.Adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.appdev.split.Model.Data.ExpenseRecord
import com.appdev.split.databinding.ItemRecentBillBinding
import kotlin.random.Random

class AllFriendExpenseAdapter(
    private val expenses: List<ExpenseRecord>, // Change to ExpenseRecord
    val navigate: (ExpenseRecord) -> Unit
) : RecyclerView.Adapter<AllFriendExpenseAdapter.BillViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BillViewHolder {
        val binding = ItemRecentBillBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BillViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BillViewHolder, position: Int) {
        val expense = expenses[position]
        holder.bind(expense)
    }

    override fun getItemCount(): Int = expenses.size

    inner class BillViewHolder(private val binding: ItemRecentBillBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(expense: ExpenseRecord) { // Changed from Bill to ExpenseRecord
            binding.billName.text = expense.title // Assuming ExpenseRecord has a 'name' field
            binding.billDate.text = expense.date // Assuming ExpenseRecord has a 'date' field
            binding.billAmount.text = expense.amount.toString() // Assuming ExpenseRecord has 'amount'

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
