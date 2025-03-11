package com.tangem.data.staking.converters.action

import com.tangem.data.staking.converters.transaction.StakingTransactionConverter
import com.tangem.datasource.api.stakekit.models.response.ActionDTO
import com.tangem.datasource.local.token.converter.StakingActionTypeConverter
import com.tangem.domain.staking.model.stakekit.action.StakingAction
import com.tangem.utils.converter.Converter

class EnterActionResponseConverter(
    private val actionStatusConverter: ActionStatusConverter,
    private val transactionConverter: StakingTransactionConverter,
) : Converter<ActionDTO, StakingAction> {

    override fun convert(value: ActionDTO): StakingAction {
        return StakingAction(
            id = value.id,
            integrationId = value.integrationId,
            status = actionStatusConverter.convert(value.status),
            type = StakingActionTypeConverter.convert(value.type),
            currentStepIndex = value.currentStepIndex,
            amount = value.amount,
            validatorAddress = value.validatorAddress,
            validatorAddresses = value.validatorAddresses,
            transactions = value.transactions?.map(transactionConverter::convert),
            createdAt = value.createdAt,
        )
    }
}