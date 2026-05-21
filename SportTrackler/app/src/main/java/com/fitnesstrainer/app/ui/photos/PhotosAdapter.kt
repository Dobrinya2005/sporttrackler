package com.fitnesstrainer.app.ui.photos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.fitnesstrainer.app.data.model.ProgressPhoto
import com.fitnesstrainer.app.databinding.ItemPhotoBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class PhotosAdapter(
    private val onClick: (ProgressPhoto) -> Unit,
    private val onSelectionChanged: (List<ProgressPhoto>) -> Unit = {}
) : ListAdapter<ProgressPhoto, PhotosAdapter.VH>(DIFF) {

    private val selected = mutableListOf<ProgressPhoto>()
    val isSelecting get() = selected.isNotEmpty()
    fun getSelected(): List<ProgressPhoto> = selected.toList()

    fun clearSelection() {
        selected.clear()
        notifyDataSetChanged()
        onSelectionChanged(selected)
    }

    inner class VH(private val b: ItemPhotoBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(photo: ProgressPhoto) {
            val url = photo.thumbnailUrl ?: photo.photoUrl
            Glide.with(b.root).load(url).centerCrop()
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(b.ivPhoto)

            val date = try {
                LocalDate.parse(photo.photoDate).format(DateTimeFormatter.ofPattern("dd.MM.yy"))
            } catch (_: Exception) { photo.photoDate.take(10) }
            b.tvDate.text = date

            val isSelected = selected.contains(photo)
            b.overlaySelected.visibility = if (isSelected) android.view.View.VISIBLE else android.view.View.GONE
            b.tvCheck.visibility         = if (isSelected) android.view.View.VISIBLE else android.view.View.GONE

            b.root.setOnClickListener {
                if (isSelecting) toggleSelect(photo)
                else onClick(photo)
            }
            b.root.setOnLongClickListener {
                toggleSelect(photo)
                true
            }
        }

        private fun toggleSelect(photo: ProgressPhoto) {
            if (selected.contains(photo)) {
                selected.remove(photo)
            } else if (selected.size < 2) {
                selected.add(photo)
            }
            notifyDataSetChanged()
            onSelectionChanged(selected.toList())
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemPhotoBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ProgressPhoto>() {
            override fun areItemsTheSame(a: ProgressPhoto, b: ProgressPhoto) = a.photoId == b.photoId
            override fun areContentsTheSame(a: ProgressPhoto, b: ProgressPhoto) = a == b
        }
    }
}
