package com.tangem.features.pushnotifications.impl.presentation.viewmodel

internal interface PushNotificationsClickIntents {
    fun onRequest()

    fun onRequestLater()

    fun onAllowPermission()

    fun onDenyPermission()

    fun openSettings()
}