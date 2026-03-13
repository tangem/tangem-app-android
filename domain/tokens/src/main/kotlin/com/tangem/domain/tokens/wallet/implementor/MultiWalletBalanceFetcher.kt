package com.tangem.domain.tokens.wallet.implementor

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.tokens.MultiWalletAccountListFetcher
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesProducer
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesSupplier
import com.tangem.domain.tokens.wallet.BaseWalletBalanceFetcher
import com.tangem.domain.tokens.wallet.FetchingSource
import kotlinx.coroutines.flow.firstOrNull
import timber.log.Timber

/**
 * Implementation of [BaseWalletBalanceFetcher] for MULTI-CURRENCY wallet
 *
 * @property multiWalletAccountListFetcher multi wallet fetcher of crypto currencies
 * @property multiWalletCryptoCurrenciesSupplier multi wallet supplier of crypto currencies
 *
[REDACTED_AUTHOR]
 */
internal class MultiWalletBalanceFetcher(
    private val multiWalletAccountListFetcher: MultiWalletAccountListFetcher,
    private val multiWalletCryptoCurrenciesSupplier: MultiWalletCryptoCurrenciesSupplier,
) : BaseWalletBalanceFetcher {

    override val fetchingSources: Set<FetchingSource> = setOf(
        FetchingSource.NETWORK,
        FetchingSource.QUOTE,
        FetchingSource.STAKING,
        FetchingSource.TANGEM_PAY,
    )

    override suspend fun getCryptoCurrencies(userWallet: UserWallet): Set<CryptoCurrency> {
        val userWalletId = userWallet.walletId

        multiWalletAccountListFetcher(
            params = MultiWalletAccountListFetcher.Params(userWalletId = userWalletId),
        )
            .onLeft(Timber::e)

        return multiWalletCryptoCurrenciesSupplier(
            params = MultiWalletCryptoCurrenciesProducer.Params(userWalletId = userWalletId),
        )
            .firstOrNull()
            .orEmpty()
    }
}