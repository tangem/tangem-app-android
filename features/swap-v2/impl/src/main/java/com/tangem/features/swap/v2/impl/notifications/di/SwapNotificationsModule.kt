package com.tangem.features.swap.v2.impl.notifications.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.swap.v2.impl.notifications.DefaultSwapNotificationsUpdateTrigger
import com.tangem.features.swap.v2.impl.notifications.SwapNotificationsUpdateListener
import com.tangem.features.swap.v2.impl.notifications.SwapNotificationsUpdateTrigger
import com.tangem.features.swap.v2.impl.notifications.model.SwapNotificationsModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
@InstallIn(ModelComponent::class)
internal interface SwapNotificationsModule {

    @Binds
    @IntoMap
    @ClassKey(SwapNotificationsModel::class)
    fun provideSwapNotificationsModel(impl: SwapNotificationsModel): Model
}

@Module
@InstallIn(SingletonComponent::class)
internal interface SwapNotificationsSingletonModule {
    @Singleton
    @Binds
    fun bindsSwapNotificationsUpdateTrigger(impl: DefaultSwapNotificationsUpdateTrigger): SwapNotificationsUpdateTrigger

    @Singleton
    @Binds
    fun bindsSwapNotificationsUpdateListener(
        impl: DefaultSwapNotificationsUpdateTrigger,
    ): SwapNotificationsUpdateListener
}