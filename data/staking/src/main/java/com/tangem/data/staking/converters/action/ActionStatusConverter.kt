package com.tangem.data.staking.converters.action

import com.tangem.datasource.api.stakekit.models.response.model.action.ActionStatusDTO
import com.tangem.domain.staking.model.action.ActionStatus
import com.tangem.utils.converter.Converter

class ActionStatusConverter : Converter<ActionStatusDTO, ActionStatus> {
    override fun convert(value: ActionStatusDTO): ActionStatus {
        return when (value) {
            ActionStatusDTO.CANCELED -> ActionStatus.CANCELED
            ActionStatusDTO.CREATED -> ActionStatus.CREATED
            ActionStatusDTO.WAITING_FOR_NEXT -> ActionStatus.WAITING_FOR_NEXT
            ActionStatusDTO.PROCESSING -> ActionStatus.PROCESSING
            ActionStatusDTO.FAILED -> ActionStatus.FAILED
            ActionStatusDTO.SUCCESS -> ActionStatus.SUCCESS
            else -> ActionStatus.UNKNOWN
        }
    }
}
