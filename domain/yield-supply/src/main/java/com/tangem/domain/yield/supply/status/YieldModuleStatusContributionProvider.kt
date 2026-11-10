package com.tangem.domain.yield.supply.status

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.balance.BalanceContributionProvider
import com.tangem.domain.models.currency.balance.ContributionResolver
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.yield.supply.YieldSupplyContribution
import com.tangem.domain.tokens.TokensFeatureToggles
import com.tangem.domain.yield.supply.models.status.YieldModuleStatus
import com.tangem.domain.yield.supply.models.status.findFor
import com.tangem.domain.yield.supply.models.status.toYieldSupplyStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Exposes the Yield Module statuses as [YieldSupplyContribution]s of the tokens they belong to.
 *
 * Contributes nothing unless [TokensFeatureToggles.isYieldContributionEnabled]: until then yield supply is still
 * projected from the network status by the legacy provider in `domain/account/status`.
 */
class YieldModuleStatusContributionProvider(
    private val yieldModuleStatusSupplier: YieldModuleStatusSupplier,
    private val tokensFeatureToggles: TokensFeatureToggles,
) : BalanceContributionProvider {

    override fun contributions(userWallet: UserWallet): Flow<ContributionResolver> {
        if (!tokensFeatureToggles.isYieldContributionEnabled) return flowOf(ContributionResolver.Empty)

        return yieldModuleStatusSupplier(YieldModuleStatusProducer.Params(userWalletId = userWallet.walletId))
            .distinctUntilChanged()
            .map { statuses ->
                ContributionResolver { currency, _ -> resolve(statuses = statuses, currency = currency) }
            }
    }

    private fun resolve(statuses: Set<YieldModuleStatus>, currency: CryptoCurrency): YieldSupplyContribution? {
        val token = currency as? CryptoCurrency.Token ?: return null
        val status = statuses.findFor(token) ?: return null

        return YieldSupplyContribution(status = status.toYieldSupplyStatus(), source = status.source)
    }
}