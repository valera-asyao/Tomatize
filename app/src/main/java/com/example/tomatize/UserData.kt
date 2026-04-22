package com.example.tomatize

import java.util.Locale

object UserData {

    const val PREFS_NAME = "AppPrefs"
    const val KEY_CURRENCY = "USER_CURRENCY"
    const val KEY_OWNED_ITEMS = "OWNED_ITEMS"

    private const val EQUIPPED_PREFIX = "EQUIPPED_"

    private val excludedDrawableNames = setOf(
        "app_icon",
        "rounded_card",
        "tomagochi",
        "tomato_icon",
        "room",
        "hat"
    )

    private val excludedDrawablePrefixes = listOf(
        "arrow_",
        "bg_",
        "day_",
        "ic_",
        "nav_"
    )

    private val legacyItemIds = mapOf(
        "glasses" to 2,
        "moustache" to 3,
        "apron2024" to 4
    )

    private val itemNameOverrides = mapOf(
        "apron2024" to "Фартук 2024",
        "chefsapron" to "Фартук",
        "chefshat" to "Колпак",
        "chefssuit" to "Костюм шефа",
        "chefssuitwhite" to "Белый костюм",
        "funnyhat" to "Смешная шляпа",
        "glasses" to "Очки",
        "hat" to "Шляпа",
        "moustache" to "Усы",
        "pants" to "Штаны",
        "strangehat" to "Странная шляпа",
        "sweatshirt" to "Свитшот",
        "tshirt" to "Футболка",
        "witchshat" to "Ведьмина шляпа"
    )

    val allShopItems: List<ShopItem> by lazy {
        R.drawable::class.java.fields
            .asSequence()
            .filter { it.type == Int::class.javaPrimitiveType }
            .mapNotNull { field -> buildShopItem(field.name, field.getInt(null)) }
            .sortedWith(compareBy<ShopItem>({ overlaySortOrder(it.type) }, { it.name }))
            .toList()
    }

    val shopTypes: List<String>
        get() = allShopItems
            .map { it.type }
            .distinct()
            .sortedBy(::overlaySortOrder)

    fun keyForType(type: String): String =
        EQUIPPED_PREFIX + type.uppercase(Locale.ROOT)

    fun findItemById(itemId: Int?): ShopItem? =
        allShopItems.firstOrNull { it.id == itemId }

    fun filterItems(type: String?): List<ShopItem> =
        if (type == null) allShopItems else allShopItems.filter { it.type == type }

    fun typeLabel(type: String?): String = when (type) {
        null -> "Все"
        "hat" -> "Шапки"
        "glasses" -> "Очки"
        "mustache" -> "Усы"
        "clothes" -> "Одежда"
        else -> "Другое"
    }

    fun overlaySortOrder(type: String): Int = when (type) {
        "clothes" -> 0
        "other" -> 1
        "mustache" -> 2
        "glasses" -> 3
        "hat" -> 4
        else -> 5
    }

    private fun buildShopItem(resourceName: String, resourceId: Int): ShopItem? {
        if (!isWearableDrawable(resourceName)) {
            return null
        }

        val type = inferType(resourceName)

        return ShopItem(
            id = legacyItemIds[resourceName] ?: resourceName.hashCode(),
            resourceName = resourceName,
            name = itemNameOverrides[resourceName] ?: resourceName.toTitleCase(),
            price = priceForType(type),
            iconRes = resourceId,
            overlayRes = resourceId,
            type = type
        )
    }

    private fun isWearableDrawable(resourceName: String): Boolean {
        if (resourceName in excludedDrawableNames) {
            return false
        }

        return excludedDrawablePrefixes.none(resourceName::startsWith)
    }

    private fun inferType(resourceName: String): String {
        return when {
            "glass" in resourceName -> "glasses"
            "moustache" in resourceName || "mustache" in resourceName -> "mustache"
            "hat" in resourceName -> "hat"
            hasClothesKeyword(resourceName) -> "clothes"
            else -> "other"
        }
    }

    private fun hasClothesKeyword(resourceName: String): Boolean {
        val clothesKeywords = listOf(
            "apron",
            "dress",
            "hoodie",
            "jacket",
            "pants",
            "shirt",
            "suit",
            "sweatshirt"
        )

        return clothesKeywords.any(resourceName::contains)
    }

    private fun priceForType(type: String): Int = when (type) {
        "mustache" -> 200
        "clothes" -> 400
        else -> 300
    }

    private fun String.toTitleCase(): String {
        val normalized = replace('_', ' ')
            .replace(Regex("(\\d+)"), " $1")
            .trim()

        return normalized.replaceFirstChar { char ->
            if (char.isLowerCase()) {
                char.titlecase(Locale.ROOT)
            } else {
                char.toString()
            }
        }
    }
}
