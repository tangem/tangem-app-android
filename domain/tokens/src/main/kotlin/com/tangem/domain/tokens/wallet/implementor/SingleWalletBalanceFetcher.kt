package com.tangem.domain.tokens.wallet.implementor

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.wallet.BaseWalletBalanceFetcher
import com.tangem.domain.tokens.wallet.FetchingSource
import com.tangem.domain.wallets.models.UserWalletId

/**
 * Implementation of [BaseWalletBalanceFetcher] for SINGLE-CURRENCY wallet
 *
 * @property currenciesRepository currencies repository
 *
[REDACTED_AUTHOR]
 */
internal class SingleWalletBalanceFetcher(
    private val currenciesRepository: CurrenciesRepository,
) : BaseWalletBalanceFetcher {

    override val fetchingSources: Set<FetchingSource> = setOf(
        FetchingSource.NETWORK,
        FetchingSource.QUOTE,
    )

    override suspend fun getCryptoCurrencies(userWalletId: UserWalletId): Set<CryptoCurrency> {
        val primaryCurrency = currenciesRepository.getSingleCurrencyWalletPrimaryCurrency(
            userWalletId = userWalletId,
            refresh = true,
        )

        return setOf(primaryCurrency)
    }
}