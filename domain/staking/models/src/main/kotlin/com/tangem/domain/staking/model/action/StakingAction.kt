package com.tangem.domain.staking.model.action

import com.tangem.domain.staking.model.transaction.StakingTransaction
import org.joda.time.DateTime
import java.math.BigDecimal

data class StakingAction(
    val id: String,
    val integrationId: String,
    val status: StakingActionStatus,
    val type: StakingActionType,
    val currentStepIndex: Int,
    val amount: BigDecimal,
    val validatorAddress: String?,
    val validatorAddresses: List<String>?,
    val transactions: List<StakingTransaction>?,
    val createdAt: DateTime,
)