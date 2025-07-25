package com.nuerovent.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nuerovent.databinding.ItemControlBinding
import com.nuerovent.model.ControlItem

class ControlAdapter(
    private val items: List<ControlItem>,
    private val onToggle: (position: Int, isChecked: Boolean) -> Unit
) : RecyclerView.Adapter<ControlAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemControlBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ControlItem) {
            binding.controlLabel.text = item.label
            binding.controlSubText.text = item.subText


            binding.controlSwitch.setOnCheckedChangeListener(null)
            binding.controlSwitch.isChecked = item.isChecked


            binding.controlSwitch.setOnCheckedChangeListener { _, isChecked ->
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onToggle(adapterPosition, isChecked)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemControlBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
