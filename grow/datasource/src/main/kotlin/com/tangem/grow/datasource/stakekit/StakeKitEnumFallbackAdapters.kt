package com.tangem.grow.datasource.stakekit

import com.squareup.moshi.Moshi
import com.tangem.core.remote.moshi.UnknownEnumMoshiAdapter
import com.tangem.grow.datasource.stakekit.models.response.model.BalanceDTO.BalanceTypeDTO
import com.tangem.grow.datasource.stakekit.models.response.model.NetworkTypeDTO
import com.tangem.grow.datasource.stakekit.models.response.model.YieldDTO.MetadataDTO.RewardClaimingDTO
import com.tangem.grow.datasource.stakekit.models.response.model.YieldDTO.MetadataDTO.RewardScheduleDTO
import com.tangem.grow.datasource.stakekit.models.response.model.YieldDTO.RewardTypeDTO
import com.tangem.grow.datasource.stakekit.models.response.model.YieldDTO.ValidatorDTO.ValidatorStatusDTO
import com.tangem.grow.datasource.stakekit.models.response.model.action.StakingActionStatusDTO
import com.tangem.grow.datasource.stakekit.models.response.model.action.StakingActionTypeDTO
import com.tangem.grow.datasource.stakekit.models.response.model.transaction.StakingTransactionStatusDTO
import com.tangem.grow.datasource.stakekit.models.response.model.transaction.StakingTransactionTypeDTO

fun Moshi.Builder.addStakeKitEnumFallbackAdapters(): Moshi.Builder {
    val map = mapOf(
        // valid response enums
        BalanceTypeDTO::class.java to BalanceTypeDTO.UNKNOWN,
        NetworkTypeDTO::class.java to NetworkTypeDTO.UNKNOWN,
        RewardClaimingDTO::class.java to RewardClaimingDTO.UNKNOWN,
        RewardScheduleDTO::class.java to RewardScheduleDTO.UNKNOWN,
        RewardTypeDTO::class.java to RewardTypeDTO.UNKNOWN,
        StakingActionStatusDTO::class.java to StakingActionStatusDTO.UNKNOWN,
        StakingActionTypeDTO::class.java to StakingActionTypeDTO.UNKNOWN,
        StakingTransactionStatusDTO::class.java to StakingTransactionStatusDTO.UNKNOWN,
        StakingTransactionTypeDTO::class.java to StakingTransactionTypeDTO.UNKNOWN,
        ValidatorStatusDTO::class.java to ValidatorStatusDTO.UNKNOWN,
    )

    return apply {
        map.forEach { entry ->
            val enumClass = entry.key
            val unknownValue = entry.value
            add(enumClass, UnknownEnumMoshiAdapter.create(enumClass, unknownValue))
        }
    }
}