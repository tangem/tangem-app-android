package com.tangem.features.tangempay.di

import com.tangem.features.tangempay.components.DefaultTangemPayDetailsComponent
import com.tangem.features.tangempay.components.TangemPayDetailsComponent
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
    fun bindTangemPayDetailsComponentFactory(
        factory: DefaultTangemPayDetailsComponent.Factory,
    ): TangemPayDetailsComponent.Factory
}