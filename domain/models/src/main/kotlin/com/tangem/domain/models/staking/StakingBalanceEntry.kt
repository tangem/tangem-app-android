package com.tangem.domain.models.staking

import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class StakingBalanceEntry(
    val id: String,
    val type: StakingEntryType,
    val amount: SerializedBigDecimal,
    val validator: ValidatorInfo?,
    val date: Instant?,
    val actions: StakingEntryActions,
    val isPending: Boolean,
    val rawCurrencyId: String?,
)

@Serializable
data class ValidatorInfo(
    val address: String,
    val name: String?,
)