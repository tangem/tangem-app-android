package com.tangem.domain.account.status.contribution

import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.yield.supply.YieldSupplyContribution
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * Exposes yield supply as a [com.tangem.domain.models.currency.balance.BalanceContribution].
 *
 * Interim source: yield statuses ride along the wallet-manager fetch and reach us inside
 * [NetworkStatus.Verified.yieldSupplyStatuses], so this provider has no data source of its own — it emits a single
 * stateless frame and projects out of the network status the producer already holds. When yield gets its own store
 * only this class changes.
 *
 * The contribution adds **zero** to any total; it exists so that freshness and the partition data travel through
 * the same generic channel as every other extra balance.
 *
 * [REDACTED_TODO_COMMENT]
 */
internal class YieldSupplyContributionProvider @Inject constructor() : BalanceContributionProvider {

    override fun contributions(userWallet: UserWallet): Flow<ContributionResolver> = flowOf(RESOLVER)

    private companion object {

        val RESOLVER = ContributionResolver { currency, networkStatus ->
            val verified = networkStatus?.value as? NetworkStatus.Verified

            verified?.yieldSupplyStatuses?.get(currency.id)?.let { status ->
                YieldSupplyContribution(status = status, source = verified.source)
            }
        }
    }
}