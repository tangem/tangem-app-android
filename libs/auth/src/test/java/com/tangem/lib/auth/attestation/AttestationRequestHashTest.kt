package com.tangem.lib.auth.attestation

import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class AttestationRequestHashTest {

    @BeforeEach
    fun setup() {
        // Delegate android.util.Base64 to java.util.Base64 so the real digest/encoding pipeline runs
        // and the test can assert the exact contract vector shared with the backend.
        mockkStatic(android.util.Base64::class)
        every { android.util.Base64.decode(any<String>(), any()) } answers {
            java.util.Base64.getUrlDecoder().decode(firstArg<String>())
        }
        every { android.util.Base64.encodeToString(any(), any()) } answers {
            java.util.Base64.getEncoder().encodeToString(firstArg())
        }
    }

    @AfterEach
    fun teardown() = unmockkAll()

    @Test
    fun `GIVEN contract test vector WHEN create THEN matches backend-agreed requestHash`() {
        // Arrange — the shared spec test vector (synthetic P-256 SPKI + example nonce).
        val devicePublicKey = hexToBytes(
            "3059301306072a8648ce3d020106082a8648ce3d030107034200" +
                "04" + "22".repeat(64),
        )
        val nonce = "aGVsbG8td29ybGQtdGVzdC1ub25jZQ"

        // Act
        val requestHash = AttestationRequestHash.create(devicePublicKey, nonce)

        // Assert
        assertThat(requestHash).isEqualTo("ppnhIAKZzU9Xy0Wkv9NH/dX1lmKDYHnO2kVWM6YUzsY=")
    }

    private fun hexToBytes(hex: String): ByteArray =
        ByteArray(hex.length / 2) { hex.substring(it * 2, it * 2 + 2).toInt(radix = 16).toByte() }
}