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
        // Honor the flags so the vector breaks if production switches the digest encoding (e.g. to
        // URL_SAFE or NO_PADDING) — the backend contract requires standard padded Base64.
        every { android.util.Base64.encodeToString(any(), any()) } answers {
            val flags = secondArg<Int>()
            val encoder = if (flags and android.util.Base64.URL_SAFE != 0) {
                java.util.Base64.getUrlEncoder()
            } else {
                java.util.Base64.getEncoder()
            }
            val configured = if (flags and android.util.Base64.NO_PADDING != 0) encoder.withoutPadding() else encoder
            configured.encodeToString(firstArg())
        }
    }

    @AfterEach
    fun teardown() = unmockkAll()

    @Test
    fun `GIVEN contract test vector WHEN create THEN matches backend-agreed requestHash`() {
        // Arrange — the nonce string is hashed as-is (backend does sha256Base64(nonce) on the string).
        val nonce = "aGVsbG8td29ybGQtdGVzdC1ub25jZQ"

        // Act
        val requestHash = AttestationRequestHash.create(nonce)

        // Assert
        assertThat(requestHash).isEqualTo("2B5Ffvj+P3qV39OpVB3i9OQCYkAlArs4Tna6nwE69LE=")
    }
}