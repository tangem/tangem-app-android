package com.tangem.features.tangempay.di

import com.tangem.features.tangempay.components.DefaultTangemPayOnboardingComponent
import com.tangem.features.tangempay.components.TangemPayOnboardingComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface TangemPayOnboardingFeatureModule {

    @Binds
    fun bindFactory(impl: DefaultTangemPayOnboardingComponent.Factory): TangemPayOnboardingComponent.Factory
}