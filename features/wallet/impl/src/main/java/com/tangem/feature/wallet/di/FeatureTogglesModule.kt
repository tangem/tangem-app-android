package com.tangem.feature.wallet.di

import com.tangem.core.featuretoggle.manager.FeatureTogglesManager
import com.tangem.feature.wallet.featuretoggle.WalletFeatureToggles
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object FeatureTogglesModule {

    @Provides
    @Singleton
    fun provideWalletFeatureToggles(featureTogglesManager: FeatureTogglesManager): WalletFeatureToggles {
        return WalletFeatureToggles(featureTogglesManager)
    }
}
