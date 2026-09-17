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
     * stored, in which case the stored set is kept.
     *
     * Several endpoints reissue the session (`/refresh`, `/authenticate`, wallet registration and
     * unregistration, the social binding), and their responses can arrive out of order, so a plain
     * last-write-wins would let a superseded set overwrite the current one. Ordering is taken from
     * the `iat` claim of the access token.
     *
     * `iat` is second-granular, so two rotations that complete within one second stay
     * indistinguishable and the later arrival wins — which is wrong whenever it is the older of the
     * two. Nothing on the client can settle that: the response itself is all the ordering
     * information there is. It self-heals, since the superseded set 401s, refreshes and (the refresh
     * token having been rotated away) re-authenticates into a fresh pair. Closing the window for
     * good needs a monotonic marker from the service — a sub-second `iat` or a session version.
     *
     * @return the tokens in effect after the call — [tokens] when they were stored, the surviving
     * set when they were not. Callers that go on to use the session (a retry, say) must use this
     * value rather than what they passed in.
     */
    suspend fun save(tokens: SessionTokens): SessionTokens

    /** Removes stored tokens. */
    suspend fun clear()
}