package com.tangem.datasource.api.common.adapter

import com.squareup.moshi.*
import com.squareup.moshi.adapters.EnumJsonAdapter
import com.tangem.datasource.api.stakekit.models.response.model.StakingActionType
import com.tangem.datasource.api.stakekit.models.response.model.Token
import com.tangem.datasource.api.stakekit.models.response.model.Yield
import com.tangem.datasource.api.stakekit.models.response.model.YieldBalanceWrapper

/**
 * Object to create a adapter for enum types with support for unknown enum values.
 */
object UnknownEnumMoshiAdapter {

    @Suppress("UNCHECKED_CAST")
    fun <T : Enum<T>> create(enumType: Class<out Enum<*>>, defaultValue: Enum<*>): JsonAdapter<out Enum<*>> {
        return EnumJsonAdapter.create(enumType as Class<T>).withUnknownFallback(defaultValue as T)
    }
}

fun Moshi.Builder.addStakeKitEnumFallbackAdapters(): Moshi.Builder {
    val map = mapOf(
        Token.NetworkType::class.java to Token.NetworkType.UNKNOWN,
        StakingActionType::class.java to StakingActionType.UNKNOWN,
        Yield.RewardType::class.java to Yield.RewardType.UNKNOWN,
        YieldBalanceWrapper.Balance.BalanceType::class.java to YieldBalanceWrapper.Balance.BalanceType.UNKNOWN,
    )

    return apply {
        map.forEach { entry ->
            val enumClass = entry.key
            val unknownValue = entry.value
            add(enumClass, UnknownEnumMoshiAdapter.create(enumClass, unknownValue))
        }
    }
}
