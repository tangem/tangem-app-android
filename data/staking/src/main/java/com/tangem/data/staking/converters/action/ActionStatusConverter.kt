package com.tangem.data.staking.converters.action

import com.tangem.datasource.api.stakekit.models.response.model.action.StakingActionStatusDTO
import com.tangem.domain.staking.model.stakekit.action.StakingActionStatus
import com.tangem.utils.converter.TwoWayConverter

class ActionStatusConverter : TwoWayConverter<StakingActionStatusDTO, StakingActionStatus> {

    override fun convert(value: StakingActionStatusDTO): StakingActionStatus {
        return when (value) {
            StakingActionStatusDTO.CANCELED -> StakingActionStatus.CANCELED
            StakingActionStatusDTO.CREATED -> StakingActionStatus.CREATED
            StakingActionStatusDTO.WAITING_FOR_NEXT -> StakingActionStatus.WAITING_FOR_NEXT
            StakingActionStatusDTO.PROCESSING -> StakingActionStatus.PROCESSING
            StakingActionStatusDTO.FAILED -> StakingActionStatus.FAILED
            StakingActionStatusDTO.SUCCESS -> StakingActionStatus.SUCCESS
            else -> StakingActionStatus.UNKNOWN
        }
    }

    override fun convertBack(value: StakingActionStatus): StakingActionStatusDTO {
        return when (value) {
            StakingActionStatus.CANCELED -> StakingActionStatusDTO.CANCELED
            StakingActionStatus.CREATED -> StakingActionStatusDTO.CREATED
            StakingActionStatus.WAITING_FOR_NEXT -> StakingActionStatusDTO.WAITING_FOR_NEXT
            StakingActionStatus.PROCESSING -> StakingActionStatusDTO.PROCESSING
            StakingActionStatus.FAILED -> StakingActionStatusDTO.FAILED
            StakingActionStatus.SUCCESS -> StakingActionStatusDTO.SUCCESS
            else -> StakingActionStatusDTO.UNKNOWN
        }
    }
}