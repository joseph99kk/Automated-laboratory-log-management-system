package com.nuerovent.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nuerovent.databinding.ItemDefaultConditionBinding
import com.nuerovent.model.DefaultConditionItem

class DefaultConditionAdapter(
    private val items: List<DefaultConditionItem>,
    private val onMenuAction: (position: Int, action: String) -> Unit  // callback for direct actions
) : RecyclerView.Adapter<DefaultConditionAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemDefaultConditionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: DefaultConditionItem) {
            binding.titleText.text = item.title
            binding.subText.text = item.subtext


            binding.root.setOnClickListener {
                when (item.title) {
                    "Target Temperature" -> onMenuAction(adapterPosition, "set_temp")
                    "Target Humidity" -> onMenuAction(adapterPosition, "set_humidity")
                    "Target Pressure" -> onMenuAction(adapterPosition, "set_pressure")
                    "Air Quality Threshold" -> onMenuAction(adapterPosition, "set_air_quality")
                }
            }


            binding.moreIcon.setOnClickListener(null)
            binding.moreIcon.visibility = android.view.View.GONE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDefaultConditionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
