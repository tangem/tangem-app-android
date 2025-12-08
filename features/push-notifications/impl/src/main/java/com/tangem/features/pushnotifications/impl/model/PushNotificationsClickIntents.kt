package com.tangem.features.pushnotifications.impl.model

internal interface PushNotificationsClickIntents {
    fun onAllowClick()

    fun onLaterClick()

    fun onAllowPermission()

    fun onDenyPermission()
}