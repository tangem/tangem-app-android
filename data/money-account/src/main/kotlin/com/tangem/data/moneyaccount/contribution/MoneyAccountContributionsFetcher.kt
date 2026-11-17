package com.tangem.data.moneyaccount.contribution

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.balance.BalanceContributionFetcher
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

/** Refreshes the Money account contributions — the fetching half of [MoneyAccountContributionsResolver]. */
internal class MoneyAccountContributionsFetcher @Inject constructor(
    @MoneyAccountContributions
    private val contributionFetchers: List<@JvmSuppressWildcards BalanceContributionFetcher>,
    private val policy: MoneyAccountContributionPolicy,
) {

    /** Refreshes the contributions of [currency]; a failed one is logged and never fails the Money account fetch. */
    suspend fun fetch(userWalletId: UserWalletId, currency: CryptoCurrency.Token) {
        if (!policy.isEnabled) return

        coroutineScope {
            contributionFetchers
                .map { fetcher ->
                    async {
                        fetcher.fetch(userWalletId = userWalletId, currencies = setOf(currency)).onLeft { error ->
                            TangemLogger.e("Money account ${fetcher.javaClass.simpleName} fetch failed", error)
                        }
                    }
                }
                .awaitAll()
        }
    }
}