package com.tangem.features.send.impl.presentation.domain

sealed class SendNotification {

    sealed class Info(val message: String) : SendNotification()

    sealed class Critical(val message: String) : SendNotification()

    sealed class Error(val message: String) : SendNotification()
}