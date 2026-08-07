package com.tangem.lib.auth.devicekey

import arrow.core.Option

/**
 * Manages a device-bound secp256r1 keypair in Android Keystore (TEE/StrongBox).
 * The private key never leaves the secure hardware.
 */
interface DeviceKeyManager {

    /**
     * Ensures the device keypair exists. Generates one if missing.
     * Never throws — generation failures are logged and reported via the return value.
     * @return `true` if a new keypair was generated, `false` if it already existed or generation failed
     */
    suspend fun generateIfMissing(): Boolean

    /**
     * X.509 `SubjectPublicKeyInfo` (DER) encoding of the device public key — the form the auth
     * service parses via `X509EncodedKeySpec`. Use this for the `devicePublicKey` field of auth
     * requests (nonce / register / authenticate) and for signed-payload canonicalisation.
     * @return the SPKI-encoded key, or [arrow.core.None] if it cannot be read.
     */
    suspend fun getPublicKeyEncoded(): Option<ByteArray>

    /**
     * Raw uncompressed EC point (0x04 || x || y, 65 bytes) of the device public key. Use this to
     * build the DPoP proof JWK, where the `x` / `y` coordinates are sliced out directly.
     * @return the raw point, or [arrow.core.None] if it cannot be read.
     */
    suspend fun getPublicKeyRawPoint(): Option<ByteArray>

    /**
     * Signs [data] with SHA256withECDSA and returns the signature as raw 64-byte `r || s`
     * (each component zero-padded to 32 bytes) — the form JOSE/JWS (ES256) expects. Use this for
     * the DPoP proof.
     * @throws DeviceKeySigningException if signing fails
     */
    suspend fun sign(data: ByteArray): ByteArray

    /**
     * Signs [data] with SHA256withECDSA and returns the signature in ASN.1 **DER** form
     * (`SEQUENCE { INTEGER r, INTEGER s }`) — the form `java.security.Signature.verify` expects.
     * Use this for the `/register` and `/authenticate` payload signature the auth service verifies.
     * @throws DeviceKeySigningException if signing fails
     */
    suspend fun signDer(data: ByteArray): ByteArray
}