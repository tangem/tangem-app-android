package com.tangem.domain.notifications.models

sealed class NotificationsError {

    data class DataError(val message: String) : NotificationsError()

    data object ApplicationIdNotFound : NotificationsError()
}