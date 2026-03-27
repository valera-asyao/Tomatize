package ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tomatize.R
import com.example.tomatize.ShopItem
import com.example.tomatize.ShopStorage
import com.example.tomatize.UserData

class ShopFragment : Fragment() {

    companion object {
        private const val PREFS_NAME = "AppPrefs"
        private const val KEY_CURRENCY = "USER_CURRENCY"
        private const val KEY_OWNED_ITEMS = "OWNED_ITEMS"

        private const val KEY_EQUIPPED_HAT = "EQUIPPED_HAT"
        private const val KEY_EQUIPPED_GLASSES = "EQUIPPED_GLASSES"
        private const val KEY_EQUIPPED_OTHER = "EQUIPPED_OTHER"
    }

    private lateinit var adapter: ShopAdapter
    private lateinit var tvCurrency: TextView
    private lateinit var filterAll: TextView
    private lateinit var filterHats: TextView
    private lateinit var filterGlasses: TextView

    private val allShopItems: List<ShopItem>
        get() = UserData.allShopItems

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_shop, container, false)

        tvCurrency = root.findViewById(R.id.tvCurrency)
        filterAll = root.findViewById(R.id.filterAll)
        filterHats = root.findViewById(R.id.filterHats)
        filterGlasses = root.findViewById(R.id.filterGlasses)

        val btnCheat = root.findViewById<Button>(R.id.btnCheatCode)
        val recyclerView = root.findViewById<RecyclerView>(R.id.shop_recycler)

        adapter = ShopAdapter(
            items = allShopItems,
            isOwned = { ShopStorage.isOwned(requireContext(), it.id) },
            isEquipped = { ShopStorage.isEquipped(requireContext(), it) },
            onItemClick = { item ->
                if (ShopStorage.isOwned(requireContext(), item.id)) {
                    if (ShopStorage.isEquipped(requireContext(), item)) {
                        ShopStorage.unequipType(requireContext(), item.type)
                        Toast.makeText(requireContext(), "Снято: ${item.name}", Toast.LENGTH_SHORT).show()
                    } else {
                        ShopStorage.equipItem(requireContext(), item)
                        Toast.makeText(requireContext(), "Надето: ${item.name}", Toast.LENGTH_SHORT).show()
                    }
                    adapter.notifyDataSetChanged()
                } else {
                    handlePurchase(item)
                }
            }
        )

        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        recyclerView.adapter = adapter

        btnCheat.setOnClickListener {
            ShopStorage.addBalance(requireContext(), 5000)
            updateBalanceUI()
        }

        setupFilters()
        updateBalanceUI()

        return root
    }

    private fun onShopItemClick(item: ShopItem) {
        if (isOwned(item.id)) {
            toggleEquip(item)
        } else {
            buyItem(item)
        }
    }

    private fun buyItem(item: ShopItem) {
        val balance = getBalance()

        if (balance < item.price) {
            Toast.makeText(requireContext(), "Недостаточно средств!", Toast.LENGTH_SHORT).show()
            return
        }

        val newOwnedIds = getOwnedItemIds().toMutableSet()
        newOwnedIds.add(item.id)

        getPrefs().edit()
            .putInt(KEY_CURRENCY, balance - item.price)
            .putString(KEY_OWNED_ITEMS, newOwnedIds.joinToString(","))
            .apply()

        updateBalanceUI()
        adapter.notifyDataSetChanged()
        Toast.makeText(requireContext(), "Куплено: ${item.name}", Toast.LENGTH_SHORT).show()
    }

    private fun toggleEquip(item: ShopItem) {
        val currentEquippedId = getEquippedItemId(item.type)

        if (currentEquippedId == item.id) {
            setEquippedItemId(item.type, null)
            Toast.makeText(requireContext(), "Снято: ${item.name}", Toast.LENGTH_SHORT).show()
        } else {
            setEquippedItemId(item.type, item.id)
            Toast.makeText(requireContext(), "Надето: ${item.name}", Toast.LENGTH_SHORT).show()
        }

        adapter.notifyDataSetChanged()
    }

    private fun setupFilters() {
        filterAll.setOnClickListener {
            adapter.updateItems(allShopItems)
            updateFilterVisuals(filterAll)
        }

        filterHats.setOnClickListener {
            adapter.updateItems(allShopItems.filter { it.type == "hat" })
            updateFilterVisuals(filterHats)
        }

        filterGlasses.setOnClickListener {
            adapter.updateItems(allShopItems.filter { it.type == "glasses" })
            updateFilterVisuals(filterGlasses)
        }
    }

    private fun updateFilterVisuals(selectedView: TextView) {
        val filters = listOf(filterAll, filterHats, filterGlasses)

        filters.forEach { filter ->
            if (filter == selectedView) {
                filter.setBackgroundResource(R.drawable.bg_pill_red)
            } else {
                filter.setBackgroundResource(R.drawable.bg_pill_dark)
            }
        }
    }

    private fun updateBalanceUI() {
        tvCurrency.text = getBalance().toString()
    }

    private fun getPrefs() =
        requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun getBalance(): Int {
        return getPrefs().getInt(KEY_CURRENCY, 0)
    }

    private fun addCurrency(amount: Int) {
        val newBalance = getBalance() + amount
        getPrefs().edit().putInt(KEY_CURRENCY, newBalance).apply()
    }

    private fun getOwnedItemIds(): Set<Int> {
        val raw = getPrefs().getString(KEY_OWNED_ITEMS, "") ?: ""

        if (raw.isBlank()) return emptySet()

        return raw.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .toSet()
    }

    private fun isOwned(itemId: Int): Boolean {
        return getOwnedItemIds().contains(itemId)
    }

    private fun getEquippedKey(type: String): String {
        return when (type) {
            "hat" -> KEY_EQUIPPED_HAT
            "glasses" -> KEY_EQUIPPED_GLASSES
            else -> KEY_EQUIPPED_OTHER
        }
    }

    private fun getEquippedItemId(type: String): Int? {
        val value = getPrefs().getInt(getEquippedKey(type), -1)
        return if (value == -1) null else value
    }

    private fun setEquippedItemId(type: String, itemId: Int?) {
        getPrefs().edit()
            .putInt(getEquippedKey(type), itemId ?: -1)
            .apply()
    }
    private fun handlePurchase(item: ShopItem) {
        if (ShopStorage.isOwned(requireContext(), item.id)) {
            Toast.makeText(requireContext(), "Предмет уже куплен", Toast.LENGTH_SHORT).show()
            return
        }

        val success = ShopStorage.buyItem(requireContext(), item)

        if (success) {
            updateBalanceUI()
            adapter.notifyDataSetChanged()
            Toast.makeText(requireContext(), "Куплено: ${item.name}", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Недостаточно средств", Toast.LENGTH_SHORT).show()
        }
    }
}
