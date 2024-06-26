package com.tangem.data.staking.converters

import com.tangem.datasource.api.stakekit.models.response.model.BalanceDTO
import com.tangem.domain.staking.model.BalanceItem
import com.tangem.domain.staking.model.BalanceType
import com.tangem.domain.staking.model.YieldBalance
import com.tangem.domain.staking.model.YieldBalanceItem
import com.tangem.utils.converter.Converter

internal class YieldBalanceConverter : Converter<YieldBalanceConverter.Data, YieldBalance> {

    override fun convert(value: Data): YieldBalance {
        return if (value.balance.isEmpty()) {
            YieldBalance.Empty
        } else {
            YieldBalance.Data(
                balance = YieldBalanceItem(
                    items = value.balance.map { item ->
                        BalanceItem(
                            type = BalanceType.valueOf(item.type.name),
                            amount = item.amount,
                            pricePerShare = item.pricePerShare,
                            rawCurrencyId = item.tokenDTO.coinGeckoId,
                        )
                    },
                    integrationId = value.integrationId,
                ),
            )
        }
    }

    data class Data(
        val balance: List<BalanceDTO>,
        val integrationId: String?,
    )
}