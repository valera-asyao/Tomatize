package ui

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
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
        
        val owned = isOwned(item)
        val equipped = isEquipped(item)

        if (owned || equipped) {
            holder.price.visibility = View.GONE
            

            val matrix = ColorMatrix()
            matrix.setSaturation(0f)
            holder.icon.colorFilter = ColorMatrixColorFilter(matrix)
            holder.container.alpha = 0.5f
            holder.container.isEnabled = false
            

        } else {
            holder.price.visibility = View.VISIBLE
            holder.price.text = item.price.toString()
            

            holder.icon.colorFilter = null
            holder.container.alpha = 1.0f
            holder.container.isEnabled = true
            holder.container.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    override fun getItemCount() = items.size

    fun updateItems(newItems: List<ShopItem>) {
        this.items = newItems
        notifyDataSetChanged()
    }
}
