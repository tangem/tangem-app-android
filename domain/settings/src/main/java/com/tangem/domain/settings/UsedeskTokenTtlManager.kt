package com.tangem.domain.settings

import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration.Companion.days

/**
 * Manages the lifetime (TTL) of the Usedesk support-chat token.
 *
 * The token is reused while the time since the last chat interaction is below this TTL;
 * once it expires the SDK obtains a fresh token on the next chat init.
 *
 * In production the TTL is fixed to [DEFAULT_TTL_MILLIS]. Debug builds let testers override it
 * via the Tester Menu (e.g. shorten it to a few minutes to verify token-refresh behaviour).
 */
interface UsedeskTokenTtlManager {

    fun getTokenTtlMillis(): StateFlow<Long>
    fun getTokenTtlMillisSync(): Long
    suspend fun setTokenTtlMillis(millis: Long)

    companion object {
        val DEFAULT_TTL_MILLIS: Long = 7.days.inWholeMilliseconds
    }
}