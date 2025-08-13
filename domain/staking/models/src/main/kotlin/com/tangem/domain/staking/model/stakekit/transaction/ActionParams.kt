package com.tangem.domain.staking.model.stakekit.transaction

import com.tangem.domain.models.staking.YieldToken
import com.tangem.domain.models.staking.action.StakingActionType
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import java.math.BigDecimal

data class ActionParams(
    val actionCommonType: StakingActionCommonType,
    val integrationId: String,
    val amount: BigDecimal,
    val address: String,
    val validatorAddress: String,
    val token: YieldToken,
    val publicKey: String? = null,
    val passthrough: String? = null,
    val type: StakingActionType? = null,
)