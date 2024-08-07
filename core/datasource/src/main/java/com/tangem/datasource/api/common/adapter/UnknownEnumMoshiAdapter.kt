package com.tangem.datasource.api.common.adapter

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.EnumJsonAdapter
import com.tangem.datasource.api.stakekit.models.response.model.BalanceDTO
import com.tangem.datasource.api.stakekit.models.response.model.NetworkTypeDTO
import com.tangem.datasource.api.stakekit.models.response.model.YieldDTO
import com.tangem.datasource.api.stakekit.models.response.model.action.StakingActionStatusDTO
import com.tangem.datasource.api.stakekit.models.response.model.action.StakingActionTypeDTO
import com.tangem.datasource.api.stakekit.models.response.model.transaction.StakingTransactionStatusDTO
import com.tangem.datasource.api.stakekit.models.response.model.transaction.StakingTransactionTypeDTO

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
        NetworkTypeDTO::class.java to NetworkTypeDTO.UNKNOWN,
        StakingActionTypeDTO::class.java to StakingActionTypeDTO.UNKNOWN,
        YieldDTO.RewardTypeDTO::class.java to YieldDTO.RewardTypeDTO.UNKNOWN,
        BalanceDTO.BalanceType::class.java to BalanceDTO.BalanceType.UNKNOWN,
        StakingTransactionTypeDTO::class.java to StakingTransactionTypeDTO.UNKNOWN,
        StakingTransactionStatusDTO::class.java to StakingTransactionStatusDTO.UNKNOWN,
        StakingActionStatusDTO::class.java to StakingActionStatusDTO.UNKNOWN,
    )

    return apply {
        map.forEach { entry ->
            val enumClass = entry.key
            val unknownValue = entry.value
            add(enumClass, UnknownEnumMoshiAdapter.create(enumClass, unknownValue))
        }
    }
}
