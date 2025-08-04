package com.tangem.features.send.v2.subcomponents.notifications

import com.tangem.features.send.v2.api.SendNotificationsComponent.Params.NotificationData
import com.tangem.features.send.v2.api.subcomponents.notifications.SendNotificationsUpdateListener
import com.tangem.features.send.v2.api.subcomponents.notifications.SendNotificationsUpdateTrigger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultNotificationsUpdateTrigger @Inject constructor() :
    SendNotificationsUpdateListener,
    SendNotificationsUpdateTrigger {

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