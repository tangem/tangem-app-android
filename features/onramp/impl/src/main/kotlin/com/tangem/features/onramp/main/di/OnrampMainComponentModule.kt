package com.tangem.features.onramp.main.di

import com.tangem.features.onramp.main.DefaultOnrampMainComponent
import com.tangem.features.onramp.main.OnrampMainComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface OnrampMainComponentModule {

    @Binds
    @Singleton
    fun bindOnrampMainComponentFactory(factory: DefaultOnrampMainComponent.Factory): OnrampMainComponent.Factory
}