package ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tomatize.R
import com.example.tomatize.ShopItem

class ShopAdapter(
    private var items: List<ShopItem>,
    private val isOwned: (ShopItem) -> Boolean,
    private val isEquipped: (ShopItem) -> Boolean,
    private val onItemClick: (ShopItem) -> Unit
) : RecyclerView.Adapter<ShopAdapter.ShopViewHolder>() {

    class ShopViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.itemIcon)
        val name: TextView = view.findViewById(R.id.itemName)
        val price: TextView = view.findViewById(R.id.itemPrice)
        val container: View = view.findViewById(R.id.itemCardContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShopViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shop, parent, false)
        return ShopViewHolder(view)
    }

    override fun onBindViewHolder(holder: ShopViewHolder, position: Int) {
        val item = items[position]
        holder.icon.setImageResource(item.iconRes)
        holder.name.text = item.name
        holder.price.text = item.price.toString()
        holder.container.setOnClickListener {
            onItemClick(item)
        }
        when {
            isEquipped(item) -> holder.price.text = "Надето"
            isOwned(item) -> holder.price.text = "Куплено"
            else -> holder.price.text = item.price.toString()
        }
    }

    override fun getItemCount() = items.size
    // Метод для обновления списка при использовании фильтров
    fun updateItems(newItems: List<ShopItem>) {
        this.items = newItems
        notifyDataSetChanged()
    }
}