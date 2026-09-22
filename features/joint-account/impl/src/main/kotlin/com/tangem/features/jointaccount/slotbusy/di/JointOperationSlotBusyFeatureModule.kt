package com.tangem.features.jointaccount.slotbusy.di

import com.tangem.features.jointaccount.slotbusy.JointOperationSlotBusyComponent
import com.tangem.features.jointaccount.slotbusy.component.DefaultJointOperationSlotBusyComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface JointOperationSlotBusyFeatureModule {

    @Binds
    fun bindJointOperationSlotBusyComponentFactory(
        factory: DefaultJointOperationSlotBusyComponent.Factory,
    ): JointOperationSlotBusyComponent.Factory
}