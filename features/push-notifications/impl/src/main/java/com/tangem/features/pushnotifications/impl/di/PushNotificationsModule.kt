package com.tangem.features.pushnotifications.impl.di

import com.tangem.core.decompose.model.Model
import com.tangem.features.pushnotifications.api.PushNotificationsBottomSheetComponent
import com.tangem.features.pushnotifications.api.PushNotificationsComponent
import com.tangem.features.pushnotifications.impl.DefaultPushNotificationsBottomSheetComponent
import com.tangem.features.pushnotifications.impl.DefaultPushNotificationsComponent
import com.tangem.features.pushnotifications.impl.model.PushNotificationsModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
internal interface PushNotificationsModule {

    @Binds
    fun bindBottomSheetComponentFactory(
        impl: DefaultPushNotificationsBottomSheetComponent.Factory,
    ): PushNotificationsBottomSheetComponent.Factory

    @Binds
    fun bindComponentFactory(impl: DefaultPushNotificationsComponent.Factory): PushNotificationsComponent.Factory

    @Binds
    @IntoMap
    @ClassKey(PushNotificationsModel::class)
    fun bindModel(model: PushNotificationsModel): Model
}