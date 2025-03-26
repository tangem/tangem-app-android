package com.tangem.datasource.local.token.converter

import com.tangem.datasource.api.stakekit.models.response.model.BalanceDTO
import com.tangem.domain.staking.model.stakekit.PendingAction
import com.tangem.domain.staking.model.stakekit.PendingActionConstraints
import com.tangem.utils.converter.Converter

internal object PendingActionConstraintsConverter :
    Converter<BalanceDTO.PendingActionConstraints, PendingActionConstraints> {
    override fun convert(value: BalanceDTO.PendingActionConstraints): PendingActionConstraints {
        return PendingActionConstraints(
            type = StakingActionTypeConverter.convert(value.type),
            amountArg = PendingAction.PendingActionArgs.Amount(
                required = value.amountArg.required,
                minimum = value.amountArg.minimum,
                maximum = value.amountArg.maximum,
            ),
        )
    }
}