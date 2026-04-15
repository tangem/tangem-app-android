package com.tangem.domain.markets

import kotlinx.serialization.Serializable

@Serializable
enum class PreselectedTokenDetailsSection(val value: String) {
    News("news"),
    ;

    companion object {
        fun parse(value: String?): PreselectedTokenDetailsSection? = entries.firstOrNull { it.value == value }
    }
}