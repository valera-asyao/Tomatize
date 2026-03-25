package ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tomatize.R
import com.example.tomatize.ShopItem
import com.example.tomatize.ShopStorage
import com.example.tomatize.UserData

class ProfileFragment : Fragment() {

    private lateinit var glassesOverlay: ImageView
    private lateinit var mustacheOverlay: ImageView
    private lateinit var clothesOverlay: ImageView
    private lateinit var ownedItemsRecycler: RecyclerView
    private lateinit var inventoryAdapter: InventoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        val settingsIcon = view.findViewById<ImageView>(R.id.settings_icon)
        settingsIcon.setOnClickListener { showSettingsMenu(it) }

        glassesOverlay = view.findViewById(R.id.glasses_overlay)
        mustacheOverlay = view.findViewById(R.id.mustache_overlay)
        clothesOverlay = view.findViewById(R.id.clothes_overlay)
        ownedItemsRecycler = view.findViewById(R.id.owned_items_recycler)

        setupInventory()
        updateMascot()

        return view
    }

    override fun onResume() {
        super.onResume()
        updateMascot()
        if (::inventoryAdapter.isInitialized) {
            inventoryAdapter.notifyDataSetChanged()
        }
    }

    private fun setupInventory() {
        val ownedIds = ShopStorage.getOwnedItemIds(requireContext())
        val ownedItems = UserData.allShopItems.filter { it.id in ownedIds }

        ownedItemsRecycler.layoutManager = GridLayoutManager(requireContext(), 3)
        inventoryAdapter = InventoryAdapter(ownedItems) { item ->
            if (ShopStorage.isEquipped(requireContext(), item)) {
                ShopStorage.unequipType(requireContext(), item.type)
            } else {
                ShopStorage.equipItem(requireContext(), item)
            }

            updateMascot()
            inventoryAdapter.notifyDataSetChanged()
        }
        ownedItemsRecycler.adapter = inventoryAdapter
    }

    private fun updateMascot() {
        updateOverlay(glassesOverlay, "glasses")
        updateOverlay(mustacheOverlay, "mustache")
        updateOverlay(clothesOverlay, "clothes")
    }

    private fun updateOverlay(imageView: ImageView, type: String) {
        val equippedId = ShopStorage.getEquippedItemId(requireContext(), type)
        val item = UserData.allShopItems.find { it.id == equippedId }

        if (item != null) {
            imageView.setImageResource(item.overlayRes)
            imageView.visibility = View.VISIBLE
        } else {
            imageView.visibility = View.GONE
        }
    }

    private fun showSettingsMenu(view: View) {
        val popup = PopupMenu(requireContext(), view)
        popup.menu.add(0, 1, 0, "Toggle Theme")

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    val currentMode = AppCompatDelegate.getDefaultNightMode()
                    if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    } else {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    }
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private inner class InventoryAdapter(
        private val items: List<ShopItem>,
        private val onItemClick: (ShopItem) -> Unit
    ) : RecyclerView.Adapter<InventoryAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val icon: ImageView = view.findViewById(R.id.itemIcon)
            val name: TextView = view.findViewById(R.id.itemName)
            val price: TextView = view.findViewById(R.id.itemPrice)
            val container: View = view.findViewById(R.id.itemCardContainer)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_shop, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]

            holder.icon.setImageResource(item.iconRes)
            holder.name.text = item.name
            holder.price.text = if (ShopStorage.isEquipped(requireContext(), item)) "Надето" else ""

            holder.container.alpha = if (ShopStorage.isEquipped(requireContext(), item)) 0.7f else 1f

            holder.itemView.setOnClickListener {
                onItemClick(item)
            }
        }

        override fun getItemCount(): Int = items.size
    }
}