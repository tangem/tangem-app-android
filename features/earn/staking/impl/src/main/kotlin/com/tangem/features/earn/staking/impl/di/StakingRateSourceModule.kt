package com.tangem.features.earn.staking.impl.di

import com.tangem.features.earn.product.api.product.EarnRateSource
import com.tangem.features.earn.staking.impl.StakingRateSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
internal interface StakingRateSourceModule {

    @Binds
    @IntoSet
    fun bindStakingRateSource(source: StakingRateSource): EarnRateSource
}