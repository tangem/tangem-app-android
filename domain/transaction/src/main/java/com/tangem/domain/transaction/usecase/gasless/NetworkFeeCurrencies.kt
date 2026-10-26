package com.tangem.domain.transaction.usecase.gasless

import arrow.core.raise.Raise
import com.tangem.domain.account.status.provider.NonPortfolioCurrenciesProvider
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.raiseIllegalStateError

internal class NetworkFeeCurrencies(
    val nativeCurrencyStatus: CryptoCurrencyStatus,
    val nonPortfolioStatuses: List<CryptoCurrencyStatus>,
) {

    val nonPortfolioFeeToken: CryptoCurrencyStatus?
        get() = nonPortfolioStatuses.firstOrNull { it.currency is CryptoCurrency.Token }
}

internal suspend fun Raise<GetFeeError>.resolveNetworkFeeCurrencies(
    nonPortfolioCurrenciesProvider: NonPortfolioCurrenciesProvider,
    userWalletId: UserWalletId,
    network: Network,
    portfolioCoinStatus: CryptoCurrencyStatus?,
): NetworkFeeCurrencies {
    val isPortfolioMiss = portfolioCoinStatus == null && network.derivationPath !is Network.DerivationPath.Joint
    val nonPortfolioStatuses = if (isPortfolioMiss) {
        nonPortfolioCurrenciesProvider.getStatuses(userWalletId, network)
    } else {
        emptyList()
    }

    val nativeCurrencyStatus = portfolioCoinStatus
        ?: nonPortfolioStatuses.firstOrNull { it.currency is CryptoCurrency.Coin }
        ?: raiseIllegalStateError("No native currency found: ${network.id}")

    return NetworkFeeCurrencies(
        nativeCurrencyStatus = nativeCurrencyStatus,
        nonPortfolioStatuses = nonPortfolioStatuses,
    )
}