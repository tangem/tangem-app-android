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
                    amount = this?.amount?.let {
                        PendingAction.PendingActionArgs.Amount(
                            required = it.required,
                            minimum = it.minimum,
                            maximum = it.maximum,
                        )
                    },
                    duration = this?.duration?.let {
                        PendingAction.PendingActionArgs.Duration(
                            required = it.required,
                            minimum = it.minimum,
                            maximum = it.maximum,
                        )
                    },
                    validatorAddress = this?.validatorAddress?.required,
                    validatorAddresses = this?.validatorAddresses?.required,
                    tronResource = this?.tronResource?.let {
                        PendingAction.PendingActionArgs.TronResource(
                            required = it.required,
                            options = it.options,
                        )
                    },
                    signatureVerification = this?.signatureVerification?.required,
                )
            },
        )
    }
}