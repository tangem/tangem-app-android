package com.tangem.features.yield.supply.impl.subcomponents.notifications

import com.tangem.features.yield.supply.impl.subcomponents.notifications.entity.YieldSupplyNotificationData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

interface YieldSupplyNotificationsUpdateTrigger {

    /** Flow returns whether there is error notifications */
    val hasErrorFlow: Flow<Boolean>

    /** Trigger fee check reload */
    suspend fun triggerUpdate(data: YieldSupplyNotificationData)
}

interface YieldSupplyNotificationsUpdateListener {
    /** Flow triggers notifications update */
    val updateTriggerFlow: Flow<YieldSupplyNotificationData>

    /** Trigger return callback with check result */
    suspend fun callbackHasError(hasError: Boolean)
}

@Singleton
internal class DefaultYieldSupplyNotificationsUpdateTrigger @Inject constructor() :
    YieldSupplyNotificationsUpdateTrigger,
    YieldSupplyNotificationsUpdateListener {

    override val hasErrorFlow: Flow<Boolean>
        field = MutableSharedFlow()

    override val updateTriggerFlow: Flow<YieldSupplyNotificationData>
        field = MutableSharedFlow()

    override suspend fun triggerUpdate(data: YieldSupplyNotificationData) {
        updateTriggerFlow.emit(data)
    }

    override suspend fun callbackHasError(hasError: Boolean) {
        hasErrorFlow.emit(hasError)
    }
}