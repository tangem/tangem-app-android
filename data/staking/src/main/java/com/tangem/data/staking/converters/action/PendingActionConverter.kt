package com.tangem.data.staking.converters.action

import com.tangem.datasource.api.stakekit.models.response.model.BalanceDTO
import com.tangem.domain.staking.model.stakekit.PendingAction
import com.tangem.utils.converter.Converter

internal class PendingActionConverter : Converter<BalanceDTO.PendingAction, PendingAction> {

    private val stakingActionTypeConverter by lazy(LazyThreadSafetyMode.NONE) { StakingActionTypeConverter() }

    override fun convert(value: BalanceDTO.PendingAction): PendingAction {
        return PendingAction(
            type = stakingActionTypeConverter.convert(value.type),
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
