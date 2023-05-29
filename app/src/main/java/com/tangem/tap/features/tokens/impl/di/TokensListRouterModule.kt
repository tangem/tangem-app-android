package com.tangem.tap.features.tokens.impl.di

import com.tangem.tap.features.customtoken.api.featuretoggles.CustomTokenFeatureToggles
import com.tangem.tap.features.tokens.impl.presentation.router.DefaultTokensListRouter
import com.tangem.tap.features.tokens.impl.presentation.router.TokensListRouter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

/**
[REDACTED_AUTHOR]
 */
@Module
@InstallIn(ViewModelComponent::class)
internal object TokensListRouterModule {

    @Provides
    @ViewModelScoped
    fun provideTokensListRouter(customTokenFeatureToggles: CustomTokenFeatureToggles): TokensListRouter {
        return DefaultTokensListRouter(customTokenFeatureToggles)
    }
}