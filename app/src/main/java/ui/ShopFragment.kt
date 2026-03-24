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

class ShopFragment : Fragment() {
    private val allShopItems = listOf(
        ShopItem(1, "шляпа\nповара", 500, R.drawable.hat, "hat"),
        ShopItem(2, "очки\nкрутые", 300, R.drawable.glasses, "glasses"),
        ShopItem(3, "усы\nмодные", 200, R.drawable.mustache, "other")
    )

    private var currentItems = allShopItems.toList()
    private lateinit var adapter: ShopAdapter
    private lateinit var tvCurrency: TextView
    private lateinit var filterAll: TextView
    private lateinit var filterHats: TextView
    private lateinit var filterGlasses: TextView

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
        btnCheat.setOnClickListener {
            val prefs = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            val currentBalance = prefs.getInt("USER_CURRENCY", 0)
            prefs.edit().putInt("USER_CURRENCY", currentBalance + 5000).apply()
            updateBalanceUI()
            android.widget.Toast.makeText(requireContext(), "Валюта начислена! ✨", android.widget.Toast.LENGTH_SHORT).show()
        }
        adapter = ShopAdapter(allShopItems) { item ->
            handlePurchase(item)
        }
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        recyclerView.adapter = adapter

        setupFilters()
        updateBalanceUI()
        updateFilterVisuals(filterAll)

        return root
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
        filters.forEach { textView ->
            if (textView == selectedView) {
                textView.setBackgroundResource(R.drawable.bg_pill_red) // Активный - красный
            } else {
                textView.setBackgroundResource(R.drawable.bg_pill_dark) // Неактивный - темный
            }
        }
    }

    private fun handlePurchase(item: ShopItem) {
        val prefs = requireActivity().getSharedPreferences("AppPrefs", android.content.Context.MODE_PRIVATE)
        val balance = prefs.getInt("USER_CURRENCY", 0)
        val ownedItems = prefs.getString("OWNED_ITEMS", "")?.split(",")?.filter { it.isNotBlank() } ?: listOf()
        if (ownedItems.contains(item.id.toString())) {
            android.widget.Toast.makeText(requireContext(), "Предмет уже куплен!", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        if (balance >= item.price) {
            val newBalance = balance - item.price
            val newOwnedItems = if (ownedItems.isEmpty()) item.id.toString() else "${prefs.getString("OWNED_ITEMS", "")},${item.id}"
            prefs.edit()
                .putInt("USER_CURRENCY", newBalance)
                .putString("OWNED_ITEMS", newOwnedItems)
                .apply()

            updateBalanceUI()
            android.widget.Toast.makeText(requireContext(), "Куплено: ${item.name}", android.widget.Toast.LENGTH_SHORT).show()
        } else {
            android.widget.Toast.makeText(requireContext(), "Недостаточно средств!", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateBalanceUI() {
        val prefs = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val balance = prefs.getInt("USER_CURRENCY", 0)
        tvCurrency.text = balance.toString()
    }

    private fun setupFilters(root: View) {
        val filterAll = root.findViewById<TextView>(R.id.filterAll)
        val filterHats = root.findViewById<TextView>(R.id.filterHats)
        val filterGlasses = root.findViewById<TextView>(R.id.filterGlasses)
        filterAll.setOnClickListener {
            adapter.updateItems(allShopItems)
        }
        filterHats.setOnClickListener {
            adapter.updateItems(allShopItems.filter { it.type == "hat" })
        }
        filterGlasses.setOnClickListener {
            adapter.updateItems(allShopItems.filter { it.type == "glasses" })
        }
    }
}