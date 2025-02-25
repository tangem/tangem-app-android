package com.tangem.features.pushnotifications.impl.model

internal interface PushNotificationsClickIntents {
    fun onRequest()

    fun onNeverRequest()

    fun onAllowPermission()

    fun onDenyPermission()
}