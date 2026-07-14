package com.tangem.features.send.api.subcomponents.notifications

interface SendNotificationsUpdateTrigger {
    /** Trigger return callback with check result */
    suspend fun callbackHasError(hasError: Boolean)

    /** Trigger fee check reload */
    suspend fun triggerUpdate(data: SendNotificationsComponent.Params.NotificationData)
}