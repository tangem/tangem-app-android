package com.tangem.features.jointaccount.creation.di

import com.tangem.features.jointaccount.creation.component.DefaultJointAccountCreationComponent
import com.tangem.features.jointaccount.creation.JointAccountCreationComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface JointAccountCreationFeatureModule {

    @Binds
    fun bindFactory(impl: DefaultJointAccountCreationComponent.Factory): JointAccountCreationComponent.Factory
}