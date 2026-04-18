package com.example.tomatize

import android.content.Context

object ShopStorage {

    private fun prefs(context: Context) =
        context.getSharedPreferences(UserData.PREFS_NAME, Context.MODE_PRIVATE)

    fun getBalance(context: Context): Int {
        return prefs(context).getInt(UserData.KEY_CURRENCY, 0)
    }

    fun addBalance(context: Context, amount: Int) {
        val newValue = getBalance(context) + amount
        prefs(context).edit().putInt(UserData.KEY_CURRENCY, newValue).apply()
    }

    fun spendBalance(context: Context, amount: Int) {
        val newValue = getBalance(context) - amount
        prefs(context).edit().putInt(UserData.KEY_CURRENCY, newValue).apply()
    }

    fun getOwnedItemIds(context: Context): Set<Int> {
        val raw = prefs(context).getString(UserData.KEY_OWNED_ITEMS, "") ?: ""

        if (raw.isBlank()) return emptySet()

        return raw.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .toSet()
    }

    fun isOwned(context: Context, itemId: Int): Boolean {
        return getOwnedItemIds(context).contains(itemId)
    }

    fun buyItem(context: Context, item: ShopItem): Boolean {
        if (isOwned(context, item.id)) return false

        val balance = getBalance(context)
        if (balance < item.price) return false

        val owned = getOwnedItemIds(context).toMutableSet()
        owned.add(item.id)

        val ownedString = owned.joinToString(",")

        prefs(context).edit()
            .putInt(UserData.KEY_CURRENCY, balance - item.price)
            .putString(UserData.KEY_OWNED_ITEMS, ownedString)
            .apply()

        return true
    }

    private fun keyForType(type: String): String = UserData.keyForType(type)

    fun getEquippedItemId(context: Context, type: String): Int? {
        val value = prefs(context).getInt(keyForType(type), -1)
        return if (value == -1) null else value
    }

    fun equipItem(context: Context, item: ShopItem) {
        prefs(context).edit()
            .putInt(keyForType(item.type), item.id)
            .apply()
    }

    fun unequipType(context: Context, type: String) {
        prefs(context).edit()
            .putInt(keyForType(type), -1)
            .apply()
    }

    fun isEquipped(context: Context, item: ShopItem): Boolean {
        return getEquippedItemId(context, item.type) == item.id
    }
}