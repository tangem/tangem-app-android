package com.tangem.features.onramp.settings.di

import com.tangem.features.onramp.settings.DefaultOnrampSettingsComponent
import com.tangem.features.onramp.settings.OnrampSettingsComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface OnrampSettingsComponentModule {

    @Binds
    @Singleton
    fun bindOnrampSettingsComponentFactory(
        factory: DefaultOnrampSettingsComponent.Factory,
    ): OnrampSettingsComponent.Factory
}