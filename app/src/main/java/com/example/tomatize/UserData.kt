package com.example.tomatize

object UserData {

    const val PREFS_NAME = "AppPrefs"
    const val KEY_CURRENCY = "USER_CURRENCY"
    const val KEY_OWNED_ITEMS = "OWNED_ITEMS"

    const val KEY_EQUIPPED_HAT = "EQUIPPED_HAT"
    const val KEY_EQUIPPED_GLASSES = "EQUIPPED_GLASSES"
    const val KEY_EQUIPPED_MUSTACHE = "EQUIPPED_MUSTACHE"
    const val KEY_EQUIPPED_CLOTHES = "EQUIPPED_CLOTHES"
    const val KEY_EQUIPPED_OTHER = "EQUIPPED_OTHER"

    val allShopItems = listOf(
        ShopItem(2, "Очки крутые", 300, R.drawable.glasses, R.drawable.glasses, "glasses"),
        ShopItem(3, "Усы модные", 200, R.drawable.moustache, R.drawable.moustache, "mustache"),
        ShopItem(4, "Фартук", 400, R.drawable.apron2024, R.drawable.apron2024, "clothes")
    )
}