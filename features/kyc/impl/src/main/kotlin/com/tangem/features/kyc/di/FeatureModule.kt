package com.tangem.features.kyc.di

import com.tangem.features.kyc.DefaultKycComponent
import com.tangem.features.kyc.KycComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface FeatureModule {

    @Binds
    fun bindComponentFactory(impl: DefaultKycComponent.Factory): KycComponent.Factory
}