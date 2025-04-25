package com.tangem.features.send.v2.subcomponents.notifications

import com.tangem.features.send.v2.subcomponents.notifications.model.NotificationData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

interface NotificationsUpdateTrigger {
    /** Flow triggers notifications update */
    val updateTriggerFlow: Flow<NotificationData>

    /** Flow returns whether there is error notifications */
    val hasErrorFlow: Flow<Boolean>

    /** Trigger return callback with check result */
    suspend fun callbackHasError(hasError: Boolean)

    /** Trigger fee check reload */
    suspend fun triggerUpdate(data: NotificationData)
}

@Singleton
internal class DefaultNotificationsUpdateTrigger @Inject constructor() : NotificationsUpdateTrigger {

    private val _updateTriggerFlow = MutableSharedFlow<NotificationData>()
    override val updateTriggerFlow = _updateTriggerFlow.asSharedFlow()

    private val _hasErrorFlow = MutableSharedFlow<Boolean>()
    override val hasErrorFlow = _hasErrorFlow.asSharedFlow()

    override suspend fun callbackHasError(hasError: Boolean) {
        _hasErrorFlow.emit(hasError)
    }

    override suspend fun triggerUpdate(data: NotificationData) {
        _updateTriggerFlow.emit(data)
    }
}