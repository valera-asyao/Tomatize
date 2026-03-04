package ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.tomatize.R
import com.example.tomatize.ShopItem
import com.example.tomatize.UserData

class ShopFragment : Fragment() {

    private val shopItems = listOf(
        ShopItem(1, "Hat", 10, R.drawable.tomato_icon),
        ShopItem(2, "Glasses", 15, R.drawable.tomato_icon),
        ShopItem(3, "Mustache", 20, R.drawable.tomato_icon)
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_shop, container, false)

        val shopContainer = root.findViewById<LinearLayout>(R.id.shop_container)

        for (item in shopItems) {
            val itemView = inflater.inflate(R.layout.item_shop, shopContainer, false)

            val icon = itemView.findViewById<ImageView>(R.id.itemIcon)
            val name = itemView.findViewById<TextView>(R.id.itemName)
            val button = itemView.findViewById<Button>(R.id.buyButton)

            icon.setImageResource(item.iconRes)
            name.text = "${item.name} — ${item.price} 🍅"

            button.setOnClickListener {
                if (UserData.ownedItems.contains(item.id)) {
                    Toast.makeText(requireContext(), "Already owned", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (UserData.tomatoes >= item.price) {
                    UserData.tomatoes -= item.price
                    UserData.ownedItems.add(item.id)
                    Toast.makeText(requireContext(), "Purchased!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Not enough tomatoes", Toast.LENGTH_SHORT).show()
                }
            }

            shopContainer.addView(itemView)
        }

        return root
    }
}