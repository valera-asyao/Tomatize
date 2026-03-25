package com.example.tomatize

import com.example.tomatize.R

object UserData {

    var tomatoes = 50

    val ownedItems = mutableListOf<Int>()
    
    var equippedItem: Int? = null

    val allShopItems = listOf(
        ShopItem(1, "Hat", 10, R.drawable.hat, "accessory"),
        ShopItem(2, "Glasses", 15, R.drawable.glasses, "accessory"),
        ShopItem(3, "Mustache", 20, R.drawable.mustache, "accessory")
    )

}