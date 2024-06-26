package com.tangem.data.staking.converters

import com.tangem.datasource.api.stakekit.models.response.model.YieldBalanceWrapperDTO
import com.tangem.domain.staking.model.YieldBalanceList
import com.tangem.utils.converter.Converter

internal class YieldBalanceListConverter : Converter<List<YieldBalanceWrapperDTO>, YieldBalanceList> {

    internal val converter by lazy(LazyThreadSafetyMode.NONE) {
        YieldBalanceConverter()
    }

    override fun convert(value: List<YieldBalanceWrapperDTO>): YieldBalanceList {
        return if (value.isEmpty()) {
            YieldBalanceList.Empty
        } else {
            YieldBalanceList.Data(
                balances = value.map {
                    converter.convert(
                        YieldBalanceConverter.Data(
                            balance = it.balances,
                            integrationId = it.integrationId,
                        ),
                    )
                },
            )
        }
    }
}
