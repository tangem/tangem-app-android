package com.tangem.datasource.local.token.converter

import com.tangem.datasource.api.stakekit.models.response.model.action.StakingActionTypeDTO
import com.tangem.domain.staking.model.stakekit.action.StakingActionType
import com.tangem.utils.converter.Converter

@Suppress("CyclomaticComplexMethod")
object StakingActionTypeConverter : Converter<StakingActionTypeDTO, StakingActionType> {

    override fun convert(value: StakingActionTypeDTO): StakingActionType {
        return when (value) {
            StakingActionTypeDTO.STAKE -> StakingActionType.STAKE
            StakingActionTypeDTO.UNSTAKE -> StakingActionType.UNSTAKE
            StakingActionTypeDTO.CLAIM_REWARDS -> StakingActionType.CLAIM_REWARDS
            StakingActionTypeDTO.RESTAKE_REWARDS -> StakingActionType.RESTAKE_REWARDS
            StakingActionTypeDTO.WITHDRAW -> StakingActionType.WITHDRAW
            StakingActionTypeDTO.RESTAKE -> StakingActionType.RESTAKE
            StakingActionTypeDTO.CLAIM_UNSTAKED -> StakingActionType.CLAIM_UNSTAKED
            StakingActionTypeDTO.UNLOCK_LOCKED -> StakingActionType.UNLOCK_LOCKED
            StakingActionTypeDTO.STAKE_LOCKED -> StakingActionType.STAKE_LOCKED
            StakingActionTypeDTO.VOTE -> StakingActionType.VOTE
            StakingActionTypeDTO.REVOKE -> StakingActionType.REVOKE
            StakingActionTypeDTO.VOTE_LOCKED -> StakingActionType.VOTE_LOCKED
            StakingActionTypeDTO.REVOTE -> StakingActionType.REVOTE
            StakingActionTypeDTO.REBOND -> StakingActionType.REBOND
            StakingActionTypeDTO.MIGRATE -> StakingActionType.MIGRATE
            StakingActionTypeDTO.UNKNOWN -> StakingActionType.UNKNOWN
        }
    }
}