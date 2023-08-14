package com.tangem.tap.features.shop.di

import com.tangem.core.featuretoggle.manager.FeatureTogglesManager
import com.tangem.tap.features.shop.toggles.DefaultShopifyFeatureToggleManager
import com.tangem.tap.features.shop.toggles.ShopifyFeatureToggleManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object ShopifyTogglesModule {

    @Provides
    @Singleton
    fun provideDefaultShopifyFeatureToggleManager(
        featureToggleManager: FeatureTogglesManager,
    ): ShopifyFeatureToggleManager {
        return DefaultShopifyFeatureToggleManager(featureToggleManager)
    }
}