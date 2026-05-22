package com.tangem.lib.auth.devicekey.internal

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import arrow.core.None
import arrow.core.Option
import com.tangem.crypto.Secp256r1
import com.tangem.lib.auth.devicekey.DeviceKeyManager
import com.tangem.lib.auth.devicekey.DeviceKeySigningException
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.withContext
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Signature
import java.security.spec.ECGenParameterSpec

internal class DefaultDeviceKeyManager(
    private val keyStore: KeyStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : DeviceKeyManager {

    override suspend fun generateIfMissing(): Boolean = withContext(dispatchers.io) {
        if (keyStore.containsAlias(KEY_ALIAS)) return@withContext false

        try {
            generateKey()
            TangemLogger.i("Device key generated")
            true
        } catch (e: Exception) {
            TangemLogger.e("Failed to generate device key", e)
            false
        }
    }

    override suspend fun getPublicKey(): Option<ByteArray> = withContext(dispatchers.io) {
        Option.catch(
            recover = { e ->
                TangemLogger.e("Failed to get device public key", e)
                None
            },
            f = ::getPublicKeyBytes,
        )
    }

    override suspend fun sign(data: ByteArray): ByteArray = withContext(dispatchers.io) {
        try {
            val privateKey = keyStore.getKey(KEY_ALIAS, null)
                ?: throw DeviceKeySigningException("Device key not found")

            val signature = Signature.getInstance(SIGNATURE_ALGORITHM).apply {
                initSign(privateKey as java.security.PrivateKey)
                update(data)
            }

            val derSignature = signature.sign()
            Secp256r1.toByte64(derSignature)
        } catch (e: DeviceKeySigningException) {
            TangemLogger.e("Device key signing failed", e)
            throw e
        } catch (e: Exception) {
            TangemLogger.e("Device key signing failed", e)
            throw DeviceKeySigningException("Signing failed", e)
        }
    }

    private fun generateKey() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                initAndGenerateKeyPair(strongBox = true)
            } catch (e: Exception) {
                TangemLogger.i("StrongBox unavailable, falling back to TEE", e)
                initAndGenerateKeyPair(strongBox = false)
            }
        } else {
            initAndGenerateKeyPair(strongBox = false)
        }
    }

    private fun initAndGenerateKeyPair(strongBox: Boolean) {
        val spec = buildKeyGenSpec(strongBox)
        val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, KEYSTORE_PROVIDER)
        generator.initialize(spec)
        generator.generateKeyPair()
    }

    private fun buildKeyGenSpec(strongBox: Boolean): KeyGenParameterSpec {
        val builder = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY,
        )
            .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setUserAuthenticationRequired(false)

        if (strongBox && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            builder.setIsStrongBoxBacked(true)
        }

        return builder.build()
    }

    private fun getPublicKeyBytes(): ByteArray {
        val cert = checkNotNull(keyStore.getCertificate(KEY_ALIAS)) { "Device key not found" }

        val encoded = cert.publicKey.encoded
        check(encoded.size >= EC_UNCOMPRESSED_POINT_SIZE) {
            "Invalid encoded public key: expected at least $EC_UNCOMPRESSED_POINT_SIZE bytes, got ${encoded.size}"
        }
        val point = encoded.copyOfRange(encoded.size - EC_UNCOMPRESSED_POINT_SIZE, encoded.size)
        check(point[0] == UNCOMPRESSED_POINT_PREFIX) {
            "Invalid EC public key: expected uncompressed point prefix 0x04, got 0x${"%02x".format(point[0])}"
        }
        return point
    }

    private companion object {
        const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val KEY_ALIAS = "tangem_device_key"
        const val SIGNATURE_ALGORITHM = "SHA256withECDSA"
        const val EC_UNCOMPRESSED_POINT_SIZE = 65
        const val UNCOMPRESSED_POINT_PREFIX = 0x04.toByte()
    }
}