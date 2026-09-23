package com.tangem.features.txhistory.di

import com.tangem.domain.txhistory.TxHistoryFeatureToggles
import com.tangem.features.txhistory.component.DefaultTxHistoryComponent
import com.tangem.features.txhistory.component.DefaultTxHistoryDetailsComponent
import com.tangem.features.txhistory.component.JointCosignRequestsComponent
import com.tangem.features.txhistory.component.TxHistoryComponent
import com.tangem.features.txhistory.component.TxHistoryDetailsComponent
import com.tangem.features.txhistory.joint.operation.JointOperationComponent
import com.tangem.features.txhistory.joint.operation.component.DefaultJointOperationComponent
import com.tangem.features.txhistory.joint.requests.component.DefaultJointCosignRequestsComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface TxHistoryFeatureModule {

    @Binds
    @Singleton
    fun bindComponentFactory(factory: DefaultTxHistoryComponent.Factory): TxHistoryComponent.Factory

    @Binds
    @Singleton
    fun bindTxHistoryFeatureToggle(impl: DefaultTxHistoryFeatureToggles): TxHistoryFeatureToggles

    @Binds
    @Singleton
    fun bindTxHistoryDetailsComponentFactory(
        factory: DefaultTxHistoryDetailsComponent.Factory,
    ): TxHistoryDetailsComponent.Factory

    @Binds
    @Singleton
    fun bindJointCosignRequestsComponentFactory(
        factory: DefaultJointCosignRequestsComponent.Factory,
    ): JointCosignRequestsComponent.Factory

    @Binds
    @Singleton
    fun bindJointOperationComponentFactory(
        factory: DefaultJointOperationComponent.Factory,
    ): JointOperationComponent.Factory
}