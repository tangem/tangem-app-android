package com.tangem.tap.features.welcome.di

import com.tangem.tap.features.welcome.component.DefaultWelcomeComponent
import com.tangem.tap.features.welcome.component.WelcomeComponent
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
    fun bindWelcomeComponentFactory(factory: DefaultWelcomeComponent.Factory): WelcomeComponent.Factory
}