package com.tangem.data.staking.converters.action

import com.tangem.datasource.api.stakekit.models.response.EnterActionResponse
import com.tangem.domain.staking.model.action.ActionStatus
import com.tangem.utils.converter.Converter

class ActionStatusConverter : Converter<EnterActionResponse.ActionStatusDTO, ActionStatus> {
    override fun convert(value: EnterActionResponse.ActionStatusDTO): ActionStatus {
        return when (value) {
            EnterActionResponse.ActionStatusDTO.CANCELED -> ActionStatus.CANCELED
            EnterActionResponse.ActionStatusDTO.CREATED -> ActionStatus.CREATED
            EnterActionResponse.ActionStatusDTO.WAITING_FOR_NEXT -> ActionStatus.WAITING_FOR_NEXT
            EnterActionResponse.ActionStatusDTO.PROCESSING -> ActionStatus.PROCESSING
            EnterActionResponse.ActionStatusDTO.FAILED -> ActionStatus.FAILED
            EnterActionResponse.ActionStatusDTO.SUCCESS -> ActionStatus.SUCCESS
            else -> ActionStatus.UNKNOWN
        }
    }
}
