package com.tangem.features.jointaccount.main.di

import com.tangem.features.jointaccount.main.JointAccountMembersComponent
import com.tangem.features.jointaccount.main.component.DefaultJointAccountMembersComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface JointAccountMainModule {
    @Binds
    fun bindJointAccountMembersComponentFactory(
        factory: DefaultJointAccountMembersComponent.Factory,
    ): JointAccountMembersComponent.Factory
}