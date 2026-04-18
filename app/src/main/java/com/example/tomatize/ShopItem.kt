package com.example.tomatize

data class ShopItem(
    val id: Int,
    val resourceName: String,
    val name: String,
    val price: Int,
    val iconRes: Int,
    val overlayRes: Int,
    val type: String
)