package com.tangem.features.pushnotifications.api

interface PushNotificationsModelCallbacks {
    fun onAllowSystemPermission()
    fun onDenySystemPermission()
    fun onDismiss()
}

class PushNotificationsModelCallbacksStub(
    val onAllowSystemPermission: () -> Unit = {},
    val onDenySystemPermission: () -> Unit = {},
    val onDismiss: () -> Unit = {},
) : PushNotificationsModelCallbacks {
    override fun onAllowSystemPermission() = onAllowSystemPermission.invoke()
    override fun onDenySystemPermission() = onDenySystemPermission.invoke()
    override fun onDismiss() = onDismiss.invoke()
}