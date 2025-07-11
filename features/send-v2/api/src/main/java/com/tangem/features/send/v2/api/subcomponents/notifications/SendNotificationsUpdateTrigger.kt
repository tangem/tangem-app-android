package com.tangem.features.send.v2.api.subcomponents.notifications

import com.tangem.features.send.v2.api.SendNotificationsComponent.Params.NotificationData

interface SendNotificationsUpdateTrigger {
    /** Trigger return callback with check result */
    suspend fun callbackHasError(hasError: Boolean)

    /** Trigger fee check reload */
    suspend fun triggerUpdate(data: NotificationData)
}