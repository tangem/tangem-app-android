package com.tangem.features.earn.lending.impl.provider.aave

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.balance.BalanceContributionProvider
import com.tangem.domain.models.currency.balance.ContributionResolver
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.isMultiCurrency
import com.tangem.domain.yield.supply.models.status.YieldModuleStatus
import com.tangem.domain.yield.supply.models.status.findFor
import com.tangem.domain.yield.supply.status.YieldModuleStatusProducer
import com.tangem.domain.yield.supply.status.YieldModuleStatusSupplier
import com.tangem.features.earn.api.EarnFeatureToggles
import com.tangem.features.earn.lending.impl.contribution.LendingContribution
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import java.math.BigDecimal
import javax.inject.Inject

/**
 * Reads the stored module statuses rather than the Lending market: the market reads the chain on every network
 * update, and this runs for every holder of the coin. The statuses are refreshed by the Lending balance fetcher.
 */
internal class AaveContributionProvider @Inject constructor(
    private val statusSupplier: YieldModuleStatusSupplier,
    private val assetResolver: AaveAssetResolver,
    private val earnFeatureToggles: EarnFeatureToggles,
) : BalanceContributionProvider {

    override fun contributions(userWallet: UserWallet): Flow<ContributionResolver> {
        if (!earnFeatureToggles.isEarnEnabled || !userWallet.isMultiCurrency) return flowOf(ContributionResolver.Empty)

        return statusSupplier(YieldModuleStatusProducer.Params(userWallet.walletId))
            .distinctUntilChanged()
            .map<Set<YieldModuleStatus>, ContributionResolver> { statuses ->
                ContributionResolver { currency, _ -> statuses.contributionTo(currency) }
            }
            .onStart { emit(ContributionResolver.Empty) }
    }

    private fun Set<YieldModuleStatus>.contributionTo(currency: CryptoCurrency): LendingContribution? {
        val asset = assetResolver.resolve(currency) ?: return null
        val status = findFor(asset.reserveToken) ?: return null
        val position = status.position ?: return null

        return LendingContribution(amount = position, source = status.source)
    }
}

internal val YieldModuleStatus.position: BigDecimal?
    get() = effectiveProtocolBalance?.takeIf { it.signum() > 0 }