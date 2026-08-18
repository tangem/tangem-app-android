package com.tangem.features.jointaccount.common.di

import com.tangem.features.jointaccount.common.displayname.DefaultJointAccountDisplayNameComponent
import com.tangem.features.jointaccount.common.JointAccountDisplayNameComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface JointAccountCommonFeatureModule {

    @Binds
    fun bindDisplayNameFactory(
        impl: DefaultJointAccountDisplayNameComponent.Factory,
    ): JointAccountDisplayNameComponent.Factory
}