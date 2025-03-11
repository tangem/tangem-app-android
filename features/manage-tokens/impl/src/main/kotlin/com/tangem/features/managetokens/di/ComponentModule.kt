package com.tangem.features.managetokens.di

import com.tangem.features.managetokens.component.*
import com.tangem.features.managetokens.component.impl.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface ComponentModule {

    @Binds
    @Singleton
    fun bindManageTokensComponentFactory(factory: DefaultManageTokensComponent.Factory): ManageTokensComponent.Factory

    @Binds
    @Singleton
    fun bindOnboardingManageTokensComponentFactory(
        factory: DefaultOnboardingManageTokensComponent.Factory,
    ): OnboardingManageTokensComponent.Factory

    @Binds
    @Singleton
    fun bindAddCustomTokenComponentFactory(
        factory: DefaultAddCustomTokenComponent.Factory,
    ): AddCustomTokenComponent.Factory

    @Binds
    @Singleton
    fun bindCustomTokenSelectorComponentFactory(
        factory: DefaultCustomTokenSelectorComponent.Factory,
    ): CustomTokenSelectorComponent.Factory

    @Binds
    @Singleton
    fun bindCustomTokenFormComponentFactory(
        factory: DefaultCustomTokenFormComponent.Factory,
    ): CustomTokenFormComponent.Factory

    @Binds
    @Singleton
    fun bindCustomTokenDerivationInputComponentFactory(
        factory: DefaultCustomTokenDerivationInputComponent.Factory,
    ): CustomTokenDerivationInputComponent.Factory
}