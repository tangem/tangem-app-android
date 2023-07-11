package com.tangem.feature.tokendetails.di

import com.tangem.core.featuretoggle.manager.FeatureTogglesManager
import com.tangem.feature.tokendetails.featuretoggles.DefaultTokenDetailsFeatureToggles
import com.tangem.features.tokendetails.featuretoggles.TokenDetailsFeatureToggles
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object TokenDetailsFeatureTogglesModule {

    @Provides
    @Singleton
    fun provideTokenDetailsFeatureToggles(featureTogglesManager: FeatureTogglesManager): TokenDetailsFeatureToggles {
        return DefaultTokenDetailsFeatureToggles(featureTogglesManager = featureTogglesManager)
    }
}
