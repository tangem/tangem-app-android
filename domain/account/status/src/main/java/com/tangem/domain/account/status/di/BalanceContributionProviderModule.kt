package com.tangem.domain.account.status.di

import com.tangem.domain.account.status.contribution.BalanceContributionProvider
import com.tangem.domain.account.status.contribution.StakingContributionProvider
import com.tangem.domain.account.status.contribution.YieldSupplyContributionProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/**
 * Registry of every extra-balance source. Adding a balance type = one more `@Binds @IntoSet` line here plus the
 * provider itself; no other file in the status pipeline changes.
 */
@Module
@InstallIn(SingletonComponent::class)
internal interface BalanceContributionProviderModule {

    @Binds
    @IntoSet
    fun bindStakingContributionProvider(impl: StakingContributionProvider): BalanceContributionProvider

    @Binds
    @IntoSet
    fun bindYieldSupplyContributionProvider(impl: YieldSupplyContributionProvider): BalanceContributionProvider
}