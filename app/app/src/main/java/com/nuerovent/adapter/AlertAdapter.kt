package com.nuerovent.adapter


import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nuerovent.databinding.LayoutAlertBinding
import com.nuerovent.model.Alert

class AlertAdapter(private var items: List<Alert>) :
    RecyclerView.Adapter<AlertAdapter.ItemViewHolder>() {

    inner class ItemViewHolder(val binding: LayoutAlertBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding =
            LayoutAlertBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val alert = items[position]
        holder.binding.icon.setImageResource(alert.iconResId)
        holder.binding.title.text = alert.title
        holder.binding.description.text = alert.description
        holder.binding.timeAgo.text = alert.timeAgo
    }

    override fun getItemCount(): Int = items.size

    fun updateAlerts(newAlerts: List<Alert>) {
        items = newAlerts
        notifyDataSetChanged()
    }
}
