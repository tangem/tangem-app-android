package com.tangem.features.tangempay.eligibility

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface TangemPayEligibilityFeatureModule {

    @Binds
    fun bindFactory(impl: DefaultTangemPayEligibilityComponent.Factory): TangemPayEligibilityComponent.Factory
}