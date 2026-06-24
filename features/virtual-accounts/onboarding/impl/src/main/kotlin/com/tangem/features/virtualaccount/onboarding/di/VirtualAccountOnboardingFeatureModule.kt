package com.tangem.features.virtualaccount.onboarding.di

import com.tangem.features.virtualaccount.onboarding.component.DefaultVirtualAccountOnboardingComponent
import com.tangem.features.virtualaccount.onboarding.component.VirtualAccountOnboardingComponent
import com.tangem.features.virtualaccount.onboarding.deeplink.DefaultOnboardVirtualAccountsDeepLinkHandler
import com.tangem.features.virtualaccount.onboarding.deeplink.OnboardVirtualAccountsDeepLinkHandler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface VirtualAccountOnboardingFeatureModule {

    @Binds
    fun bindFactory(impl: DefaultVirtualAccountOnboardingComponent.Factory): VirtualAccountOnboardingComponent.Factory

    @Binds
    @Singleton
    fun bindOnboardVirtualAccountsDeepLinkHandlerFactory(
        impl: DefaultOnboardVirtualAccountsDeepLinkHandler.Factory,
    ): OnboardVirtualAccountsDeepLinkHandler.Factory
}