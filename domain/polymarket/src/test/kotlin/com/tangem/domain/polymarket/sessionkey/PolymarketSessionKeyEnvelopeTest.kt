package com.tangem.domain.polymarket.sessionkey

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal class PolymarketSessionKeyEnvelopeTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Wrap {

        @Test
        fun `GIVEN a 65-byte signature WHEN wrap THEN names the key, pads the signature and ends with the suffix`() {
            // Arrange
            val signature = "0x" + "ab".repeat(n = 65)

            // Act
            val actual = PolymarketSessionKeyEnvelope.wrap(
                depositWalletSignature = signature,
                sessionKeyAddress = SESSION_KEY,
            )

            // Assert
            assertThat(actual).isEqualTo(AB_ENVELOPE.right())
        }

        @Test
        fun `GIVEN a word-aligned signature WHEN wrap THEN adds no padding`() {
            // Arrange
            val signature = "0x" + "cd".repeat(n = 64)

            // Act
            val actual = PolymarketSessionKeyEnvelope.wrap(
                depositWalletSignature = signature,
                sessionKeyAddress = SESSION_KEY,
            )

            // Assert
            assertThat(actual).isEqualTo(CD_ENVELOPE.right())
            assertThat(actual.getOrNull()?.length).isEqualTo(2 + 2 * (4 * 32 + 64 + 32))
        }

        @Test
        fun `GIVEN a malformed session key WHEN wrap THEN refuses it`() {
            // Act
            val actual = PolymarketSessionKeyEnvelope.wrap(depositWalletSignature = "0xab", sessionKeyAddress = "0x12")

            // Assert
            assertThat(actual).isEqualTo(PolymarketSessionKeyEnvelope.Error.InvalidAddress(address = "0x12").left())
        }

        @ParameterizedTest
        @ValueSource(strings = ["0xabc", "0x", "0xzz"])
        fun `GIVEN a malformed signature WHEN wrap THEN refuses it`(signature: String) {
            // Act
            val actual = PolymarketSessionKeyEnvelope.wrap(
                depositWalletSignature = signature,
                sessionKeyAddress = SESSION_KEY,
            )

            // Assert
            assertThat(actual).isEqualTo(PolymarketSessionKeyEnvelope.Error.InvalidSignature.left())
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Unwrap {

        @Test
        fun `GIVEN the envelope of a padded signature WHEN unwrap THEN returns the key and the signature`() {
            // Act
            val actual = PolymarketSessionKeyEnvelope.unwrap(AB_ENVELOPE)

            // Assert
            assertThat(actual).isEqualTo(
                PolymarketSessionKeyEnvelope.Unwrapped(
                    sessionKeyAddress = SESSION_KEY.lowercase(),
                    depositWalletSignature = "0x" + "ab".repeat(n = 65),
                ),
            )
        }

        @Test
        fun `GIVEN the envelope of a word-aligned signature WHEN unwrap THEN returns the key and the signature`() {
            // Act
            val actual = PolymarketSessionKeyEnvelope.unwrap(CD_ENVELOPE)

            // Assert
            assertThat(actual).isEqualTo(
                PolymarketSessionKeyEnvelope.Unwrapped(
                    sessionKeyAddress = SESSION_KEY.lowercase(),
                    depositWalletSignature = "0x" + "cd".repeat(n = 64),
                ),
            )
        }

        @ParameterizedTest
        @ValueSource(
            strings = [
                "0x" + "ab",
                AB_PAYLOAD,
                AB_PAYLOAD + "0000" + MAGIC,
                "0x" + "11" + AB_HEAD_AND_BODY_WITHOUT_FIRST_BYTE + MAGIC,
                "0x" + AB_HEAD + "ab" + MAGIC,
            ],
        )
        fun `GIVEN a signature that is no well-formed envelope WHEN unwrap THEN returns null`(signature: String) {
            // Act
            val actual = PolymarketSessionKeyEnvelope.unwrap(signature)

            // Assert
            assertThat(actual).isNull()
        }
    }

    private companion object {
        const val SESSION_KEY = "0x5E55000000000000000000000000000000000001"
        const val MAGIC = "6492649264926492649264926492649264926492649264926492649264926492"

        const val SIGNER_WORD = "0000000000000000000000005e55000000000000000000000000000000000001"
        const val ZERO_WORD = "0000000000000000000000000000000000000000000000000000000000000000"
        const val OFFSET_WORD = "0000000000000000000000000000000000000000000000000000000000000060"
        const val AB_LENGTH_WORD = "0000000000000000000000000000000000000000000000000000000000000041"
        const val CD_LENGTH_WORD = "0000000000000000000000000000000000000000000000000000000000000040"

        const val AB_HEAD = SIGNER_WORD + ZERO_WORD + OFFSET_WORD + AB_LENGTH_WORD
        const val AB_BODY = "abababababababababababababababababababababababababababababababab" +
            "abababababababababababababababababababababababababababababababab" +
            "ab00000000000000000000000000000000000000000000000000000000000000"
        const val AB_PAYLOAD = "0x$AB_HEAD$AB_BODY"
        const val AB_ENVELOPE = "$AB_PAYLOAD$MAGIC"
        const val AB_HEAD_AND_BODY_WITHOUT_FIRST_BYTE =
            "00000000000000000000005e55000000000000000000000000000000000001" +
                ZERO_WORD + OFFSET_WORD + AB_LENGTH_WORD + AB_BODY

        const val CD_BODY = "cdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcd" +
            "cdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcd"
        const val CD_ENVELOPE = "0x$SIGNER_WORD$ZERO_WORD$OFFSET_WORD$CD_LENGTH_WORD$CD_BODY$MAGIC"
    }
}