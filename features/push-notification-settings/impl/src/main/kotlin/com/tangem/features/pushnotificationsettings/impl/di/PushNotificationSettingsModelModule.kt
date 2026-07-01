package com.tangem.features.pushnotificationsettings.impl.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.pushnotificationsettings.impl.model.PushNotificationSettingsModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface PushNotificationSettingsModelModule {

    @Binds
    @IntoMap
    @ClassKey(PushNotificationSettingsModel::class)
    fun bindPushNotificationSettingsModel(model: PushNotificationSettingsModel): Model
}