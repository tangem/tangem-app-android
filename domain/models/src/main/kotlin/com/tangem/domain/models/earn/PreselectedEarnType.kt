package com.tangem.domain.models.earn

import kotlinx.serialization.Serializable

@Serializable
enum class PreselectedEarnType(val value: String) {
    Staking("staking"),
    Yield("yield"),
    ;

    companion object {
        fun parse(value: String?): PreselectedEarnType? = entries.firstOrNull { it.value == value }
    }
}