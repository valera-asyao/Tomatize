package ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.tomatize.R
import com.example.tomatize.ShopItem
import com.example.tomatize.UserData

class ShopAdapter(private val items: List<ShopItem>) :
    RecyclerView.Adapter<ShopAdapter.ShopViewHolder>() {

    class ShopViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.itemIcon)
        val name: TextView = view.findViewById(R.id.itemName)
        val button: Button = view.findViewById(R.id.buyButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShopViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shop, parent, false)
        return ShopViewHolder(view)
    }

    override fun onBindViewHolder(holder: ShopViewHolder, position: Int) {
        val item = items[position]
        holder.icon.setImageResource(item.iconRes)
        holder.name.text = "${item.name} — ${item.price} 🍅"

        holder.button.setOnClickListener {
            val context = holder.itemView.context
            if (UserData.ownedItems.contains(item.id)) {
                Toast.makeText(context, "Already owned", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (UserData.tomatoes >= item.price) {
                UserData.tomatoes -= item.price
                UserData.ownedItems.add(item.id)
                Toast.makeText(context, "Purchased!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Not enough tomatoes", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun getItemCount() = items.size
}