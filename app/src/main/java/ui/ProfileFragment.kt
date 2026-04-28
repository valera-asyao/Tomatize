package ui

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tomatize.R
import com.example.tomatize.ShopItem
import com.example.tomatize.UserData

class ProfileFragment : Fragment() {

    private companion object {
        private const val PROFILE_CLOTHES_OFFSET_DP = 4f
    }

    private lateinit var mascotOverlayContainer: FrameLayout
    private lateinit var ownedItemsRecycler: RecyclerView
    private lateinit var inventoryAdapter: InventoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        mascotOverlayContainer = view.findViewById(R.id.mascot_overlay_container)
        ownedItemsRecycler = view.findViewById(R.id.owned_items_recycler)

        setupInventory()
        updateMascot()

        return view
    }

    override fun onResume() {
        super.onResume()
        updateMascot()
        if (::inventoryAdapter.isInitialized) {
            refreshInventory()
        }
    }

    private fun setupInventory() {
        ownedItemsRecycler.layoutManager = GridLayoutManager(requireContext(), 2)
        inventoryAdapter = InventoryAdapter(emptyList()) { item ->
            if (ShopStorage.isEquipped(requireContext(), item)) {
                ShopStorage.unequipType(requireContext(), item.type)
            } else {
                ShopStorage.equipItem(requireContext(), item)
            }

            updateMascot()
            refreshInventory()
        }
        ownedItemsRecycler.adapter = inventoryAdapter
        refreshInventory()
    }

    private fun refreshInventory() {
        val ownedIds = ShopStorage.getOwnedItemIds(requireContext())
        val ownedItems = UserData.allShopItems.filter { it.id in ownedIds }

        inventoryAdapter.updateItems(ownedItems)
    }

    private fun updateMascot() {
        val equippedItems = UserData.shopTypes
            .mapNotNull { type -> ShopStorage.getEquippedItemId(requireContext(), type) }
            .mapNotNull(UserData::findItemById)

        MascotOverlayRenderer.render(
            requireContext(),
            mascotOverlayContainer,
            equippedItems,
            clothesOffsetDp = PROFILE_CLOTHES_OFFSET_DP
        )
    }

    private inner class InventoryAdapter(
        private var items: List<ShopItem>,
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
            val isEquipped = ShopStorage.isEquipped(requireContext(), item)

            holder.icon.setImageResource(item.iconRes)
            holder.price.visibility = View.GONE

            val params = holder.name.layoutParams as RelativeLayout.LayoutParams
            
            if (isEquipped) {
                holder.name.text = "НАДЕТО"
                holder.name.gravity = Gravity.CENTER
                holder.name.setTextColor(android.graphics.Color.parseColor("#FFFFFF"))
                
                params.removeRule(RelativeLayout.START_OF)
                params.removeRule(RelativeLayout.LEFT_OF)
                params.width = RelativeLayout.LayoutParams.MATCH_PARENT
            } else {
                holder.name.text = item.name
                holder.name.gravity = Gravity.CENTER
                holder.name.setTextColor(android.graphics.Color.WHITE)
                
                params.width = RelativeLayout.LayoutParams.MATCH_PARENT
            }
            holder.name.layoutParams = params

            holder.container.alpha = if (isEquipped) 0.8f else 1f

            holder.itemView.setOnClickListener {
                onItemClick(item)
            }
        }

        override fun getItemCount(): Int = items.size

        fun updateItems(newItems: List<ShopItem>) {
            items = newItems
            notifyDataSetChanged()
        }
    }
}
