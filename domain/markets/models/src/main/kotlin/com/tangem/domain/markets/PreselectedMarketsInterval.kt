package com.tangem.domain.markets

import kotlinx.serialization.Serializable

@Serializable
enum class PreselectedMarketsInterval(val value: String) {
    H24("24h"),
    W1("1w"),
    D30("30d"),
    ;

    companion object {
        fun parse(value: String?): PreselectedMarketsInterval? = entries.firstOrNull { it.value == value }
    }
}