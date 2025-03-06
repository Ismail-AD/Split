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
            imageRes = R.drawable.firston,
            title = "Overwhelmed with bills? We’ve got you!",
            description = "Easily split expenses with friends and say goodbye to the hassle of tracking who owes what."
        ),
        OnboardingItem(
            R.drawable.secondon,
            "Fair & Simple Bill Splitting!",
            "Effortlessly divide expenses, track payments, and keep friendships stress-free."
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