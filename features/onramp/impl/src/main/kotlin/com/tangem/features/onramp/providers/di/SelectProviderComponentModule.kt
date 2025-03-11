package com.tangem.features.onramp.providers.di

import com.tangem.features.onramp.providers.DefaultSelectProviderComponent
import com.tangem.features.onramp.providers.SelectProviderComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface SelectProviderComponentModule {

    @Binds
    @Singleton
    fun bindSelectProviderComponentFactory(
        factory: DefaultSelectProviderComponent.Factory,
    ): SelectProviderComponent.Factory
}