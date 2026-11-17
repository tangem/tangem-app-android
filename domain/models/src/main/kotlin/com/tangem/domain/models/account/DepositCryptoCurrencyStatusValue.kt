package com.tangem.domain.models.account

import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.currency.balance.BalanceContribution
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.serialization.SerializedBigDecimal

internal fun depositCryptoCurrencyStatusValue(
    amount: SerializedBigDecimal,
    fiatRate: SerializedBigDecimal?,
    depositAddress: String,
    source: StatusSource,
    contributions: List<BalanceContribution> = emptyList(),
): CryptoCurrencyStatus.Value {
    val networkAddress = NetworkAddress.Single(
        defaultAddress = NetworkAddress.Address(
            type = NetworkAddress.Address.Type.Primary,
            value = depositAddress,
        ),
    )
    val sources = CryptoCurrencyStatus.Sources(
        networkSource = source,
        quoteSource = source,
        stakingBalanceSource = source,
        contributionSources = contributions.map(BalanceContribution::source),
    )

    return if (fiatRate != null) {
        CryptoCurrencyStatus.Loaded(
            amount = amount,
            fiatAmount = amount.multiply(fiatRate),
            fiatRate = fiatRate,
            priceChange = SerializedBigDecimal.ZERO,
            networkAddress = networkAddress,
            sources = sources,
            pendingTransactions = emptySet(),
            yieldSupplyStatus = null,
            hasCurrentNetworkTransactions = false,
            contributions = contributions,
        )
    } else {
        CryptoCurrencyStatus.NoQuote(
            amount = amount,
            networkAddress = networkAddress,
            yieldSupplyStatus = null,
            sources = sources,
            pendingTransactions = emptySet(),
            hasCurrentNetworkTransactions = false,
            contributions = contributions,
        )
    }
}