package com.tangem.core.analytics.models

/**
 * Marker interface for analytics events that can be throttled by session and/or time.
 *
 * Events implementing this interface will be tracked by their [oneTimeEventId].
 *
 * Behavior depends on [throttleSeconds]:
 * - **null** (default): One time per session — event is sent only once during the application session.
 * - **non-null**: Time-based throttling — event is not sent if less than [throttleSeconds] seconds
 *   have passed since the last send for this [oneTimeEventId].
 *
 * @see Analytics.send
 */
interface OneTimePerSessionEvent {
    /**
     * Unique identifier for the throttled event.
     *
     * This ID is used to track whether and when the event was last sent.
     * Events with the same [oneTimeEventId] share the same throttling state.
     */
    val oneTimeEventId: String

    /**
     * Minimum interval in seconds between sends for this event.
     *
     * - **null**: One time per session only. Event is sent at most once per session.
     * - **non-null**: Don't send if less than this many seconds have passed since the last send.
     */
    val throttleSeconds: Long?
        get() = null
}