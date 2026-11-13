package com.tangem.features.earn.lending.impl.supplier

import arrow.core.Either
import arrow.core.right
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.balance.BalanceContributionFetcher
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.earn.api.EarnFeatureToggles
import com.tangem.features.earn.lending.impl.provider.LendingProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

/**
 * Re-reads the Lending positions with every balance update. The provider may hold a coin as a wrapped token the
 * portfolio does not list, so the regular refresh of the portfolio currencies would never read such a position.
 *
 * Always answers `Right`: it runs for every holder of the coin, and a `Left` would abort the rest of the wallet refresh
 * (payment, prediction and collectibles accounts) for users who may not use Lending at all. A failed read is already
 * logged and kept as a stale position by the provider.
 */
internal class LendingBalanceFetcher @Inject constructor(
    private val provider: LendingProvider,
    private val earnFeatureToggles: EarnFeatureToggles,
) : BalanceContributionFetcher {

    override suspend fun fetch(
        userWalletId: UserWalletId,
        currencies: Collection<CryptoCurrency>,
    ): Either<Throwable, Unit> {
        if (!earnFeatureToggles.isEarnEnabled || !provider.isAvailable) return Unit.right()

        val assets = currencies.mapNotNullTo(hashSetOf(), provider::resolveAsset)

        coroutineScope {
            assets
                .map { asset -> async { provider.refreshPosition(userWalletId, asset) } }
                .awaitAll()
        }

        return Unit.right()
    }
}