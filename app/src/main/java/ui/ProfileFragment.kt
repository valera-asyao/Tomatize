package ui

import android.content.Context
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
import com.example.tomatize.UserData

class ProfileFragment : Fragment() {

    private lateinit var accessoryOverlay: ImageView
    private lateinit var ownedItemsRecycler: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        
        /* кнопка настроек
        val settingsIcon = view.findViewById<ImageView>(R.id.settings_icon)
        settingsIcon.setOnClickListener {
            showSettingsMenu(it)
        }
        */

        accessoryOverlay = view.findViewById(R.id.accessory_overlay)
        ownedItemsRecycler = view.findViewById(R.id.owned_items_recycler)

        setupInventory()
        updateMascot()
        
        return view
    }

    
    private fun getOwnedItemsIds(): List<Int> {
        val prefs = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val ownedString = prefs.getString("OWNED_ITEMS", "") ?: ""
        return ownedString.split(",")
            .filter { it.isNotBlank() }
            .mapNotNull { it.toIntOrNull() }
    }

    private fun getEquippedItemId(): Int? {
        val prefs = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val id = prefs.getInt("EQUIPPED_ITEM", -1)
        return if (id == -1) null else id
    }

    private fun setEquippedItemId(id: Int?) {
        val prefs = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("EQUIPPED_ITEM", id ?: -1).apply()
    }

    private fun setupInventory() {
        val ownedIds = getOwnedItemsIds()
        // Используем список предметов из UserData как мастер-список
        val ownedItems = UserData.allShopItems.filter { ownedIds.contains(it.id) }
        
        ownedItemsRecycler.layoutManager = GridLayoutManager(requireContext(), 3)
        ownedItemsRecycler.adapter = InventoryAdapter(ownedItems) { item ->
            val currentEquipped = getEquippedItemId()
            if (currentEquipped == item.id) {
                setEquippedItemId(null)
            } else {
                setEquippedItemId(item.id)
            }
            updateMascot()
            ownedItemsRecycler.adapter?.notifyDataSetChanged()
        }
    }

    private fun updateMascot() {
        val equippedId = getEquippedItemId()
        if (equippedId != null) {
            val item = UserData.allShopItems.find { it.id == equippedId }
            if (item != null) {
                accessoryOverlay.setImageResource(item.iconRes)
                accessoryOverlay.visibility = View.VISIBLE
            } else {
                accessoryOverlay.visibility = View.GONE
            }
        } else {
            accessoryOverlay.visibility = View.GONE
        }
    }

    /*   кнопка настроек
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
    */

    private inner class InventoryAdapter(
        private val items: List<ShopItem>,
        private val onItemClick: (ShopItem) -> Unit
    ) : RecyclerView.Adapter<InventoryAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val icon: ImageView = view.findViewById(R.id.itemIcon)
            val name: TextView = view.findViewById(R.id.itemName)
            val container: View = view.findViewById(R.id.itemCardContainer)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_shop, parent, false)
            view.findViewById<View>(R.id.itemPrice)?.visibility = View.GONE
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.icon.setImageResource(item.iconRes)
            holder.name.text = item.name
            
            if (getEquippedItemId() == item.id) {
                holder.container.setBackgroundResource(R.drawable.bg_selected_item)
            } else {
                holder.container.setBackgroundResource(android.R.color.transparent)
            }

            holder.itemView.setOnClickListener { onItemClick(item) }
        }

        override fun getItemCount() = items.size
    }
}