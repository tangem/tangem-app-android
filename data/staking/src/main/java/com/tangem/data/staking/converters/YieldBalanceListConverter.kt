package com.tangem.data.staking.converters

import com.tangem.datasource.api.stakekit.models.response.model.YieldBalanceWrapperDTO
import com.tangem.domain.staking.model.stakekit.YieldBalanceList
import com.tangem.utils.converter.Converter

internal class YieldBalanceListConverter(
    private val yieldBalanceConverter: YieldBalanceConverter,
) : Converter<Set<YieldBalanceWrapperDTO>, YieldBalanceList> {

    override fun convert(value: Set<YieldBalanceWrapperDTO>): YieldBalanceList {
        return if (value.isEmpty()) {
            YieldBalanceList.Empty
        } else {
            YieldBalanceList.Data(
                balances = value.map(yieldBalanceConverter::convert),
            )
        }
    }
}