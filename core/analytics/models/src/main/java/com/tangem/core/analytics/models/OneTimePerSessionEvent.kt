package com.tangem.core.analytics.models

/**
 * Marker interface for analytics events that should be sent only once per session.
 *
 * Events implementing this interface will be tracked by their [oneTimeEventId] to ensure
 * they are not sent multiple times during the same application session. Once an event
 * with a specific [oneTimeEventId] has been sent, subsequent attempts to send an event
 * with the same ID will be ignored.
 *
 * @see Analytics.send
 */
interface OneTimePerSessionEvent {
    /**
     * Unique identifier for the one-time event.
     *
     * This ID is used to track whether the event has already been sent in the current session.
     * Events with the same [oneTimeEventId] will only be sent once, even if they are
     * different instances of the same event class.
     */
    val oneTimeEventId: String
}