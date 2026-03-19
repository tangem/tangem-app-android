package com.tangem.domain.tokens.wallet.implementor

import com.tangem.domain.common.tokens.CardCryptoCurrencyFactory
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.requireColdWallet
import com.tangem.domain.tokens.FetchingSource
import com.tangem.domain.tokens.wallet.BaseWalletBalanceFetcher
import com.tangem.domain.tokens.wallet.WalletFetchingSource

/**
 * Implementation of [BaseWalletBalanceFetcher] for SINGLE-CURRENCY wallet WITH TOKEN (like, NODL)
 *
 * @property cardCryptoCurrencyFactory card crypto currency factory
 *
[REDACTED_AUTHOR]
 */
internal class SingleWalletWithTokenBalanceFetcher(
    private val cardCryptoCurrencyFactory: CardCryptoCurrencyFactory,
) : BaseWalletBalanceFetcher {

    override val fetchingSources: Set<WalletFetchingSource> = setOf(
        WalletFetchingSource.Balance(
            sources = setOf(FetchingSource.NETWORK, FetchingSource.QUOTE),
        ),
    )

    override suspend fun getCryptoCurrencies(userWallet: UserWallet): Set<CryptoCurrency> {
        val coldWallet = userWallet.requireColdWallet()

        val currencies = cardCryptoCurrencyFactory.createCurrenciesForSingleCurrencyCardWithToken(coldWallet)

        return currencies.toSet()
    }
}