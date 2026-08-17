package com.tangem.features.jointaccount.main.di

import com.tangem.features.jointaccount.main.component.DefaultJointAccountMainBlockComponent
import com.tangem.features.jointaccount.main.JointAccountMainBlockComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface JointAccountMainModule {
    @Binds
    fun bindJointAccountMainBlockComponent(
        factory: DefaultJointAccountMainBlockComponent.Factory,
    ): JointAccountMainBlockComponent.Factory
}