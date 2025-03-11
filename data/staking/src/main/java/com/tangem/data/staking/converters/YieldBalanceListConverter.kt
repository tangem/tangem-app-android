package com.tangem.data.staking.converters

import com.tangem.domain.staking.model.stakekit.YieldBalance
import com.tangem.domain.staking.model.stakekit.YieldBalanceList
import com.tangem.utils.converter.Converter

internal object YieldBalanceListConverter : Converter<Set<YieldBalance>, YieldBalanceList> {

    override fun convert(value: Set<YieldBalance>): YieldBalanceList {
        return if (value.isEmpty()) {
            YieldBalanceList.Empty
        } else {
            YieldBalanceList.Data(balances = value.toList())
        }
    }
}