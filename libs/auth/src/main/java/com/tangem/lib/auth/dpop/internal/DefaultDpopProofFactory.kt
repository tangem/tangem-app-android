package com.tangem.lib.auth.dpop.internal

import android.util.Base64
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import com.tangem.lib.auth.devicekey.DeviceKeyManager
import com.tangem.lib.auth.dpop.DpopProofFactory
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.security.MessageDigest
import java.util.UUID

internal class DefaultDpopProofFactory(
    private val deviceKeyManager: DeviceKeyManager,
    private val json: Json,
    private val clock: Clock,
    private val dispatchers: CoroutineDispatcherProvider,
) : DpopProofFactory {

    override suspend fun create(httpMethod: String, httpUri: String, accessToken: String?): Option<String> =
        withContext(dispatchers.default) {
            val publicKey = deviceKeyManager.getPublicKey().getOrNull()

            if (publicKey == null) {
                TangemLogger.e("DPoP proof skipped: device key unavailable")
                return@withContext None
            }

            // DeviceKeyManager.getPublicKey() guarantees an uncompressed P-256 point
            // (0x04 || X(32) || Y(32)) — see DefaultDeviceKeyManager.getPublicKeyBytes.
            val x = publicKey.copyOfRange(fromIndex = 1, toIndex = 1 + COORDINATE_SIZE)
            val y = publicKey.copyOfRange(fromIndex = 1 + COORDINATE_SIZE, toIndex = 1 + 2 * COORDINATE_SIZE)

            val header: JsonObject = buildJsonObject {
                put("alg", ES256_ALG)
                put("typ", DPOP_TYP)
                putJsonObject("jwk") {
                    put("kty", EC_KTY)
                    put("crv", P256_CRV)
                    put("x", x.base64UrlNoPad())
                    put("y", y.base64UrlNoPad())
                }
            }

            val claims: JsonObject = buildJsonObject {
                put("jti", UUID.randomUUID().toString())
                put("iat", clock.now().epochSeconds)
                put("htm", httpMethod.uppercase())
                put("htu", stripQueryAndFragment(httpUri))
                if (accessToken != null) {
                    put("ath", sha256(accessToken.toByteArray(Charsets.US_ASCII)).base64UrlNoPad())
                }
            }

            val signingInput = json.encodeToString(JsonObject.serializer(), header)
                .toByteArray(Charsets.UTF_8).base64UrlNoPad() +
                "." +
                json.encodeToString(JsonObject.serializer(), claims)
                    .toByteArray(Charsets.UTF_8).base64UrlNoPad()

            val signature = try {
                deviceKeyManager.sign(signingInput.toByteArray(Charsets.US_ASCII))
            } catch (e: Exception) {
                TangemLogger.e("Failed to sign DPoP proof", e)
                return@withContext None
            }

            Some("$signingInput.${signature.base64UrlNoPad()}")
        }

    private fun sha256(bytes: ByteArray): ByteArray {
        return MessageDigest.getInstance(SHA_256).digest(bytes)
    }

    private fun ByteArray.base64UrlNoPad(): String =
        Base64.encodeToString(this, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)

    /**
     * Removes query and fragment without touching scheme/authority/path encoding.
     * `java.net.URI.path` would decode percent-encoded bytes (e.g. `%2F` → `/`), which would
     * make the `htu` claim diverge from the wire URI and fail DPoP verification.
     */
    private fun stripQueryAndFragment(uri: String): String = uri.substringBefore('#').substringBefore('?')

    private companion object {
        const val ES256_ALG = "ES256"
        const val DPOP_TYP = "dpop+jwt"
        const val EC_KTY = "EC"
        const val P256_CRV = "P-256"
        const val SHA_256 = "SHA-256"
        const val COORDINATE_SIZE = 32
    }
}