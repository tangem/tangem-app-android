package com.tangem.features.swap.v2.impl.featuretoggles.di

import com.tangem.features.swap.v2.api.SwapV2FeatureToggles
import com.tangem.features.swap.v2.impl.featuretoggles.DefaultSwapV2FeatureToggles
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface SwapV2FeatureTogglesModule {

    @Binds
    @Singleton
    fun bindSwapV2FeatureToggles(impl: DefaultSwapV2FeatureToggles): SwapV2FeatureToggles
}