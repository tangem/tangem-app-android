package com.tangem.features.onramp.selecttoken.di

import com.tangem.features.onramp.selecttoken.DefaultOnrampSelectTokenComponent
import com.tangem.features.onramp.selecttoken.OnrampSelectTokenComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface OnrampSelectTokenComponentModule {

    @Binds
    @Singleton
    fun bindOnrampSelectTokenComponentFactory(
        factory: DefaultOnrampSelectTokenComponent.Factory,
    ): OnrampSelectTokenComponent.Factory
}