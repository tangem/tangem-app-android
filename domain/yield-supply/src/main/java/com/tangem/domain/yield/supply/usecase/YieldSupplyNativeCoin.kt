package com.tangem.domain.yield.supply.usecase

import com.tangem.domain.account.status.provider.NonPortfolioCurrenciesProvider
import com.tangem.domain.account.status.utils.CryptoCurrencyOperations.getCoin
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId

/**
 * The network coin of [currency], taken from the account that holds [currency] only: the Money account when
 * [isHeldOutsidePortfolio], the portfolio otherwise.
 */
internal suspend fun findNativeCoin(
    userWalletId: UserWalletId,
    currency: CryptoCurrency,
    isHeldOutsidePortfolio: Boolean,
    singleAccountListSupplier: SingleAccountListSupplier,
    nonPortfolioCurrenciesProvider: NonPortfolioCurrenciesProvider,
): CryptoCurrency.Coin {
    val coin = if (isHeldOutsidePortfolio) {
        nonPortfolioCurrenciesProvider.getStatuses(userWalletId = userWalletId, network = currency.network)
            .firstNotNullOfOrNull { status -> status.currency as? CryptoCurrency.Coin }
    } else {
        singleAccountListSupplier.getSyncOrNull(userWalletId = userWalletId)?.getCoin(currency)?.getOrNull()
    }

    val holder = if (isHeldOutsidePortfolio) "outside the portfolio" else "in the portfolio"
    return coin ?: error("Unable to find coin for network ID: ${currency.network.id} $holder")
}