package com.tangem.features.onboarding.v2.addresssync.di

import com.tangem.features.onboarding.v2.addresssync.AddressSyncComponent
import com.tangem.features.onboarding.v2.addresssync.DefaultAddressSyncComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface AddressSyncComponentModule {

    @Binds
    @Singleton
    fun bindAddressSyncComponentFactory(factory: DefaultAddressSyncComponent.Factory): AddressSyncComponent.Factory
}