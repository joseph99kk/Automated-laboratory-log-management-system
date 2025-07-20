import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nuerovent.databinding.LayoutHomeItemBinding
import com.nuerovent.model.HomeItem

class ItemAdapter() : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {

    private var items: List<HomeItem> = listOf()

    @SuppressLint("NotifyDataSetChanged")
    fun setDataList(data: List<HomeItem>) {
        items = data
        notifyDataSetChanged()
    }

    inner class ItemViewHolder(val binding: LayoutHomeItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding =
            LayoutHomeItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val data = items[position]
        with(holder.binding) {
            // Convert label resource ID (Int) to actual string:
            title.text = holder.itemView.context.getString(data.label)
            textTemperature.text = data.reading
            icon.setImageResource(data.imageResId)
        }
    }


    override fun getItemCount(): Int = items.size
}
