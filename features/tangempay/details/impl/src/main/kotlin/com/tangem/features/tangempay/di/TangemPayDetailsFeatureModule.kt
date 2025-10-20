package com.tangem.features.tangempay.di

import com.tangem.features.tangempay.components.DefaultTangemPayDetailsContainerComponent
import com.tangem.features.tangempay.components.TangemPayDetailsContainerComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface TangemPayDetailsFeatureModule {

    @Binds
    @Singleton
    fun bindTangemPayDetailsContainerComponentFactory(
        factory: DefaultTangemPayDetailsContainerComponent.Factory,
    ): TangemPayDetailsContainerComponent.Factory
}