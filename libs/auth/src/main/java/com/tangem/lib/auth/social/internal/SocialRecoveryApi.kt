package com.tangem.lib.auth.social.internal

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.core.remote.response.ApiResponse
import com.tangem.lib.auth.api.RequiresSessionAuth
import com.tangem.lib.auth.api.models.request.DeviceMetadata
import com.tangem.lib.auth.api.models.request.NonceApiRequest
import com.tangem.lib.auth.api.models.response.NonceApiResponse
import com.tangem.lib.auth.api.models.response.TokenApiResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Social login endpoints of the Tangem Auth Service. Split from `AuthApi` only so the not-yet-shipped
 * half can be served by a stand-in implementation without touching the live authentication path.
 */
internal interface SocialRecoveryApi {

    /**
     * The nonce plaintext serves twice: as the `nonce` claim the provider id token is minted with, and
     * as [WalletRecoverApiRequest.nonce], which the backend consumes when it resolves the binding.
     */
    @POST("api/authentication/v1/mobile/nonce/recover")
    suspend fun requestRecoverNonce(@Body request: NonceApiRequest): ApiResponse<NonceApiResponse>

    /** `404` means the identity has no wallet yet */
    @POST("api/authentication/v1/mobile/wallet/recover")
    @RequiresSessionAuth
    suspend fun recoverWallet(@Body request: WalletRecoverApiRequest): ApiResponse<WalletRecoverApiResponse>

    /** The KMS public key the recovery key is sealed to before it is stored */
    @GET("api/authentication/v1/mobile/wallet/recovery-public-key")
    @RequiresSessionAuth
    suspend fun getRecoveryPublicKey(): ApiResponse<RecoveryPublicKeyApiResponse>

    /**
     * Same endpoint a hot wallet already registers through at creation time — repeating it with the
     * social fields binds a wallet that is registered but not yet bound, which is the order the create
     * flow runs in.
     */
    @POST("api/authentication/v1/mobile/wallet/register")
    @RequiresSessionAuth
    suspend fun registerWalletBinding(@Body request: SocialWalletRegistrationApiRequest): ApiResponse<TokenApiResponse>
}

/**
 * @property recoveryEncryptionPublicKey base64 X25519 public key (32 bytes) of the ephemeral pair
 * @property deviceSignature base64 DER ECDSA-SHA256 over the canonical payload, made with the device key
 * @property appAttestKeyId iOS App Attest key id; always absent on Android, but part of the signed
 * payload as an empty segment so both platforms canonicalize identically
 */
@JsonClass(generateAdapter = true)
internal data class WalletRecoverApiRequest(
    @Json(name = "nonce") val nonce: String,
    @Json(name = "recoveryEncryptionPublicKey") val recoveryEncryptionPublicKey: String,
    @Json(name = "deviceSignature") val deviceSignature: String,
    @Json(name = "providerIdToken") val providerIdToken: String,
    @Json(name = "attestationToken") val attestationToken: String?,
    @Json(name = "appAttestKeyId") val appAttestKeyId: String?,
    // `metadata`, not the `deviceMetadata` the OpenAPI schema shows — the deployed endpoint and the
    // iOS client both use the former, and the schema's name makes the request unreadable
    @Json(name = "metadata") val metadata: DeviceMetadata,
)

/**
 * @property enc base64 encapsulated HPKE key, 32 bytes
 * @property ct  base64 ciphertext with the GCM tag, 48 bytes for a 32-byte recovery key
 */
@JsonClass(generateAdapter = true)
internal data class WalletRecoverApiResponse(
    @Json(name = "enc") val enc: String,
    @Json(name = "ct") val ct: String,
)

/**
 * @property modulus base64url, unsigned big-endian
 * @property exponent base64url, unsigned big-endian
 * @property algorithm JWA algorithm the key is issued for; nullable only because the field can be absent
 * from the response — the client honours `RSA-OAEP-256` and refuses everything else, `null` included
 */
@JsonClass(generateAdapter = true)
internal data class RecoveryPublicKeyApiResponse(
    @Json(name = "n") val modulus: String,
    @Json(name = "e") val exponent: String,
    @Json(name = "kty") val keyType: String? = null,
    @Json(name = "alg") val algorithm: String? = null,
    @Json(name = "kid") val keyId: String? = null,
    @Json(name = "use") val use: String? = null,
)

/**
 * `WalletRegistrationRequest` plus the social binding fields, kept apart so the plain registration body
 * stays free of optional fields it never sends.
 *
 * @property encryptedRecoveryKey base64 RSA-OAEP-256 ciphertext, sealed to the KMS public key
 */
@Suppress("LongParameterList")
@JsonClass(generateAdapter = true)
internal data class SocialWalletRegistrationApiRequest(
    @Json(name = "nonce") val nonce: String,
    @Json(name = "walletId") val walletId: String,
    @Json(name = "walletSignature") val walletSignature: String,
    @Json(name = "walletSignatureSalt") val walletSignatureSalt: String,
    @Json(name = "providerIdToken") val providerIdToken: String,
    @Json(name = "encryptedRecoveryKey") val encryptedRecoveryKey: String,
    @Json(name = "attestationToken") val attestationToken: String?,
    @Json(name = "metadata") val metadata: DeviceMetadata,
)