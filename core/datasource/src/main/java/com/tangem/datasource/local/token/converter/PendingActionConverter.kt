package com.tangem.datasource.local.token.converter

import com.tangem.datasource.api.stakekit.models.response.model.BalanceDTO
import com.tangem.domain.models.staking.PendingAction
import com.tangem.utils.converter.Converter

internal object PendingActionConverter : Converter<BalanceDTO.PendingAction, PendingAction> {

    override fun convert(value: BalanceDTO.PendingAction): PendingAction {
        return PendingAction(
            type = StakingActionTypeConverter.convert(value.type),
            passthrough = value.passthrough,
            args = with(value.args) {
                PendingAction.PendingActionArgs(
                    amount = this?.amount?.let { amount ->
                        PendingAction.PendingActionArgs.Amount(
                            required = amount.required,
                            minimum = amount.minimum,
                            maximum = amount.maximum,
                        )
                    },
                    duration = this?.duration?.let { duration ->
                        PendingAction.PendingActionArgs.Duration(
                            required = duration.required,
                            minimum = duration.minimum,
                            maximum = duration.maximum,
                        )
                    },
                    validatorAddress = this?.validatorAddress?.required,
                    validatorAddresses = this?.validatorAddresses?.required,
                    tronResource = this?.tronResource?.let { tronResource ->
                        PendingAction.PendingActionArgs.TronResource(
                            required = tronResource.required,
                            options = tronResource.options,
                        )
                    },
                    signatureVerification = this?.signatureVerification?.required,
                )
            },
        )
    }
}