package com.tangem.lib.auth.session

import arrow.core.Either

/**
 * Refreshes (rotates) session tokens.
 *
 * Implementations must serialise refresh attempts so concurrent callers share a single
 * network round-trip — replaying a consumed refresh token causes the backend to revoke
 * the entire session chain (SR-8 / RFC 9449 §5).
 *
 * Refresh strategy:
 *  1. Refresh with the stored refresh token when it is present and unexpired.
 *  2. On 401 (revoked / replayed / expired refresh token), fall back to full re-authentication
 *     (request an auth nonce and authenticate) signed by the device key.
 *  3. On 403 (RED tier — device blocked server-side), return [SessionRefreshError.DeviceBlocked]
 *     without trying to re-authenticate (it would also 403).
 *  4. On 401/403 during re-authentication, clear the session store and return
 *     [SessionRefreshError.SessionRevoked] — the device must be re-registered.
 */
interface SessionTokenRefresher {

    suspend fun refresh(): Either<SessionRefreshError, SessionTokens>
}