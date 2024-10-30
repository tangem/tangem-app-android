package com.tangem.features.onramp.selecttoken.di

import com.tangem.features.onramp.selecttoken.DefaultOnrampOperationComponent
import com.tangem.features.onramp.selecttoken.OnrampOperationComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface OnrampOperationComponentModule {

    @Binds
    @Singleton
    fun bindOnrampOperationComponentFactory(
        factory: DefaultOnrampOperationComponent.Factory,
    ): OnrampOperationComponent.Factory
}