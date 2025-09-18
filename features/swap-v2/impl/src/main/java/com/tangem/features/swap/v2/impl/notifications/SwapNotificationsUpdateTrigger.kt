package com.tangem.features.swap.v2.impl.notifications

import com.tangem.features.swap.v2.impl.notifications.SwapNotificationsComponent.Params.SwapNotificationData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

internal interface SwapNotificationsUpdateListener {

    /** Flow triggers swap notifications update */
    val updateTriggerFlow: Flow<SwapNotificationData>

    /** Flow returns whether there is swap error notifications */
    val hasErrorFlow: Flow<Boolean>
}

internal interface SwapNotificationsUpdateTrigger {
    /** Trigger return callback with check result */
    suspend fun callbackHasError(hasError: Boolean)

    /** Trigger check swap notifications */
    suspend fun triggerUpdate(data: SwapNotificationData)
}

@Singleton
internal class DefaultSwapNotificationsUpdateTrigger @Inject constructor() :
    SwapNotificationsUpdateListener,
    SwapNotificationsUpdateTrigger {

    override val updateTriggerFlow: SharedFlow<SwapNotificationData>
        field = MutableSharedFlow<SwapNotificationData>()

    override val hasErrorFlow: SharedFlow<Boolean>
        field = MutableSharedFlow<Boolean>()

    override suspend fun callbackHasError(hasError: Boolean) {
        hasErrorFlow.emit(hasError)
    }

    override suspend fun triggerUpdate(data: SwapNotificationData) {
        updateTriggerFlow.emit(data)
    }
}