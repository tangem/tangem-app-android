package com.tangem.tap.domain.tasks.jointaccount

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * Pins the parts of joint account signing that a cold card and a hot wallet must produce identically.
 *
 * The digest is checked against the cross-platform vector shared with iOS and the backend — the same value
 * `JointAccountCrossPlatformVectorTest` in `domain:joint-account` derives from the same canonical payload.
 * A digest that differs by a single byte (the EIP-191 `0x19` prefix is the easiest one to lose) makes the
 * backend recover a different address and reject every signature this build produces.
 */
internal class JointAccountSigningTest {

    @Test
    fun `GIVEN the cross-platform vector payload WHEN digest THEN matches the shared EIP-191 value`() {
        // Act
        val actual = JointAccountSigning.eip191Digest(canonicalPayload = VECTOR_CANONICAL_PAYLOAD.toByteArray())

        // Assert
        assertThat(actual.toHex()).isEqualTo(VECTOR_EIP191_DIGEST)
    }

    @Test
    fun `GIVEN payloads of equal content but unequal length WHEN digest THEN digests differ`() {
        // Act
        val short = JointAccountSigning.eip191Digest(canonicalPayload = """{"a":1}""".toByteArray())
        val long = JointAccountSigning.eip191Digest(canonicalPayload = """{"a":11}""".toByteArray())

        // Assert
        // The prefix carries the payload length, so a longer payload can never produce the same digest
        assertThat(short.toHex()).isNotEqualTo(long.toHex())
        assertThat(short).hasLength(DIGEST_SIZE)
        assertThat(long).hasLength(DIGEST_SIZE)
    }

    private fun ByteArray.toHex(): String = joinToString(separator = "") { "%02x".format(it) }

    private companion object {

        const val DIGEST_SIZE = 32

        val VECTOR_CANONICAL_PAYLOAD = """{"config":{"icon":"Family","iconColor":"Azure",""" +
            """"membersCount":3,"name":"Family","threshold":2},""" +
            """"creator":{"address":"0xE31C6A9eE83A0f6f2e44dDcb9837B162802DeC12","derivation":0,""" +
            """"name":"Alice","walletId":"4B2F1C8A9E7D6053A1B4C7E2F8D9A0B3C5E7F1A2D4B6C8E0F2A4B6C8D0E2F4A6"}}"""

        const val VECTOR_EIP191_DIGEST = "e6d9ac30fb18e01faba90cda8249f91ca17e427be57390f8f50964731eb753c2"
    }
}