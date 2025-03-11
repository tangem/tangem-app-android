package com.tangem.features.pushnotifications.impl.presentation.viewmodel

internal interface PushNotificationsClickIntents {
    fun onRequest()

    fun onNeverRequest()

    fun onAllowPermission()

    fun onDenyPermission()
}