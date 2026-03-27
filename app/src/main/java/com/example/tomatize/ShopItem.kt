package com.example.tomatize

data class ShopItem(
    val id: Int,
    val name: String,
    val price: Int,
    val iconRes: Int,      // иконка в карточке магазина / инвентаря
    val overlayRes: Int,   // картинка, которая надевается на маскота
    val type: String       // hat / glasses / mustache / clothes
)