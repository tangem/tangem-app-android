package com.tangem.features.yield.supply.impl.subcomponents.notifications.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.yield.supply.impl.subcomponents.notifications.DefaultYieldSupplyNotificationsUpdateTrigger
import com.tangem.features.yield.supply.impl.subcomponents.notifications.YieldSupplyNotificationsUpdateListener
import com.tangem.features.yield.supply.impl.subcomponents.notifications.YieldSupplyNotificationsUpdateTrigger
import com.tangem.features.yield.supply.impl.subcomponents.notifications.model.YieldSupplyNotificationsModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface YieldSupplyNotificationsModule {

    @Binds
    @IntoMap
    @ClassKey(YieldSupplyNotificationsModel::class)
    fun provideYieldSupplyNotificationsModel(impl: YieldSupplyNotificationsModel): Model

    @Binds
    fun provideYieldSupplyNotificationsUpdateListener(
        impl: DefaultYieldSupplyNotificationsUpdateTrigger,
    ): YieldSupplyNotificationsUpdateListener

    @Binds
    fun provideYieldSupplyNotificationsUpdateTrigger(
        impl: DefaultYieldSupplyNotificationsUpdateTrigger,
    ): YieldSupplyNotificationsUpdateTrigger
}