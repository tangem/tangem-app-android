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

    /**
     * Atomically replaces the stored tokens — unless [tokens] were issued before the ones already
     * stored, in which case the call is a no-op.
     *
     * Several endpoints reissue the session (`/refresh`, `/authenticate`, wallet registration and
     * unregistration, the social binding), and their responses can arrive out of order, so a plain
     * last-write-wins would let a superseded set overwrite the current one. Ordering is taken from
     * the `iat` claim of the access token; sets that cannot be ordered (no readable claim on either
     * side, or the same instant on both) are stored as they come.
     */
    suspend fun save(tokens: SessionTokens)

    /** Removes stored tokens. */
    suspend fun clear()
}