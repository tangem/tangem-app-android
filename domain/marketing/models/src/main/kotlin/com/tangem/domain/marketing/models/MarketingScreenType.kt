package com.tangem.domain.marketing.models

enum class MarketingScreenType(val value: String) {
    SWAP("swap"),
    ONRAMP("onramp"),
    TOKEN_DETAILS("token_details"),
    TOKEN_MARKETS("token_markets"),
    STAKING("staking"),
    YIELD("yield"),
    ;

    /** Background types are ETag-cached; swap/onramp are always re-requested per pair selection. */
    val isCacheable: Boolean
        get() = this != SWAP && this != ONRAMP

    companion object {
        fun fromValue(value: String): MarketingScreenType? = entries.firstOrNull { it.value == value }
    }
}