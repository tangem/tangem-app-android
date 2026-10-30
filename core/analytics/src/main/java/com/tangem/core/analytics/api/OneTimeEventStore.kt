package com.tangem.core.analytics.api

/**
 * Remembers analytics events that must be sent once per installation. Survives app restarts; cleared only with the
 * app data.
 */
interface OneTimeEventStore {

    /** Records [eventId] as sent and tells whether it had already been sent before this call. */
    suspend fun markSent(eventId: String): Boolean
}