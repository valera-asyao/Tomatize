package ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tomatize.R
import com.example.tomatize.ShopItem

class ShopFragment : Fragment() {

    private val shopItems = listOf(
        ShopItem(1, "Hat", 10, R.drawable.hat),
        ShopItem(2, "Glasses", 15, R.drawable.glasses),
        ShopItem(3, "Mustache", 20, R.drawable.mustache)
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_shop, container, false)

        val recyclerView = root.findViewById<RecyclerView>(R.id.shop_recycler)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        recyclerView.adapter = ShopAdapter(shopItems)

        return root
    }
}