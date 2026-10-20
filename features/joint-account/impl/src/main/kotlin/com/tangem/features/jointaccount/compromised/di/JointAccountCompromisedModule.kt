package com.tangem.features.jointaccount.compromised.di

import com.tangem.features.jointaccount.compromised.DefaultJointAccountCompromisedAlert
import com.tangem.features.jointaccount.compromised.JointAccountCompromisedAlert
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface JointAccountCompromisedModule {

    @Binds
    fun bindJointAccountCompromisedAlert(alert: DefaultJointAccountCompromisedAlert): JointAccountCompromisedAlert
}