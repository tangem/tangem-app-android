package com.tangem.datasource.local.token.converter

import com.tangem.grow.datasource.stakekit.models.response.model.BalanceDTO
import com.tangem.domain.models.staking.PendingAction
import com.tangem.utils.converter.Converter

internal object PendingActionConverter : Converter<BalanceDTO.PendingAction, PendingAction> {

    override fun convert(value: BalanceDTO.PendingAction): PendingAction {
        val args = value.args
        return PendingAction(
            type = StakingActionTypeConverter.convert(value.type),
            passthrough = value.passthrough,
            args = PendingAction.PendingActionArgs(
                amount = args?.amount?.let { amount ->
                    PendingAction.PendingActionArgs.Amount(
                        isRequired = amount.required,
                        minimum = amount.minimum,
                        maximum = amount.maximum,
                    )
                },
                duration = args?.duration?.let { duration ->
                    PendingAction.PendingActionArgs.Duration(
                        isRequired = duration.required,
                        minimum = duration.minimum,
                        maximum = duration.maximum,
                    )
                },
                hasValidatorAddress = args?.validatorAddress?.required,
                hasValidatorAddresses = args?.validatorAddresses?.required,
                tronResource = args?.tronResource?.let { tronResource ->
                    PendingAction.PendingActionArgs.TronResource(
                        isRequired = tronResource.required,
                        options = tronResource.options,
                    )
                },
                hasSignatureVerification = args?.signatureVerification?.required,
            ),
        )
    }
}