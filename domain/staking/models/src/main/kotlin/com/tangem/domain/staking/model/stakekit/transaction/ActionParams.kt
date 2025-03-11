package com.tangem.domain.staking.model.stakekit.transaction

import com.tangem.domain.staking.model.stakekit.Token
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.domain.staking.model.stakekit.action.StakingActionType
import java.math.BigDecimal

data class ActionParams(
    val actionCommonType: StakingActionCommonType,
    val integrationId: String,
    val amount: BigDecimal,
    val address: String,
    val validatorAddress: String,
    val token: Token,
    val publicKey: String? = null,
    val passthrough: String? = null,
    val type: StakingActionType? = null,
)