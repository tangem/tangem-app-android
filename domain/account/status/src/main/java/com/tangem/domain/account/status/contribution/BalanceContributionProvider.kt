package com.tangem.domain.account.status.contribution

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.balance.BalanceContribution
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.models.wallet.UserWallet
import kotlinx.coroutines.flow.Flow

/**
 * Source of one kind of extra balance — staking, yield supply, and whatever comes next.
 *
 * Registered with Hilt `@Binds @IntoSet`, so introducing a balance type means adding a provider: neither the
 * status producer nor `domain/models` has to change. Gated by `TWI_1717_BALANCE_CONTRIBUTIONS`; while the toggle
 * is off the producer never collects these flows and keeps its legacy staking join.
 *
 * A provider subscribes only to **its own** data source and emits a [ContributionResolver] per frame. It is the
 * producer — which already holds a consistent snapshot of network statuses — that asks the resolver about each
 * currency, so providers neither duplicate those subscriptions nor risk mixing two different snapshots.
 */
interface BalanceContributionProvider {

    /**
     * Frames of everything this provider knows for [userWallet].
     *
     * Takes the whole wallet rather than its id because the answer can depend on wallet properties — staking, for
     * one, only applies to multi-currency wallets. Providers with no reactive source of their own (yield supply
     * projects from the network status handed to the resolver) simply emit a single frame.
     */
    fun contributions(userWallet: UserWallet): Flow<ContributionResolver>
}

/** Resolves one provider's frame against a single currency. Returns `null` when it contributes nothing to it. */
fun interface ContributionResolver {

    fun resolve(currency: CryptoCurrency, networkStatus: NetworkStatus?): BalanceContribution?

    companion object {

        /** Contributes nothing to any currency — e.g. staking on a single-currency wallet. */
        val Empty = ContributionResolver { _, _ -> null }
    }
}