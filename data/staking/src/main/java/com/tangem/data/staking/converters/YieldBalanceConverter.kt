package com.tangem.data.staking.converters

import com.tangem.data.staking.converters.action.PendingActionConverter
import com.tangem.datasource.api.stakekit.models.response.model.YieldBalanceWrapperDTO
import com.tangem.domain.staking.model.stakekit.*
import com.tangem.utils.converter.Converter

internal class YieldBalanceConverter : Converter<YieldBalanceWrapperDTO, YieldBalance> {

    private val pendingActionConverter by lazy(LazyThreadSafetyMode.NONE) { PendingActionConverter() }

    override fun convert(value: YieldBalanceWrapperDTO): YieldBalance {
        return if (value.balances.isEmpty()) {
            YieldBalance.Empty
        } else {
            YieldBalance.Data(
                address = value.addresses.address,
                balance = YieldBalanceItem(
                    items = value.balances.map { item ->
                        BalanceItem(
                            groupId = item.groupId,
                            type = BalanceType.valueOf(item.type.name),
                            amount = item.amount,
                            rawCurrencyId = item.tokenDTO.coinGeckoId,
                            // tron-specific. operates validatorAddresses instead of validatorAddress
                            validatorAddress = item.validatorAddress ?: item.validatorAddresses?.get(0),
                            date = item.date?.toDateTime(),
                            pendingActions = pendingActionConverter
                                .convertList(item.pendingActions),
                            isPending = false,
                        )
                    },
                    integrationId = value.integrationId,
                ),
            )
        }
    }
}