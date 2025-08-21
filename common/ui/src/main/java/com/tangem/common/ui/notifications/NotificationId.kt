package com.tangem.common.ui.notifications

/**
 * NotificationId represents unique identifiers for notifications in the app.
 *
 * These ids can be used with [ShouldShowNotificationUseCase] and [SetShouldShowNotificationUseCase]
 * to check or update the visibility state of notifications.
 */
enum class NotificationId(val key: String) {
    SendViaSwapTokenSelectorNotification("SendViaSwapTokenSelectorNotificationKey"),
}