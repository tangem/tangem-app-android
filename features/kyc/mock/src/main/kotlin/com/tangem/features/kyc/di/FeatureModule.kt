package com.tangem.features.kyc.di

import com.tangem.features.kyc.KycComponent
import com.tangem.features.kyc.MockKycComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface FeatureModule {

    @Binds
    fun bindComponentFactory(impl: MockKycComponent.Factory): KycComponent.Factory
}