package com.tangem.features.jointaccount.join.di

import com.tangem.features.jointaccount.join.component.DefaultJointAccountJoinComponent
import com.tangem.features.jointaccount.join.JointAccountJoinComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface JointAccountJoinFeatureModule {

    @Binds
    fun bindFactory(impl: DefaultJointAccountJoinComponent.Factory): JointAccountJoinComponent.Factory
}