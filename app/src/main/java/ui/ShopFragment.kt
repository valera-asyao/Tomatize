package ui

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tomatize.R
import com.example.tomatize.ShopItem
import com.example.tomatize.UserData
import kotlin.math.roundToInt

class ShopFragment : Fragment() {

    private lateinit var adapter: ShopAdapter
    private lateinit var tvCurrency: TextView
    private lateinit var filterContainer: LinearLayout
    private var selectedType: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_shop, container, false)

        tvCurrency = root.findViewById(R.id.tvCurrency)
        filterContainer = root.findViewById(R.id.filterContainer)

        val btnCheat = root.findViewById<Button>(R.id.btnCheatCode)
        val recyclerView = root.findViewById<RecyclerView>(R.id.shop_recycler)

        adapter = ShopAdapter(
            items = UserData.allShopItems,
            isOwned = { ShopStorage.isOwned(requireContext(), it.id) },
            isEquipped = { ShopStorage.isEquipped(requireContext(), it) },
            onItemClick = ::onShopItemClick
        )

        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        recyclerView.adapter = adapter

        btnCheat.setOnClickListener {
            ShopStorage.addBalance(requireContext(), 5000)
            updateBalanceUI()
        }

        setupFilters()
        updateVisibleItems()
        updateBalanceUI()

        return root
    }

    override fun onResume() {
        super.onResume()
        updateBalanceUI()
        updateVisibleItems()
    }

    private fun onShopItemClick(item: ShopItem) {
        if (ShopStorage.isOwned(requireContext(), item.id)) {
            toggleEquip(item)
        } else {
            handlePurchase(item)
        }
    }

    private fun toggleEquip(item: ShopItem) {
        val currentEquippedId = ShopStorage.getEquippedItemId(requireContext(), item.type)

        if (currentEquippedId == item.id) {
            ShopStorage.unequipType(requireContext(), item.type)
            Toast.makeText(
                requireContext(),
                "\u0421\u043d\u044f\u0442\u043e: ${item.name}",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            ShopStorage.equipItem(requireContext(), item)
            Toast.makeText(
                requireContext(),
                "\u041d\u0430\u0434\u0435\u0442\u043e: ${item.name}",
                Toast.LENGTH_SHORT
            ).show()
        }

        adapter.notifyDataSetChanged()
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
                updateVisibleItems()
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

    private fun updateVisibleItems() {
        adapter.updateItems(UserData.filterItems(selectedType))
    }

    private fun updateBalanceUI() {
        tvCurrency.text = ShopStorage.getBalance(requireContext()).toString()
    }

    private fun handlePurchase(item: ShopItem) {
        if (ShopStorage.isOwned(requireContext(), item.id)) {
            Toast.makeText(
                requireContext(),
                "\u041f\u0440\u0435\u0434\u043c\u0435\u0442 \u0443\u0436\u0435 \u043a\u0443\u043f\u043b\u0435\u043d",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val success = ShopStorage.buyItem(requireContext(), item)

        if (success) {
            updateBalanceUI()
            adapter.notifyDataSetChanged()
            Toast.makeText(
                requireContext(),
                "\u041a\u0443\u043f\u043b\u0435\u043d\u043e: ${item.name}",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                requireContext(),
                "\u041d\u0435\u0434\u043e\u0441\u0442\u0430\u0442\u043e\u0447\u043d\u043e \u0441\u0440\u0435\u0434\u0441\u0442\u0432",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).roundToInt()
}