package ui

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tomatize.R
import com.example.tomatize.ShopItem
import com.example.tomatize.UserData
import kotlin.math.roundToInt

class ProfileFragment : Fragment() {

    private lateinit var mascotOverlayContainer: FrameLayout
    private lateinit var ownedItemsRecycler: RecyclerView
    private lateinit var inventoryAdapter: InventoryAdapter
    private lateinit var filterContainer: LinearLayout
    private var selectedType: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        mascotOverlayContainer = view.findViewById(R.id.mascot_overlay_container)
        ownedItemsRecycler = view.findViewById(R.id.owned_items_recycler)
        filterContainer = view.findViewById(R.id.filterContainer)

        setupInventory()
        setupFilters()
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

    private fun setupFilters() {
        filterContainer.removeAllViews()

        (listOf<String?>(null) + UserData.shopTypes).forEach { type ->
            filterContainer.addView(createFilterView(type))
        }

        updateFilterVisuals()
    }

    private fun createFilterView(type: String?): TextView {
        val filterTag = type ?: "all"

        return TextView(requireContext()).apply {
            tag = filterTag
            text = UserData.typeLabel(type)
            setTextColor(android.graphics.Color.WHITE)
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(24), dp(10), dp(24), dp(10))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dp(12)
            }
            setOnClickListener {
                selectedType = type
                refreshInventory()
                updateFilterVisuals()
            }
        }
    }

    private fun updateFilterVisuals() {
        for (index in 0 until filterContainer.childCount) {
            val filterView = filterContainer.getChildAt(index)
            val isSelected = filterView.tag == (selectedType ?: "all")

            filterView.setBackgroundResource(
                if (isSelected) R.drawable.bg_pill_red else R.drawable.bg_pill_dark
            )
        }
    }

    private fun refreshInventory() {
        val ownedIds = ShopStorage.getOwnedItemIds(requireContext())
        val ownedItems = UserData.allShopItems.filter { it.id in ownedIds }
        
        val filteredItems = if (selectedType == null) {
            ownedItems
        } else {
            ownedItems.filter { it.type == selectedType }
        }

        inventoryAdapter.updateItems(filteredItems)
    }

    private fun updateMascot() {
        val equippedItems = UserData.shopTypes
            .mapNotNull { type -> ShopStorage.getEquippedItemId(requireContext(), type) }
            .mapNotNull(UserData::findItemById)

        MascotOverlayRenderer.render(requireContext(), mascotOverlayContainer, equippedItems)
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).roundToInt()

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
