package com.tangem.lib.auth.social

import com.google.common.truth.Truth.assertThat
import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters
import org.junit.jupiter.api.Test
import java.security.SecureRandom
import java.util.Base64

internal class HpkeRecoveryCipherTest {

    /**
     * The round-trip tests below would stay green if [HpkeRecoveryCipher.INFO], the AAD or the algorithm
     * suite drifted, since both halves would drift together — and the other two implementations of this
     * envelope would silently stop interoperating. This envelope is frozen: it only opens while those
     * parameters stay exactly what iOS and the backend were written against.
     */
    @Test
    fun `GIVEN a frozen envelope WHEN opened THEN the suite parameters still match`() {
        // Arrange
        val recipient = keyPairOf(FROZEN_PRIVATE_KEY)
        val envelope = HpkeEnvelope(enc = decode(FROZEN_ENC), ct = decode(FROZEN_CT))

        // Act
        val opened = HpkeRecoveryCipher.open(recipient, envelope)

        // Assert
        assertThat(opened).isEqualTo(decode(FROZEN_PLAINTEXT))
    }

    @Test
    fun `GIVEN a key sealed for a recipient WHEN opened with its private half THEN the key is recovered`() {
        // Arrange
        val recipient = HpkeRecoveryCipher.generateKeyPair()
        val recoveryKey = randomRecoveryKey()

        // Act
        val envelope = HpkeRecoveryCipher.seal(
            recipientPublicKey = HpkeRecoveryCipher.serializePublicKey(recipient),
            plaintext = recoveryKey,
        )
        val opened = HpkeRecoveryCipher.open(recipient, envelope)

        // Assert
        assertThat(opened).isEqualTo(recoveryKey)
    }

    @Test
    fun `GIVEN a sealed key WHEN opened with another key pair THEN it fails`() {
        // Arrange
        val recipient = HpkeRecoveryCipher.generateKeyPair()
        val envelope = HpkeRecoveryCipher.seal(
            recipientPublicKey = HpkeRecoveryCipher.serializePublicKey(recipient),
            plaintext = randomRecoveryKey(),
        )

        // Act
        val error = runCatching { HpkeRecoveryCipher.open(HpkeRecoveryCipher.generateKeyPair(), envelope) }
            .exceptionOrNull()

        // Assert
        assertThat(error).isNotNull()
    }

    @Test
    fun `GIVEN a 32-byte key WHEN sealed THEN the envelope has the sizes the backend contract states`() {
        // Arrange
        val recipient = HpkeRecoveryCipher.generateKeyPair()

        // Act
        val envelope = HpkeRecoveryCipher.seal(
            recipientPublicKey = HpkeRecoveryCipher.serializePublicKey(recipient),
            plaintext = randomRecoveryKey(),
        )

        // Assert — enc is the X25519 encapsulated key, ct is the key plus the 16-byte GCM tag
        assertThat(envelope.enc).hasLength(HpkeRecoveryCipher.PUBLIC_KEY_SIZE_BYTES)
        assertThat(envelope.ct).hasLength(RECOVERY_KEY_SIZE_BYTES + GCM_TAG_SIZE_BYTES)
    }

    @Test
    fun `GIVEN the same key sealed twice WHEN compared THEN both halves differ`() {
        // Arrange
        val recipientPublicKey = HpkeRecoveryCipher.serializePublicKey(HpkeRecoveryCipher.generateKeyPair())
        val recoveryKey = randomRecoveryKey()

        // Act — a fresh ephemeral sender key per call, so nothing is reused across attempts
        val first = HpkeRecoveryCipher.seal(recipientPublicKey, recoveryKey)
        val second = HpkeRecoveryCipher.seal(recipientPublicKey, recoveryKey)

        // Assert
        assertThat(first.enc).isNotEqualTo(second.enc)
        assertThat(first.ct).isNotEqualTo(second.ct)
    }

    private fun randomRecoveryKey(): ByteArray =
        ByteArray(RECOVERY_KEY_SIZE_BYTES).also(SecureRandom()::nextBytes)

    private fun keyPairOf(privateKeyBase64: String): AsymmetricCipherKeyPair {
        val privateKey = X25519PrivateKeyParameters(decode(privateKeyBase64), 0)
        return AsymmetricCipherKeyPair(privateKey.generatePublicKey(), privateKey)
    }

    private fun decode(value: String): ByteArray = Base64.getDecoder().decode(value)

    private companion object {
        const val RECOVERY_KEY_SIZE_BYTES = 32
        const val GCM_TAG_SIZE_BYTES = 16
        const val FROZEN_PRIVATE_KEY = "AQIDBAUGBwgJCgsMDQ4PEBESExQVFhcYGRobHB0eHyA="
        const val FROZEN_ENC = "X5oHnIPTGUj2Nm/CPeLzySOkaswN7qg8CkHmxFy5jWM="
        const val FROZEN_CT = "L0JfBOACsZq6nv5bOQdEBaLVpG0Rd8m8r5YAdyNZvt9mtdV2DC1XUKBFspIRypYZ"
        const val FROZEN_PLAINTEXT = "BwoNEBMWGRwfIiUoKy4xNDc6PUBDRklMT1JVWFteYWQ="
    }
}