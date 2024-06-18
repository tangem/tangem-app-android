package com.tangem.datasource.api.stakekit.models.request

enum class YieldType(private val value: String) {
    STAKING("staking"),
    LIQUID_STAKING("liquid-staking"),
    LENDING("lending"),
    RESTAKING("restaking"),
    VAULT("vault"),
    ;

    override fun toString(): String {
        return value
    }
}