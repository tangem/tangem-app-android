package com.tangem.features.send.v2.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.send.v2.subcomponents.amount.model.SendAmountModel
import com.tangem.features.send.v2.subcomponents.destination.model.SendDestinationModel
import com.tangem.features.send.v2.subcomponents.fee.model.SendFeeModel
import com.tangem.features.send.v2.subcomponents.notifications.model.NotificationsModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface SendModelModule {

    @Binds
    @IntoMap
    @ClassKey(SendAmountModel::class)
    fun provideSendAmountModel(model: SendAmountModel): Model

    @Binds
    @IntoMap
    @ClassKey(SendDestinationModel::class)
    fun provideSendDestinationModel(model: SendDestinationModel): Model

    @Binds
    @IntoMap
    @ClassKey(SendFeeModel::class)
    fun provideSendFeeModel(model: SendFeeModel): Model

    @Binds
    @IntoMap
    @ClassKey(NotificationsModel::class)
    fun provideNotificationsModel(model: NotificationsModel): Model
}