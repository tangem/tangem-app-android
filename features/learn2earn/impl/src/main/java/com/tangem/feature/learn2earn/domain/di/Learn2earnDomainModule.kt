package com.tangem.feature.learn2earn.domain.di

import android.content.Context
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.featuretoggle.manager.FeatureTogglesManager
import com.tangem.feature.learn2earn.data.api.Learn2earnRepository
import com.tangem.feature.learn2earn.data.toggles.DefaultLearn2earnFeatureToggleManager
import com.tangem.feature.learn2earn.data.toggles.Learn2earnFeatureToggleManager
import com.tangem.feature.learn2earn.domain.DefaultLearn2earnInteractor
import com.tangem.feature.learn2earn.domain.api.Learn2earnDependencyProvider
import com.tangem.feature.learn2earn.domain.api.Learn2earnInteractor
import com.tangem.feature.learn2earn.domain.api.WebViewRedirectHandler
import com.tangem.feature.learn2earn.presentation.Learn2earnRouter
import com.tangem.lib.crypto.DerivationManager
import com.tangem.lib.crypto.UserWalletManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal class Learn2earnDomainModule {

    @Provides
    @Singleton
    fun provideRouter(@ApplicationContext context: Context): Learn2earnRouter {
        return Learn2earnRouter(context)
    }

    @Provides
    @Singleton
    fun provideFeatureToggle(featureToggleManager: FeatureTogglesManager): Learn2earnFeatureToggleManager {
        return DefaultLearn2earnFeatureToggleManager(featureToggleManager)
    }

    @Provides
    @Singleton
    fun provideInteractor(
        featureToggleManager: Learn2earnFeatureToggleManager,
        repository: Learn2earnRepository,
        dependencyProvider: Learn2earnDependencyProvider,
        userWalletManager: UserWalletManager,
        analyticsEventHandler: AnalyticsEventHandler,
        derivationManager: DerivationManager,
    ): Learn2earnInteractor {
        return DefaultLearn2earnInteractor(
            featureToggleManager = featureToggleManager,
            repository = repository,
            userWalletManager = userWalletManager,
            derivationManager = derivationManager,
            analytics = analyticsEventHandler,
            dependencyProvider = dependencyProvider,
        )
    }

    @Provides
    @Singleton
    fun provideWebViewHandler(interactor: Learn2earnInteractor): WebViewRedirectHandler {
        return interactor
    }
}
