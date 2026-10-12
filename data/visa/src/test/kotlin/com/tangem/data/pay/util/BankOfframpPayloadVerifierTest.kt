package com.tangem.data.pay.util

import com.google.common.truth.Truth.assertThat
import com.tangem.common.extensions.toHexString
import org.junit.jupiter.api.Test
import org.web3j.crypto.StructuredDataEncoder

/**
 * The digest is produced by the blockchain SDK, so what this suite proves is that the app agrees with an
 * independent EIP-712 implementation (web3j) about what a payload hashes to — otherwise the check would either
 * wave through every order or refuse every one of them.
 */
internal class BankOfframpPayloadVerifierTest {

    @Test
    fun `GIVEN typed data WHEN verified THEN digest matches the independent web3j encoding`() {
        // Arrange
        val hash = StructuredDataEncoder(STRUCTURED_DATA).hashStructuredData().toHexString()

        // Act
        val actual = BankOfframpPayloadVerifier.isConsistent(hash = "0x$hash", structuredData = STRUCTURED_DATA)

        // Assert
        assertThat(actual).isTrue()
    }

    @Test
    fun `GIVEN a hash without the hex prefix WHEN verified THEN consistent`() {
        // Arrange
        val hash = StructuredDataEncoder(STRUCTURED_DATA).hashStructuredData().toHexString()

        // Act
        val actual = BankOfframpPayloadVerifier.isConsistent(hash = hash, structuredData = STRUCTURED_DATA)

        // Assert
        assertThat(actual).isTrue()
    }

    @Test
    fun `GIVEN uppercase hex prefix WHEN verified THEN consistent`() {
        // Arrange
        val hash = StructuredDataEncoder(STRUCTURED_DATA).hashStructuredData().toHexString()

        // Act
        val actual = BankOfframpPayloadVerifier.isConsistent(hash = "0X$hash", structuredData = STRUCTURED_DATA)

        // Assert
        assertThat(actual).isTrue()
    }

    @Test
    fun `GIVEN a hash of another payload WHEN verified THEN inconsistent`() {
        // Arrange
        val otherData = STRUCTURED_DATA.replace(oldValue = "10000000", newValue = "20000000")
        val hash = StructuredDataEncoder(otherData).hashStructuredData().toHexString()

        // Act
        val actual = BankOfframpPayloadVerifier.isConsistent(hash = "0x$hash", structuredData = STRUCTURED_DATA)

        // Assert
        assertThat(actual).isFalse()
    }

    @Test
    fun `GIVEN malformed typed data WHEN verified THEN inconsistent`() {
        // Act
        val actual = BankOfframpPayloadVerifier.isConsistent(hash = "0xdeadbeef", structuredData = "{not json")

        // Assert
        assertThat(actual).isFalse()
    }

    private companion object {
        val STRUCTURED_DATA = """
            {
              "types": {
                "EIP712Domain": [
                  {"name": "name", "type": "string"},
                  {"name": "version", "type": "string"},
                  {"name": "chainId", "type": "uint256"},
                  {"name": "verifyingContract", "type": "address"}
                ],
                "Withdraw": [
                  {"name": "sender", "type": "address"},
                  {"name": "recipient", "type": "address"},
                  {"name": "amount", "type": "uint256"},
                  {"name": "salt", "type": "bytes32"}
                ]
              },
              "primaryType": "Withdraw",
              "domain": {
                "name": "Rain",
                "version": "1",
                "chainId": 137,
                "verifyingContract": "0x3c499c542cEF5E3811e1192ce70d8cC03d5c3359"
              },
              "message": {
                "sender": "0x059dbd5f24a000d28f83268db1c85c9c3ab6a123",
                "recipient": "0xfde4C96c8593536E31F229EA8f37b2ADa2699bb2",
                "amount": "10000000",
                "salt": "0x0000000000000000000000000000000000000000000000000000000000000001"
              }
            }
        """.trimIndent()
    }
}