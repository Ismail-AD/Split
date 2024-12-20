package com.appdev.split.Adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.appdev.split.Model.Data.Bill
import com.appdev.split.Model.Data.ExpenseRecord
import com.appdev.split.Utils.Utils
import com.appdev.split.databinding.ItemRecentBillBinding
import kotlin.random.Random

class BillAdapter(
    private val groupedExpenses: Map<String, List<ExpenseRecord>>, // Updated to receive grouped data by contact
    val navigate: (List<ExpenseRecord>, String) -> Unit // Navigate with a list of ExpenseRecord
) : RecyclerView.Adapter<BillAdapter.BillViewHolder>() {

    private val contactIds: List<String> = groupedExpenses.keys.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BillViewHolder {
        val binding =
            ItemRecentBillBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BillViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BillViewHolder, position: Int) {
        val contactId = contactIds[position]
        val expenseRecords = groupedExpenses[contactId] ?: emptyList()
        holder.bind(contactId, expenseRecords)
    }

    override fun getItemCount(): Int = contactIds.size

    inner class BillViewHolder(private val binding: ItemRecentBillBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(contactId: String, expenses: List<ExpenseRecord>) {
            // Here we use the first expense to display the contact info
            val expense = expenses.firstOrNull()

            if (expense != null) {
                binding.billName.text = expense.title // Assuming ExpenseRecord has a 'title' field
                binding.billDate.text = expense.date // Assuming ExpenseRecord has a 'date' field
                val amount: Float
                val label: String
                binding.currency.text = Utils.extractCurrencyCode(expense.currency)
                if (expense.borrowedAmount > 0f) {
                    amount = expense.paidAmount - expense.borrowedAmount
                    label = "You borrowed "
                    binding.amount.text = if (expense.borrowedAmount == expense.paidAmount) {
                        expense.paidAmount.toString()
                    } else {
                        amount.toString()
                    }
                } else {
                    amount = expense.paidAmount - expense.lentAmount
                    label = "You lent "
                    binding.amount.text = if (expense.lentAmount == expense.paidAmount) {
                        expense.paidAmount.toString()
                    } else {
                        amount.toString()
                    }
                }

                binding.youBorrowOrLent.text = label
            }

            val lightColor = getLightRandomColor()
            binding.cardView.setCardBackgroundColor(lightColor)

            // Generate a slightly darker version of the light color for the image tint
            val darkColor = getDarkerColor(lightColor)
            binding.imageView.setColorFilter(darkColor)

            // Set the click listener to navigate with the list of ExpenseRecord for this contactId
            binding.parent.setOnClickListener {
                navigate(expenses, contactId) // Send the list of expenses for this contact
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