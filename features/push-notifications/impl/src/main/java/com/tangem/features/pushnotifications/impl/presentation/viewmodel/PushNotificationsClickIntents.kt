package com.tangem.features.pushnotifications.impl.presentation.viewmodel

internal interface PushNotificationsClickIntents {
    fun onAskLater()

    fun onAllowPermission()

    fun onAllowedPermission()

    fun openSettings()
}