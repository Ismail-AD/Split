package com.appdev.split.Adapters
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.appdev.split.Model.Data.TransactionItem
import com.appdev.split.databinding.ItemTransactionBinding

class TransactionItemAdapter(private val items: List<TransactionItem>) :
    RecyclerView.Adapter<TransactionItemAdapter.TransactionViewHolder>() {

    class TransactionViewHolder(private val binding: ItemTransactionBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: TransactionItem) {
            binding.tvItemName.text = item.name
            binding.tvItemPrice.text = "$${item.price}"
            binding.tvItemQuantity.text = "${item.quantity}x"
            binding.tvTotalPrice.text = "$${"%.2f".format(item.total)}"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
