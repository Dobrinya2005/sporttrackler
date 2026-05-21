package com.fitnesstrainer.app.ui.onboarding

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.fitnesstrainer.app.databinding.ItemOnboardingSlideBinding

data class OnboardingSlide(val emoji: String, val title: String, val subtitle: String)

class OnboardingAdapter(private val slides: List<OnboardingSlide>) :
    RecyclerView.Adapter<OnboardingAdapter.SlideVH>() {

    inner class SlideVH(val b: ItemOnboardingSlideBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(slide: OnboardingSlide) {
            b.tvEmoji.text    = slide.emoji
            b.tvTitle.text    = slide.title
            b.tvSubtitle.text = slide.subtitle
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        SlideVH(ItemOnboardingSlideBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: SlideVH, position: Int) = holder.bind(slides[position])
    override fun getItemCount() = slides.size
}
