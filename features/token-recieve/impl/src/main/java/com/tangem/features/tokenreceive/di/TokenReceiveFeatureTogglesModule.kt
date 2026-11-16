package com.tangem.features.tokenreceive.di

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.tokenreceive.DefaultTokenReceiveFeatureToggles
import com.tangem.features.tokenreceive.TokenReceiveFeatureToggles
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object TokenReceiveFeatureTogglesModule {

    @Provides
    @Singleton
    fun provideTokenReceiveFeatureToggles(featureTogglesManager: FeatureTogglesManager): TokenReceiveFeatureToggles {
        return DefaultTokenReceiveFeatureToggles(featureTogglesManager)
    }
}