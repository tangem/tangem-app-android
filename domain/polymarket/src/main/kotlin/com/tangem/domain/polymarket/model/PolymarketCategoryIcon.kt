package com.tangem.domain.polymarket.model

/**
 * Category icon the BFF names by key in `Category.icon`; the client draws it from the design system.
 *
 * @property key value of `Category.icon`, matched case-sensitively
 */
enum class PolymarketCategoryIcon(val key: String) {
    WORLD("world"),
    GEOPOLITICS("geopolitics"),
    ELECTIONS("elections"),
    SOCCER("soccer"),
    NBA("nba"),
    NFL("nfl"),
    HOCKEY("hockey"),
    TENNIS("tennis"),
    FORMULA_1("formula1"),
    TECH("tech"),
    ECONOMY("economy"),
    MENTIONS("mentions"),
    TRENDING("trending"),
    NEW("new"),
    POLITICS("politics"),
    SPORTS("sports"),
    ESPORTS("esports"),
    CULTURE("culture"),
    WEATHER("weather"),
    FINANCE("finance"),
    CRYPTO("crypto"),
    ;

    companion object {

        /** @return `null` for a missing key or one this build does not know */
        fun fromKey(key: String?): PolymarketCategoryIcon? = entries.firstOrNull { it.key == key }
    }
}