package com.tangem.data.staking.converters

import com.tangem.datasource.api.stakekit.models.response.model.BalanceDTO
import com.tangem.domain.staking.model.BalanceType
import com.tangem.domain.staking.model.YieldBalance
import com.tangem.utils.converter.Converter

internal class YieldBalanceConverter : Converter<BalanceDTO, YieldBalance> {
    override fun convert(value: BalanceDTO): YieldBalance {
        return YieldBalance(
            type = BalanceType.valueOf(value.type.name),
            amount = value.amount,
            pricePerShare = value.pricePerShare,
        )
    }
}