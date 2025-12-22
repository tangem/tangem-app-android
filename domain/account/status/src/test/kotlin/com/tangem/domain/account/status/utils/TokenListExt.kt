package com.tangem.domain.account.status.utils

import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.tokenlist.TokenList
import java.math.BigDecimal

internal fun createGroupedByNetwork(
    statuses: List<CryptoCurrencyStatus>,
    sortedBy: TokensSortType = TokensSortType.NONE,
): TokenList {
    return TokenList.GroupedByNetwork(
        totalFiatBalance = TotalFiatBalance.Loaded(amount = BigDecimal("11"), source = StatusSource.ACTUAL),
        sortedBy = sortedBy,
        groups = statuses.map {
            TokenList.GroupedByNetwork.NetworkGroup(
                network = it.currency.network,
                currencies = listOf(it),
            )
        },
    )
}

internal fun createUngrouped(
    statuses: List<CryptoCurrencyStatus>,
    sortedBy: TokensSortType = TokensSortType.NONE,
): TokenList {
    return TokenList.Ungrouped(
        totalFiatBalance = TotalFiatBalance.Loaded(amount = BigDecimal("11"), source = StatusSource.ACTUAL),
        sortedBy = sortedBy,
        currencies = statuses,
    )
}

internal fun createStatus(currency: CryptoCurrency, fiatAmount: BigDecimal): CryptoCurrencyStatus {
    return CryptoCurrencyStatus(
        currency = currency,
        value = CryptoCurrencyStatus.Loaded(
            amount = fiatAmount,
            fiatRate = BigDecimal.ONE,
            fiatAmount = fiatAmount,
            priceChange = BigDecimal.ZERO,
            stakingBalance = null,
            hasCurrentNetworkTransactions = false,
            yieldSupplyStatus = null,
            pendingTransactions = emptySet(),
            networkAddress = NetworkAddress.Single(
                defaultAddress = NetworkAddress.Address(
                    value = "addr1q9",
                    type = NetworkAddress.Address.Type.Primary,
                ),
            ),
            sources = CryptoCurrencyStatus.Sources(),
        ),
    )
}