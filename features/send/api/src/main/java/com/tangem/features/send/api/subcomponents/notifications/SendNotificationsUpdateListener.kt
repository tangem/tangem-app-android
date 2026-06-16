package com.tangem.features.send.api.subcomponents.notifications

import com.tangem.features.send.api.SendNotificationsComponent
import kotlinx.coroutines.flow.Flow

interface SendNotificationsUpdateListener {
    /** Flow triggers notifications update */
    val updateTriggerFlow: Flow<SendNotificationsComponent.Params.NotificationData>

    /** Flow returns whether there is error notifications */
    val hasErrorFlow: Flow<Boolean>
}