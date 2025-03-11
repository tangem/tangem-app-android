package com.tangem.features.onramp.success.di

import com.tangem.features.onramp.component.OnrampSuccessComponent
import com.tangem.features.onramp.success.DefaultOnrampSuccessComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface OnrampSuccessComponentModule {
    @Binds
    @Singleton
    fun bindOnrampSuccessComponentFactory(
        factory: DefaultOnrampSuccessComponent.Factory,
    ): OnrampSuccessComponent.Factory
}