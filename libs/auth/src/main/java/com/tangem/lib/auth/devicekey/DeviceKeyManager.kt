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

    /** Raw uncompressed public key (0x04 || x || y), or [arrow.core.None] if it cannot be read. */
    suspend fun getPublicKey(): Option<ByteArray>

    /**
     * Signs [data] with SHA256withECDSA using the device private key.
     * @return raw 64-byte signature (r || s), each component zero-padded to 32 bytes
     * @throws DeviceKeySigningException if signing fails
     */
    suspend fun sign(data: ByteArray): ByteArray
}