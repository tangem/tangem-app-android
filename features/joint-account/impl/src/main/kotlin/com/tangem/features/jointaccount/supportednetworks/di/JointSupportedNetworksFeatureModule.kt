package com.tangem.features.jointaccount.supportednetworks.di

import com.tangem.features.jointaccount.supportednetworks.JointSupportedNetworksComponent
import com.tangem.features.jointaccount.supportednetworks.component.DefaultJointSupportedNetworksComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface JointSupportedNetworksFeatureModule {

    @Binds
    fun bindJointSupportedNetworksComponentFactory(
        factory: DefaultJointSupportedNetworksComponent.Factory,
    ): JointSupportedNetworksComponent.Factory
}