package com.tangem.feature.tokendetails.di

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.feature.tokendetails.DefaultTokenDetailsFeatureToggles
import com.tangem.features.tokendetails.TokenDetailsFeatureToggles
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object TokenDetailsFeatureModule {

    @Provides
    @Singleton
    fun provideTokenDetailsFeatureToggles(featureTogglesManager: FeatureTogglesManager): TokenDetailsFeatureToggles {
        return DefaultTokenDetailsFeatureToggles(featureTogglesManager)
    }
}