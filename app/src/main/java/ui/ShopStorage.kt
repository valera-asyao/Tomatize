package ui

import android.content.Context
import com.example.tomatize.ShopItem
import com.example.tomatize.UserData

object ShopStorage {

    const val MAX_BALANCE = 100000

    private fun prefs(context: Context) =
        context.getSharedPreferences(UserData.PREFS_NAME, Context.MODE_PRIVATE)

    fun getBalance(context: Context): Int {
        return prefs(context).getInt(UserData.KEY_CURRENCY, 0)
    }

    fun addBalance(context: Context, amount: Int): Int {
        val currentBalance = getBalance(context)
        val newValue = minOf(currentBalance + amount, MAX_BALANCE)
        prefs(context).edit().putInt(UserData.KEY_CURRENCY, newValue).apply()
        return newValue
    }

    fun setBalance(context: Context, value: Int) {
        prefs(context).edit()
            .putInt(UserData.KEY_CURRENCY, minOf(maxOf(0, value), MAX_BALANCE))
            .apply()
    }

    fun spendBalance(context: Context, amount: Int) {
        setBalance(context, getBalance(context) - amount)
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
        val editor = prefs(context).edit()

        if (item.type == "other") {
            UserData.shopTypes.forEach { type ->
                if (type != "mustache" && type != "other") {
                    editor.putInt(UserData.keyForType(type), -1)
                }
            }
        } else if (item.type != "mustache") {
            editor.putInt(UserData.keyForType("other"), -1)
        }

        editor.putInt(UserData.keyForType(item.type), item.id)
        editor.apply()
    }

    fun unequipType(context: Context, type: String) {
        prefs(context).edit()
            .putInt(keyForType(type), -1)
            .apply()
    }

    fun isEquipped(context: Context, item: ShopItem): Boolean {
        return getEquippedItemId(context, item.type) == item.id
    }

    fun clearSkins(context: Context) {
        val editor = prefs(context).edit()
            .remove(UserData.KEY_OWNED_ITEMS)

        UserData.shopTypes.forEach { type ->
            editor.putInt(keyForType(type), -1)
        }

        editor.apply()
    }

    fun clearMoneyAndSkins(context: Context) {
        prefs(context).edit()
            .putInt(UserData.KEY_CURRENCY, 0)
            .remove(UserData.KEY_OWNED_ITEMS)
            .apply()

        clearSkins(context)
    }
}
