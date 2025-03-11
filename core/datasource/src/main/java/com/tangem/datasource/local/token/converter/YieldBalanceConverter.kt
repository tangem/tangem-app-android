package com.tangem.datasource.local.token.converter

import com.tangem.datasource.api.stakekit.models.response.model.YieldBalanceWrapperDTO
import com.tangem.domain.models.StatusSource
import com.tangem.domain.staking.model.stakekit.BalanceItem
import com.tangem.domain.staking.model.stakekit.YieldBalance
import com.tangem.domain.staking.model.stakekit.YieldBalanceItem
import com.tangem.utils.converter.Converter

internal class YieldBalanceConverter(private val isCached: Boolean) : Converter<YieldBalanceWrapperDTO, YieldBalance> {

    override fun convert(value: YieldBalanceWrapperDTO): YieldBalance {
        return if (value.balances.isEmpty()) {
            YieldBalance.Empty(
                integrationId = value.integrationId,
                address = value.addresses.address,
                source = if (isCached) StatusSource.CACHE else StatusSource.ACTUAL,
            )
        } else {
            YieldBalance.Data(
                integrationId = value.integrationId,
                address = value.addresses.address,
                balance = YieldBalanceItem(
                    items = value.balances.map { item ->
                        BalanceItem(
                            groupId = item.groupId,
                            token = TokenConverter.convert(item.tokenDTO),
                            type = BalanceTypeConverter.convert(item.type),
                            amount = item.amount,
                            rawCurrencyId = item.tokenDTO.coinGeckoId,
                            // tron-specific. operates validatorAddresses instead of validatorAddress
                            validatorAddress = item.validatorAddress ?: item.validatorAddresses?.get(0),
                            date = item.date?.toDateTime(),
                            pendingActions = PendingActionConverter
                                .convertList(item.pendingActions)
                                .sortedBy { it.passthrough },
                            isPending = false,
                        )
                    }
                        .sortedWith(compareBy({ it.type }, { it.amount })),
                    integrationId = value.integrationId,
                ),
                source = if (isCached) StatusSource.CACHE else StatusSource.ACTUAL,
            )
        }
    }
}