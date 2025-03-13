package com.tangem.features.nft

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object NFTFeatureModule {

    @Provides
    @Singleton
    fun provideFeatureToggles(featureTogglesManager: FeatureTogglesManager): NFTFeatureToggles {
        return DefaultNFTFeatureToggles(featureTogglesManager)
    }
}