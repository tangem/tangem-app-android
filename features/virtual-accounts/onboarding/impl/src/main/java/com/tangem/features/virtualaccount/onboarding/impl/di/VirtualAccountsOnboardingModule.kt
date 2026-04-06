package com.tangem.features.virtualaccount.onboarding.impl.di

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.virtualaccount.onboarding.api.VirtualAccountsFeatureToggles
import com.tangem.features.virtualaccount.onboarding.impl.DefaultVirtualAccountsFeatureToggles
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object VirtualAccountsOnboardingModule {

    @Provides
    @Singleton
    fun provideVirtualAccountsFeatureToggles(
        featureTogglesManager: FeatureTogglesManager,
    ): VirtualAccountsFeatureToggles {
        return DefaultVirtualAccountsFeatureToggles(featureTogglesManager)
    }
}