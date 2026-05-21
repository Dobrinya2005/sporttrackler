package com.fitnesstrainer.app.ui.trainer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.fitnesstrainer.app.databinding.ItemClientWeightBinding

class ClientWeightAdapter : RecyclerView.Adapter<ClientWeightAdapter.VH>() {

    private var items = listOf<ClientWeight>()

    fun submitList(list: List<ClientWeight>) {
        items = list
        notifyDataSetChanged()
    }

    inner class VH(val b: ItemClientWeightBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: ClientWeight) {
            val name = "${item.client.firstName} ${item.client.lastName}"
            b.tvClientName.text = name
            b.tvInitials.text = name.trim().split(" ")
                .filter { it.isNotEmpty() }.take(2)
                .joinToString("") { it.first().uppercaseChar().toString() }

            val m = item.latestMeasurement
            if (m?.weightKg != null) {
                b.tvWeight.text = if (m.weightKg % 1 == 0.0) m.weightKg.toInt().toString()
                                  else "%.1f".format(m.weightKg)
                b.tvWeightDate.text = "замер ${m.measuredAt.take(10)}"
            } else {
                b.tvWeight.text = "—"
                b.tvWeightDate.text = "нет данных"
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemClientWeightBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])
    override fun getItemCount() = items.size
}
