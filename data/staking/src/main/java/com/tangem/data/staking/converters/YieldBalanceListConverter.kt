package com.tangem.data.staking.converters

import com.tangem.datasource.api.stakekit.models.response.model.YieldBalanceWrapperDTO
import com.tangem.domain.staking.model.YieldBalanceList
import com.tangem.utils.converter.Converter

internal class YieldBalanceListConverter : Converter<YieldBalanceWrapperDTO, YieldBalanceList> {

    internal val converter by lazy(LazyThreadSafetyMode.NONE) {
        YieldBalanceConverter()
    }

    override fun convert(value: YieldBalanceWrapperDTO): YieldBalanceList {
        return YieldBalanceList(
            balances = converter.convertList(value.balances),
        )
    }
}