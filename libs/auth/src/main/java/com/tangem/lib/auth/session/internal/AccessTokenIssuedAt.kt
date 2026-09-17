package com.tangem.lib.auth.session.internal

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.tangem.utils.logging.TangemLogger
import kotlinx.datetime.Instant
import java.util.Base64

/**
 * Reads the `iat` claim out of an access token, i.e. the instant the service issued it.
 *
 * Used to order two token sets against each other. The claim is read rather than
 * `accessTokenExpiresAt` because the latter only tracks issuance while the lifetime stays constant —
 * a shorter-lived token issued later would look older.
 *
 * The payload is read without verifying the signature, which is fine for the one thing it is used
 * for: an attacker able to forge this claim can already rewrite the whole stored token set.
 */
internal class AccessTokenIssuedAt(moshi: Moshi) {

    private val adapter by lazy { moshi.adapter(Claims::class.java) }

    /** `null` when the token is not a JWT or carries no `iat` — the caller then cannot order it */
    fun of(accessToken: String): Instant? {
        val payload = accessToken.split(".").getOrNull(index = 1) ?: return null

        return try {
            val claims = adapter.fromJson(String(Base64.getUrlDecoder().decode(payload)))
            claims?.issuedAt?.let(Instant::fromEpochSeconds)
        } catch (e: Exception) {
            TangemLogger.i("Access token carries no readable iat claim: ${e::class.simpleName}")
            null
        }
    }

    @JsonClass(generateAdapter = true)
    internal data class Claims(@Json(name = "iat") val issuedAt: Long? = null)
}