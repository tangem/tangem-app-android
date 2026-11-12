package com.tangem.features.onramp.featuretoggles.di

import com.tangem.features.onramp.featuretoggles.DefaultOnrampFeatureToggles
import com.tangem.features.onramp.featuretoggles.OnrampFeatureToggles
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface OnrampFeatureTogglesModule {

    @Binds
    @Singleton
    fun bindOnrampFeatureToggles(impl: DefaultOnrampFeatureToggles): OnrampFeatureToggles
}