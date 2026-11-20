package com.tangem.features.earn.yield.impl.di

import com.tangem.features.earn.product.api.product.EarnRateSource
import com.tangem.features.earn.yield.impl.YieldModeRateSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
internal interface YieldModeRateSourceModule {

    @Binds
    @IntoSet
    fun bindYieldModeRateSource(source: YieldModeRateSource): EarnRateSource
}