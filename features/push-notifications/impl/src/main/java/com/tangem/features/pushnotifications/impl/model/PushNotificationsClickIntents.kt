package com.tangem.features.pushnotifications.impl.model

internal interface PushNotificationsClickIntents {
    fun onAllowClick()

    fun onLaterClick(isFromBs: Boolean)

    fun onAllowPermission()

    fun onDenyPermission()
}