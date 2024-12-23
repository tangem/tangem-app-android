package com.tangem.features.onramp.confirmresidency.di

import com.tangem.features.onramp.confirmresidency.ConfirmResidencyComponent
import com.tangem.features.onramp.confirmresidency.DefaultConfirmResidencyComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface OnrampConfirmResidencyComponentModule {

    @Binds
    @Singleton
    fun bindConfirmResidencyComponentFactory(
        factory: DefaultConfirmResidencyComponent.Factory,
    ): ConfirmResidencyComponent.Factory
}