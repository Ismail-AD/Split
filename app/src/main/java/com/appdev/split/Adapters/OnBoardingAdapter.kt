package com.appdev.split.Adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.appdev.split.Model.Data.OnboardingItem
import com.appdev.split.R
import com.appdev.split.databinding.ItemOnboardingBinding

class OnboardingAdapter : RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder>() {

    private val onboardingItems = listOf(
        OnboardingItem(
            imageRes = R.drawable.split_the_amount,
            title = "Who Pays What?",
            description = "Easily share expenses with friends individually or in groups using three different ways to split."
        ),
        OnboardingItem(
            R.drawable.history_check,
            "Where Did My Money Go?",
            "Track your monthly expenses with easy-to-read graphs and stay in control of your expenses."
        ),
        OnboardingItem(
            R.drawable.addfriends,
            "Add Friends, Split Fast!",
            "Add and manage friends for stress-free expense sharing."
        )
    )

    inner class OnboardingViewHolder(private val binding: ItemOnboardingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: OnboardingItem) {
            binding.apply {
                onboardingTitle.text = item.title
                onboardingDescription.text = item.description
                onboardingImage.setImageResource(item.imageRes)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
        val binding = ItemOnboardingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OnboardingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
        holder.bind(onboardingItems[position])
    }

    override fun getItemCount() = onboardingItems.size
}