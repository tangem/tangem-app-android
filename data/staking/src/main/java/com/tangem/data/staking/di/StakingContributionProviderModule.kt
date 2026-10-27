package com.tangem.data.staking.di

import com.tangem.core.analytics.api.AnalyticsExceptionHandler
import com.tangem.domain.models.currency.balance.BalanceContributionProvider
import com.tangem.domain.staking.StakingIdFactory
import com.tangem.domain.staking.contribution.StakingContributionProvider
import com.tangem.domain.staking.multi.MultiStakingBalanceSupplier
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
internal object StakingContributionProviderModule {

    @Provides
    @IntoSet
    fun provideStakingContributionProvider(
        stakingBalanceSupplier: MultiStakingBalanceSupplier,
        stakingIdFactory: StakingIdFactory,
        analyticsExceptionHandler: AnalyticsExceptionHandler,
    ): BalanceContributionProvider {
        return StakingContributionProvider(
            stakingBalanceSupplier = stakingBalanceSupplier,
            stakingIdFactory = stakingIdFactory,
            analyticsExceptionHandler = analyticsExceptionHandler,
        )
    }
}