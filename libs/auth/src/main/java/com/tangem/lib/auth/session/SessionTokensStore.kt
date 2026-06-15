package com.tangem.lib.auth.session

import arrow.core.Option

/**
 * Persistent, hardware-backed storage of [SessionTokens].
 *
 * Implementations are expected to survive app process death but not user data wipe / app uninstall.
 */
interface SessionTokensStore {

    /** Returns the currently stored tokens, or [arrow.core.None] if the device is not authenticated. */
    suspend fun get(): Option<SessionTokens>

    /** Atomically replaces the stored tokens. */
    suspend fun save(tokens: SessionTokens)

    /** Removes stored tokens. */
    suspend fun clear()
}