package com.tangem.domain.yield.supply.status

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.balance.BalanceContributionProvider
import com.tangem.domain.models.currency.balance.ContributionResolver
import com.tangem.domain.models.network.NetworkStatus
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
import java.math.BigDecimal

/**
 * Exposes the Yield Module statuses as [YieldSupplyContribution]s of the tokens they belong to.
 *
 * Contributes nothing unless [TokensFeatureToggles.isYieldContributionEnabled]: until then yield supply is still
 * projected from the network status by the legacy provider in `domain/account/status`.
 *
 * The token amount of an active position becomes the wallet + supplied total the position was read with, rather than
 * the network amount + the supplied part: the two come from separate fetches, and funds that moved between the wallet
 * and the module in between would otherwise be counted twice (exit) or not at all (send from the module). A
 * wallet↔module move leaves that total unchanged, so a stale position still shows the right one.
 */
class YieldModuleStatusContributionProvider(
    private val yieldModuleStatusSupplier: YieldModuleStatusSupplier,
    private val tokensFeatureToggles: TokensFeatureToggles,
) : BalanceContributionProvider {

    override fun contributions(userWallet: UserWallet): Flow<ContributionResolver> {
        if (!tokensFeatureToggles.isYieldContributionEnabled) return flowOf(ContributionResolver.Empty)

        return yieldModuleStatusSupplier(YieldModuleStatusProducer.Params(userWalletId = userWallet.walletId))
            .distinctUntilChanged()
            .map { yieldStatuses ->
                ContributionResolver { currency, networkStatus ->
                    resolve(yieldStatuses = yieldStatuses, currency = currency, networkStatus = networkStatus)
                }
            }
    }

    private fun resolve(
        yieldStatuses: Set<YieldModuleStatus>,
        currency: CryptoCurrency,
        networkStatus: NetworkStatus?,
    ): YieldSupplyContribution? {
        val token = currency as? CryptoCurrency.Token ?: return null
        val yieldStatus = yieldStatuses.findFor(token) ?: return null

        return YieldSupplyContribution(
            status = yieldStatus.toYieldSupplyStatus(),
            source = yieldStatus.source,
            spendableDelta = yieldStatus.spendableDelta(currency = currency, networkStatus = networkStatus),
        )
    }

    private fun YieldModuleStatus.spendableDelta(currency: CryptoCurrency, networkStatus: NetworkStatus?): BigDecimal {
        val verified = networkStatus?.value as? NetworkStatus.Verified ?: return BigDecimal.ZERO
        val networkAmount = (verified.amounts[currency.id] as? NetworkStatus.Amount.Loaded)?.value
            ?: return BigDecimal.ZERO

        // An SDK not yet reading the wallet balance alone (or a network status cached before it did) reports an
        // active token's amount as the module's effective balance and attaches its yield status
        val isSuppliedInsideNetworkAmount = verified.yieldSupplyStatuses[currency.id]?.isActive == true

        if (!isActive || isSuppliedInsideNetworkAmount) return BigDecimal.ZERO

        // A position stored before it carried the paired total falls back to the supplied part until it is refreshed
        return effectiveBalance?.minus(networkAmount) ?: effectiveProtocolBalance ?: BigDecimal.ZERO
    }
}