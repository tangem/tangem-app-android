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
     * Atomically replaces the stored tokens — unless [tokens] were superseded while the caller's
     * request was in flight, in which case the stored set is kept.
     *
     * Several endpoints reissue the session (`/refresh`, `/authenticate`, wallet registration and
     * unregistration, the social binding), and their responses can arrive out of order, so a plain
     * last-write-wins would let a superseded set overwrite the current one. Ordering is taken from
     * the `iat` claim of the access token, and — when that cannot separate the two, because the
     * claim is unreadable or names the very same second — from [expected].
     *
     * @param expected the set the caller saw before it sent the request that produced [tokens]. If
     * the store no longer holds it, someone else has written in the meantime and an otherwise
     * indistinguishable [tokens] loses. Pass `null` when there is nothing to compare against; the
     * write then goes through.
     * @return the tokens in effect after the call — [tokens] when they were stored, the surviving
     * set when they were not. Callers that go on to use the session (a retry, say) must use this
     * value rather than what they passed in.
     */
    suspend fun save(tokens: SessionTokens, expected: SessionTokens? = null): SessionTokens

    /** Removes stored tokens. */
    suspend fun clear()
}