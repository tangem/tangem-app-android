package com.tangem.features.yield.supply.impl.common

import arrow.core.Option
import arrow.core.some
import arrow.core.toOption
import com.tangem.domain.account.status.provider.NonPortfolioCurrenciesProvider
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.utils.CryptoCurrencyStatusOperations.getCryptoCurrencyStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Status of a Yield Mode currency wherever it is held: a portfolio account, or an account outside the portfolio
 * (the Money account) through [NonPortfolioCurrenciesProvider].
 */
internal class YieldSupplyCurrencyStatusResolver @Inject constructor(
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
    private val nonPortfolioCurrenciesProvider: NonPortfolioCurrenciesProvider,
) {

    fun observe(userWalletId: UserWalletId, currency: CryptoCurrency): Flow<Option<CryptoCurrencyStatus>> = flow {
        if (isHeldOutsidePortfolio(userWalletId = userWalletId, currency = currency)) {
            emitAll(
                nonPortfolioCurrenciesProvider.observe(userWalletId = userWalletId, currency = currency)
                    .map { (_, status) -> status.some() },
            )
        } else {
            emitAll(
                singleAccountStatusListSupplier(userWalletId).map { it.getCryptoCurrencyStatus(currency = currency) },
            )
        }
    }

    suspend fun getSync(userWalletId: UserWalletId, currency: CryptoCurrency): Option<CryptoCurrencyStatus> {
        val portfolioStatus = singleAccountStatusListSupplier.getSyncOrNull(userWalletId)
            .getCryptoCurrencyStatus(currency = currency)
        if (portfolioStatus.isSome()) return portfolioStatus

        return findNonPortfolioStatus(userWalletId = userWalletId, currencyId = currency.id, network = currency.network)
            .toOption()
    }

    suspend fun getSync(
        userWalletId: UserWalletId,
        currencyId: CryptoCurrency.ID,
        network: Network,
    ): CryptoCurrencyStatus? {
        return singleAccountStatusListSupplier.getSyncOrNull(userWalletId)
            .getCryptoCurrencyStatus(currencyId = currencyId, network = network)
            .getOrNull()
            ?: findNonPortfolioStatus(userWalletId = userWalletId, currencyId = currencyId, network = network)
    }

    private suspend fun findNonPortfolioStatus(
        userWalletId: UserWalletId,
        currencyId: CryptoCurrency.ID,
        network: Network,
    ): CryptoCurrencyStatus? {
        return nonPortfolioCurrenciesProvider.getStatuses(userWalletId = userWalletId, network = network)
            .firstOrNull { it.currency.id == currencyId }
    }

    /**
     * Whether [currency] is held by an account outside the portfolio (the Money account). Decided by the holder
     * rather than by a portfolio miss: the portfolio list is empty until it first loads.
     */
    suspend fun isHeldOutsidePortfolio(userWalletId: UserWalletId, currency: CryptoCurrency): Boolean {
        return nonPortfolioCurrenciesProvider.findCurrency(userWalletId, currency.id) != null
    }

    /** Whether [currency] is in a portfolio account, the only place with a token details screen to open. */
    suspend fun isInPortfolio(userWalletId: UserWalletId, currency: CryptoCurrency): Boolean {
        return singleAccountStatusListSupplier.getSyncOrNull(userWalletId)
            .getCryptoCurrencyStatus(currency = currency)
            .isSome()
    }
}