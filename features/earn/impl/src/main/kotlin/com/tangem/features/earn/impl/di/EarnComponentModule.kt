package com.tangem.features.earn.impl.di

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.earn.api.EarnEntryComponent
import com.tangem.features.earn.api.EarnFeatureToggles
import com.tangem.features.earn.impl.DefaultEarnEntryComponent
import com.tangem.features.earn.impl.DefaultEarnFeatureToggles
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface EarnComponentModule {

    @Binds
    @Singleton
    fun bindEntryComponentFactory(impl: DefaultEarnEntryComponent.Factory): EarnEntryComponent.Factory

    companion object {

        @Provides
        @Singleton
        fun provideEarnFeatureToggles(featureTogglesManager: FeatureTogglesManager): EarnFeatureToggles {
            return DefaultEarnFeatureToggles(featureTogglesManager)
        }
    }
}