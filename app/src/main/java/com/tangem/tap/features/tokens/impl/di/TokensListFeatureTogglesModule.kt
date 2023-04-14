package com.tangem.tap.features.tokens.impl.di

import com.tangem.core.featuretoggle.manager.FeatureTogglesManager
import com.tangem.tap.features.tokens.api.featuretoggles.TokensListFeatureToggles
import com.tangem.tap.features.tokens.impl.featuretoggles.DefaultTokensListFeatureToggles
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
[REDACTED_AUTHOR]
 */
@Module
@InstallIn(SingletonComponent::class)
internal object TokensListFeatureTogglesModule {

    @Provides
    @Singleton
    fun providesTokensListFeatureToggles(featureTogglesManager: FeatureTogglesManager): TokensListFeatureToggles {
        return DefaultTokensListFeatureToggles(featureTogglesManager)
    }
}