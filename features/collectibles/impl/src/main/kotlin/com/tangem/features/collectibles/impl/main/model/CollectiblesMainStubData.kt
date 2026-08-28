package com.tangem.features.collectibles.impl.main.model

import java.math.BigDecimal

/** A gacha pack as the main screen needs it. */
internal data class CollectiblesPack(
    val id: String,
    val imageUrl: String,
    val title: String,
    val price: String,
    val buyback: String?,
    val categoryId: String,
)

/** Account balance as the header shows it. */
internal data class CollectiblesBalance(
    val fiat: BigDecimal,
    val crypto: String,
)

/** A category tab with the number of packs behind it. */
internal data class CollectiblesCategory(
    val id: String,
    val label: String,
    val count: Int,
)

/**
 * Stand-in for the catalogue the backend will serve.
 *
 * Image URLs are empty on purpose: the cards render their placeholder until there is a service to ask.
 */
internal object CollectiblesMainStubData {

    const val ALL_CATEGORY_ID = "all"
    const val COLLECTION_SIZE = 25

    val BALANCE = CollectiblesBalance(fiat = BigDecimal("2700.52"), crypto = "2,500.52 USDC")

    val CATEGORIES: List<CollectiblesCategory> = listOf(
        CollectiblesCategory(id = ALL_CATEGORY_ID, label = "All cards", count = 32),
        CollectiblesCategory(id = "sports", label = "Sports", count = 8),
        CollectiblesCategory(id = "pokemon", label = "Pokemon", count = 12),
        CollectiblesCategory(id = "something_else", label = "Something else", count = 6),
        CollectiblesCategory(id = "anime", label = "Anime", count = 6),
    )

    val PACKS: List<CollectiblesPack> = listOf(
        pack(id = "legendary_1", title = "Legendary 1", categoryId = "pokemon"),
        pack(id = "water_1", title = "Water 1", categoryId = "pokemon"),
        pack(id = "elite_1", title = "Elite 1", categoryId = "sports"),
        pack(id = "legendary_2", title = "Legendary 2", categoryId = "sports"),
        pack(id = "water_2", title = "Water 2", categoryId = "anime"),
        pack(id = "elite_2", title = "Elite 2", categoryId = "something_else"),
    )

    private fun pack(id: String, title: String, categoryId: String) = CollectiblesPack(
        id = id,
        imageUrl = "",
        title = title,
        price = "$50",
        buyback = "Buyback 85%",
        categoryId = categoryId,
    )
}