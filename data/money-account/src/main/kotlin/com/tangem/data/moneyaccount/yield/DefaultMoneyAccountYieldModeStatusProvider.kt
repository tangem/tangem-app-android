package com.tangem.data.moneyaccount.yield

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.yield.supply.YieldSupplyStatus
import com.tangem.domain.moneyaccount.MoneyAccountFeatureToggles
import com.tangem.domain.moneyaccount.yield.MoneyAccountYieldModeStatusProvider
import com.tangem.domain.tokens.TokensFeatureToggles
import com.tangem.domain.yield.supply.models.status.findFor
import com.tangem.domain.yield.supply.models.status.toYieldSupplyStatus
import com.tangem.domain.yield.supply.status.YieldModuleStatusFetcher
import com.tangem.domain.yield.supply.status.YieldModuleStatusProducer
import com.tangem.domain.yield.supply.status.YieldModuleStatusSupplier
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject

internal class DefaultMoneyAccountYieldModeStatusProvider @Inject constructor(
    private val moneyAccountFeatureToggles: MoneyAccountFeatureToggles,
    private val tokensFeatureToggles: TokensFeatureToggles,
    private val yieldModuleStatusFetcher: YieldModuleStatusFetcher,
    private val yieldModuleStatusSupplier: YieldModuleStatusSupplier,
) : MoneyAccountYieldModeStatusProvider {

    override suspend fun get(
        userWalletId: UserWalletId,
        token: CryptoCurrency.Token,
        networkStatus: NetworkStatus.Verified,
    ): YieldSupplyStatus? {
        if (!moneyAccountFeatureToggles.isYieldModeEnabled) return null
        if (!tokensFeatureToggles.isYieldContributionEnabled) return networkStatus.yieldSupplyStatuses[token.id]

        yieldModuleStatusFetcher(YieldModuleStatusFetcher.Params(userWalletId = userWalletId, tokens = setOf(token)))

        return yieldModuleStatusSupplier.getSyncOrNull(YieldModuleStatusProducer.Params(userWalletId = userWalletId))
            ?.findFor(token)
            ?.toYieldSupplyStatus()
    }
}