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
        // Arrange — the shared spec test vector (example base64url nonce).
        val nonce = "aGVsbG8td29ybGQtdGVzdC1ub25jZQ"

        // Act
        val requestHash = AttestationRequestHash.create(nonce)

        // Assert
        assertThat(requestHash).isEqualTo("0Ek1sCc/KyGP2rJE6Q7UkQE/OfzN98QSCk6wOgx5kGs=")
    }
}