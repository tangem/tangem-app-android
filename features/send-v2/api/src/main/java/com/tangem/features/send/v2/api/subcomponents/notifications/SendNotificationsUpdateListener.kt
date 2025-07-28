package com.tangem.features.send.v2.api.subcomponents.notifications

import com.tangem.features.send.v2.api.SendNotificationsComponent.Params.NotificationData
import kotlinx.coroutines.flow.Flow

interface SendNotificationsUpdateListener {
    /** Flow triggers notifications update */
    val updateTriggerFlow: Flow<NotificationData>

    /** Flow returns whether there is error notifications */
    val hasErrorFlow: Flow<Boolean>
}