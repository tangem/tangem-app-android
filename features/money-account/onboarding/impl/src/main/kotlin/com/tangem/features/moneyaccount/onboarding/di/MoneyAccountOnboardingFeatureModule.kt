package com.tangem.features.moneyaccount.onboarding.di

import com.tangem.features.moneyaccount.onboarding.component.DefaultMoneyAccountOnboardingComponent
import com.tangem.features.moneyaccount.onboarding.component.MoneyAccountOnboardingComponent
import com.tangem.features.moneyaccount.onboarding.deeplink.DefaultOnboardMoneyAccountDeepLinkHandler
import com.tangem.features.moneyaccount.onboarding.deeplink.OnboardMoneyAccountDeepLinkHandler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
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
}