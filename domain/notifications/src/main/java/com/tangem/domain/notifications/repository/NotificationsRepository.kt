package com.tangem.domain.notifications.repository

/**
 * Repository interface for managing local notification logic and state.
 *
 * This interface provides methods to check and update whether specific notifications should be shown,
 * as well as to track the display count for certain notifications (e.g., Tron token fee).
 *
 * Note: This repository is responsible only for the local logic and state (such as preferences and counters)
 * regarding notifications. It does **not** directly show or hide notifications to the user.
 * The actual display and hiding of notifications in the UI is handled by [NotificationsUM],
 * which uses this repository to determine the appropriate behavior.
 */
interface NotificationsRepository {

    /**
     * Checks whether a notification with the given [key] should be shown to the user.
     * @param key The unique identifier for the notification.
     * @return true if the notification should be shown, false otherwise.
     */
    suspend fun shouldShowNotification(key: String): Boolean

    /**
     * Sets whether a notification with the given [key] should be shown to the user.
     * @param key The unique identifier for the notification.
     * @param value true if the notification should be shown, false otherwise.
     */
    suspend fun setShouldShowNotifications(key: String, value: Boolean)

    /**
     * Gets the number of times the Tron token fee notification has been shown.
     * @return The current show counter for the Tron token fee notification.
     */
    suspend fun getTronTokenFeeNotificationShowCounter(): Int

    /**
     * Increments the counter tracking how many times the Tron token fee notification has been shown.
     */
    suspend fun incrementTronTokenFeeNotificationShowCounter()

    suspend fun shouldShowSubscribeOnNotificationsAfterUpdate(): Boolean

    suspend fun isUserAllowToSubscribeOnPushNotifications(): Boolean

    suspend fun setUserAllowToSubscribeOnPushNotifications(value: Boolean)

    suspend fun getWalletAutomaticallyEnabledList(): List<String>

    suspend fun setNotificationsWasEnabledAutomatically(userWalletId: String)
}