package com.tangem.features.moneyaccount.onboarding.di

import com.tangem.common.routing.deeplink.DeepLinkHandler
import com.tangem.common.routing.deeplink.DeepLinkKey
import com.tangem.features.moneyaccount.onboarding.component.DefaultMoneyAccountOnboardingComponent
import com.tangem.features.moneyaccount.onboarding.component.MoneyAccountOnboardingComponent
import com.tangem.features.moneyaccount.onboarding.deeplink.DefaultOnboardMoneyAccountDeepLinkHandler
import com.tangem.features.moneyaccount.onboarding.deeplink.OnboardMoneyAccountDeepLinkHandler
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface MoneyAccountOnboardingFeatureModule {

    @Binds
    fun bindFactory(impl: DefaultMoneyAccountOnboardingComponent.Factory): MoneyAccountOnboardingComponent.Factory

    @Binds
    @Singleton
    fun bindOnboardMoneyAccountDeepLinkHandlerFactory(
        impl: DefaultOnboardMoneyAccountDeepLinkHandler.Factory,
    ): OnboardMoneyAccountDeepLinkHandler.Factory

    companion object {

        // Temporary bridge to the legacy factory: once the legacy routing is removed, the handler itself must be
        // bound here as a DeepLinkHandler instead of a lambda over its factory.
        @Provides
        @IntoMap
        @DeepLinkKey(DefaultOnboardMoneyAccountDeepLinkHandler.DEEPLINK_KEY)
        fun provideOnboardMoneyAccountDeepLinkHandler(
            factory: OnboardMoneyAccountDeepLinkHandler.Factory,
        ): DeepLinkHandler = DeepLinkHandler { request -> factory.create(uri = request.uri) }
    }
}