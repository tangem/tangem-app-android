package com.tangem.features.virtualaccount.di

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.virtualaccount.DefaultVirtualAccountFeatureToggles
import com.tangem.features.virtualaccount.VirtualAccountFeatureToggles
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object VirtualAccountMainModule {

    @Provides
    @Singleton
    fun provideVirtualAccountFeatureToggles(
        featureTogglesManager: FeatureTogglesManager,
    ): VirtualAccountFeatureToggles {
        return DefaultVirtualAccountFeatureToggles(featureTogglesManager = featureTogglesManager)
    }
}