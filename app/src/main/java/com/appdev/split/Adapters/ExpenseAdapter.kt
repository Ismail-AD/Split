package com.appdev.split.Adapters


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.appdev.split.Model.Data.GroupMetaData
import com.appdev.split.R
import com.appdev.split.databinding.ItemExpenseBinding


class ExpenseAdapter(private val items: List<GroupMetaData>, val navigate: (GroupMetaData) -> Unit) :
    RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

    inner class ExpenseViewHolder(private val binding: ItemExpenseBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: GroupMetaData) {
            binding.expenseTitle.text = item.title
            binding.Icon.setImageResource(getIconForGroupType(item.groupType))
            binding.expenseDetail.text = item.groupType
            binding.parent.setOnClickListener {
                navigate(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val binding = ItemExpenseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ExpenseViewHolder(binding)
    }

    fun getIconForGroupType(groupType: String): Int {
        return when (groupType.lowercase()) {
            "trip" -> R.drawable.airplane
            "home" -> R.drawable.home
            "couple" -> R.drawable.love
            else -> R.drawable.home
        }
    }


    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        holder.bind(items[position])

    }

    override fun getItemCount() = items.size
}
