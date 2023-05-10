package com.tangem.tap.features.customtoken.impl.di

import com.tangem.core.featuretoggle.manager.FeatureTogglesManager
import com.tangem.tap.features.customtoken.api.featuretoggles.CustomTokenFeatureToggles
import com.tangem.tap.features.customtoken.impl.featuretoggles.DefaultCustomTokenFeatureToggles
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
* [REDACTED_AUTHOR]
 */
@Module
@InstallIn(SingletonComponent::class)
internal object CustomTokenFeatureTogglesModule {

    @Provides
    @Singleton
    fun providesCustomTokenFeatureToggles(featureTogglesManager: FeatureTogglesManager): CustomTokenFeatureToggles {
        return DefaultCustomTokenFeatureToggles(featureTogglesManager)
    }
}
