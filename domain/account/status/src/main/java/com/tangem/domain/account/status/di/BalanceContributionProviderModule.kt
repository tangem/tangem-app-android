package com.tangem.domain.account.status.di

import com.tangem.domain.account.status.contribution.YieldSupplyContributionProvider
import com.tangem.domain.models.currency.balance.BalanceContributionProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import dagger.multibindings.Multibinds

/**
 * Declares the set of extra-balance sources the status pipeline consumes. A balance type contributes to it with an
 * `@IntoSet` binding — staking from its own module (`data/staking`), yield supply still from here until it moves to
 * the yield module.
 */
@Module
@InstallIn(SingletonComponent::class)
internal interface BalanceContributionProviderModule {

    /**
     * Declares the set so it resolves even with zero providers installed, which is what makes the
     * `contributionProviders.isEmpty()` guard in `DefaultSingleAccountStatusListProducer` reachable rather than
     * dead — a provider can be removed without breaking the graph.
     */
    @Multibinds
    fun balanceContributionProviders(): Set<BalanceContributionProvider>

    @Binds
    @IntoSet
    fun bindYieldSupplyContributionProvider(impl: YieldSupplyContributionProvider): BalanceContributionProvider
}