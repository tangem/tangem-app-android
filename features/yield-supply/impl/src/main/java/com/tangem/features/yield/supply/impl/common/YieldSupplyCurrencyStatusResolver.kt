package com.tangem.features.yield.supply.impl.common

import arrow.core.Option
import arrow.core.some
import arrow.core.toOption
import com.tangem.common.routing.AppRoute.YieldSupplyEntry.AccountKind
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
 * Status of a Yield Mode currency in the account the flow was opened from: a portfolio account, or an account outside
 * the portfolio (the Money account) through [NonPortfolioCurrenciesProvider].
 */
internal class YieldSupplyCurrencyStatusResolver @Inject constructor(
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
    private val nonPortfolioCurrenciesProvider: NonPortfolioCurrenciesProvider,
) {

    fun observe(
        userWalletId: UserWalletId,
        currency: CryptoCurrency,
        account: AccountKind,
    ): Flow<Option<CryptoCurrencyStatus>> = flow {
        if (account.isHeldOutsidePortfolio) {
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

    suspend fun getSync(
        userWalletId: UserWalletId,
        currency: CryptoCurrency,
        account: AccountKind,
    ): Option<CryptoCurrencyStatus> {
        if (account.isHeldOutsidePortfolio) {
            return findNonPortfolioStatus(
                userWalletId = userWalletId,
                currencyId = currency.id,
                network = currency.network,
            ).toOption()
        }

        return singleAccountStatusListSupplier.getSyncOrNull(userWalletId)
            .getCryptoCurrencyStatus(currency = currency)
    }

    suspend fun getSync(
        userWalletId: UserWalletId,
        currencyId: CryptoCurrency.ID,
        network: Network,
        account: AccountKind,
    ): CryptoCurrencyStatus? {
        if (account.isHeldOutsidePortfolio) {
            return findNonPortfolioStatus(userWalletId = userWalletId, currencyId = currencyId, network = network)
        }

        return singleAccountStatusListSupplier.getSyncOrNull(userWalletId)
            .getCryptoCurrencyStatus(currencyId = currencyId, network = network)
            .getOrNull()
    }

    private suspend fun findNonPortfolioStatus(
        userWalletId: UserWalletId,
        currencyId: CryptoCurrency.ID,
        network: Network,
    ): CryptoCurrencyStatus? {
        return nonPortfolioCurrenciesProvider.getStatuses(userWalletId = userWalletId, network = network)
            .firstOrNull { it.currency.id == currencyId }
    }
}