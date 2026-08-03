package com.tangem.domain.account.status.contribution

import com.tangem.core.analytics.api.AnalyticsExceptionHandler
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.balance.BalanceContribution
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.models.network.getAddress
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.models.staking.StakingID
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.isMultiCurrency
import com.tangem.domain.staking.StakingIdFactory
import com.tangem.domain.staking.multi.MultiStakingBalanceProducer
import com.tangem.domain.staking.multi.MultiStakingBalanceSupplier
import com.tangem.domain.staking.single.SingleStakingBalanceProducer.Companion.selectStakingBalance
import com.tangem.lib.crypto.BlockchainUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Exposes staking balances as [BalanceContribution]s.
 *
 * Owns everything staking-specific about attaching a balance to a currency: resolving the [StakingID] from the
 * currency and its address, picking one balance when several share an id, narrowing a StakeKit balance to the
 * items that belong to this currency, and resolving the per-network "is the stake already inside the network
 * balance?" rule.
 *
 * The same narrowing still exists in `CryptoCurrencyStatusFactory` for the toggle-off path. That duplication is
 * deliberate: the legacy copy stays frozen and is deleted whole when the toggle goes away, while this one is the
 * long-term home (and moves into the staking module in a later phase).
 *
 * [REDACTED_TODO_COMMENT]
 */
internal class StakingContributionProvider @Inject constructor(
    private val stakingBalanceSupplier: MultiStakingBalanceSupplier,
    private val stakingIdFactory: StakingIdFactory,
    private val analyticsExceptionHandler: AnalyticsExceptionHandler,
) : BalanceContributionProvider {

    override fun contributions(userWallet: UserWallet): Flow<ContributionResolver> {
        if (!userWallet.isMultiCurrency) return flowOf(ContributionResolver.Empty)

        return stakingBalanceSupplier(MultiStakingBalanceProducer.Params(userWallet.walletId))
            .map { balances -> balances.groupBy(StakingBalance::stakingId) }
            .distinctUntilChanged()
            .map { balancesByStakingId ->
                ContributionResolver { currency, networkStatus ->
                    resolve(
                        balancesByStakingId = balancesByStakingId,
                        currency = currency,
                        networkStatus = networkStatus,
                    )
                }
            }
    }

    private fun resolve(
        balancesByStakingId: Map<StakingID, List<StakingBalance>>,
        currency: CryptoCurrency,
        networkStatus: NetworkStatus?,
    ): BalanceContribution? {
        val stakingId = stakingIdFactory.create(
            currencyId = currency.id,
            defaultAddress = networkStatus.getAddress(),
        ).getOrNull() ?: return null

        val balances = balancesByStakingId[stakingId] ?: return null

        val balance = selectStakingBalance(
            currentStakingId = stakingId,
            currentBalances = balances,
            analyticsExceptionHandler = analyticsExceptionHandler,
        ) as? StakingBalance.Data ?: return null

        return balance.narrowTo(currency = currency, address = networkStatus.getAddress())
    }

    /**
     * Keeps only what belongs to [currency] at [address], stamped with the per-network rule resolved from the
     * currency's own network. Returns `null` when nothing is left to contribute.
     */
    private fun StakingBalance.Data.narrowTo(currency: CryptoCurrency, address: String?): StakingBalance.Data? {
        if (stakingId.address != address) return null

        val isStakedIncludedInNetworkBalance = !BlockchainUtils.isIncludeStakingTotalBalance(
            networkId = currency.network.rawId,
        )

        return when (this) {
            is StakingBalance.Data.StakeKit -> {
                val currencyItems = balance.items.filter { it.token.coinGeckoId == currency.id.rawCurrencyId?.value }

                if (currencyItems.isEmpty()) {
                    null
                } else {
                    copy(
                        balance = balance.copy(items = currencyItems),
                        isStakedIncludedInNetworkBalance = isStakedIncludedInNetworkBalance,
                    )
                }
            }
            is StakingBalance.Data.P2PEthPool -> {
                copy(isStakedIncludedInNetworkBalance = isStakedIncludedInNetworkBalance)
            }
        }
    }
}