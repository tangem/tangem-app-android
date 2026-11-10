package com.tangem.domain.polymarket.model

sealed class PolymarketCategoryFilter(val value: String) {

    data object Games : PolymarketCategoryFilter("games")

    data object Props : PolymarketCategoryFilter("props")

    data class Unknown(val rawValue: String) : PolymarketCategoryFilter(rawValue)

    companion object {
        fun fromValue(value: String): PolymarketCategoryFilter = when (value) {
            Games.value -> Games
            Props.value -> Props
            else -> Unknown(value)
        }
    }
}