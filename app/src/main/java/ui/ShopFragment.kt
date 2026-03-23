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
        ShopItem(1, "шляпа\nковбоя", 500, R.drawable.hat, "hat"),
        ShopItem(2, "очки\nкрутые", 300, R.drawable.glasses, "glasses"),
        ShopItem(3, "усы\nмодные", 200, R.drawable.mustache, "other")
    )

    private var currentItems = allShopItems.toList()
    private lateinit var adapter: ShopAdapter
    private lateinit var tvCurrency: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_shop, container, false)
        val btnCheat = root.findViewById<Button>(R.id.btnCheatCode)
        btnCheat.setOnClickListener {
            val prefs = requireActivity().getSharedPreferences("AppPrefs", android.content.Context.MODE_PRIVATE)
            val currentBalance = prefs.getInt("USER_CURRENCY", 0)
            prefs.edit().putInt("USER_CURRENCY", currentBalance + 5000).apply()
            updateBalanceUI()
            android.widget.Toast.makeText(requireContext(), "Вы читер!", android.widget.Toast.LENGTH_SHORT).show()
        }
        tvCurrency = root.findViewById(R.id.tvCurrency)
        updateBalanceUI()
        val recyclerView = root.findViewById<RecyclerView>(R.id.shop_recycler)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        adapter = ShopAdapter(currentItems) { item ->
            handlePurchase(item)
        }
        recyclerView.adapter = adapter
        setupFilters(root)
        return root
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