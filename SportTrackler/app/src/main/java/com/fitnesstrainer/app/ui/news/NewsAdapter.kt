package com.fitnesstrainer.app.ui.news

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.fitnesstrainer.app.data.model.NewsItem
import com.fitnesstrainer.app.databinding.ItemNewsCardBinding

class NewsAdapter : ListAdapter<NewsItem, NewsAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemNewsCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    class VH(private val b: ItemNewsCardBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: NewsItem) {
            b.tvNewsTitle.text  = item.title
            b.tvNewsSource.text = item.source.uppercase()

            if (!item.imageUrl.isNullOrBlank()) {
                Glide.with(b.root).load(item.imageUrl)
                    .centerCrop().into(b.ivNewsImage)
            } else {
                b.ivNewsImage.setImageResource(android.R.color.transparent)
            }

            b.root.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.url))
                b.root.context.startActivity(intent)
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<NewsItem>() {
            override fun areItemsTheSame(a: NewsItem, b: NewsItem) = a.url == b.url
            override fun areContentsTheSame(a: NewsItem, b: NewsItem) = a == b
        }
    }
}
