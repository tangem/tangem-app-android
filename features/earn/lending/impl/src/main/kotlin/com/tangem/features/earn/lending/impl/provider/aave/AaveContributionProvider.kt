package com.tangem.features.earn.lending.impl.provider.aave

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.balance.BalanceContributionProvider
import com.tangem.domain.models.currency.balance.ContributionResolver
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.isMultiCurrency
import com.tangem.domain.yield.supply.models.YieldMarketToken
import com.tangem.domain.yield.supply.models.status.YieldModuleStatus
import com.tangem.domain.yield.supply.models.status.findFor
import com.tangem.domain.yield.supply.status.YieldModuleStatusProducer
import com.tangem.domain.yield.supply.status.YieldModuleStatusSupplier
import com.tangem.features.earn.api.EarnFeatureToggles
import com.tangem.features.earn.lending.impl.contribution.LendingContribution
import com.tangem.features.earn.lending.impl.product.netRateOf
import com.tangem.features.earn.lending.impl.provider.LendingProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import java.math.BigDecimal
import javax.inject.Inject

/**
 * Reads the stored module statuses and the cached yield markets rather than the Lending market: the market reads the
 * chain on every network update, and this runs for every holder of the coin. The statuses are refreshed by the Lending
 * balance fetcher.
 */
internal class AaveContributionProvider @Inject constructor(
    private val statusSupplier: YieldModuleStatusSupplier,
    private val assetResolver: AaveAssetResolver,
    private val markets: AaveMarkets,
    private val lendingProvider: LendingProvider,
    private val earnFeatureToggles: EarnFeatureToggles,
) : BalanceContributionProvider {

    override fun contributions(userWallet: UserWallet): Flow<ContributionResolver> {
        if (!earnFeatureToggles.isEarnEnabled || !userWallet.isMultiCurrency) return flowOf(ContributionResolver.Empty)

        return combine(
            statusSupplier(YieldModuleStatusProducer.Params(userWallet.walletId)).distinctUntilChanged(),
            marketsOrEmpty(),
        ) { statuses, markets ->
            ContributionResolver { currency, _ -> contributionTo(currency, statuses, markets) }
        }
            .onStart { emit(ContributionResolver.Empty) }
    }

    /** A failed read leaves the position without a rate rather than out of the balance */
    private fun marketsOrEmpty(): Flow<List<YieldMarketToken>> = markets.observeAll()
        .distinctUntilChanged()
        .catch { error ->
            TangemLogger.e("Aave contribution: yield markets read failed", error)
            emit(emptyList())
        }

    /** The base rate: Merkl rewards are paid as separate tokens and never compound into the position */
    private fun contributionTo(
        currency: CryptoCurrency,
        statuses: Set<YieldModuleStatus>,
        markets: List<YieldMarketToken>,
    ): LendingContribution? {
        val asset = assetResolver.resolve(currency) ?: return null
        val status = statuses.findFor(asset.reserveToken) ?: return null
        val position = status.position ?: return null
        val rate = markets.findFor(asset)?.apy?.percentToApy()?.fraction

        return LendingContribution(
            amount = position,
            source = status.source,
            accrualRate = rate?.let(lendingProvider::netRateOf) ?: BigDecimal.ZERO,
        )
    }
}

internal val YieldModuleStatus.position: BigDecimal?
    get() = effectiveProtocolBalance?.takeIf { it.signum() > 0 }