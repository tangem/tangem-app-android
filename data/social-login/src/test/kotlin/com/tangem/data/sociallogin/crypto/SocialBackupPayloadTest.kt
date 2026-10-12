package com.tangem.data.sociallogin.crypto

import com.google.common.truth.Truth.assertThat
import com.tangem.data.sociallogin.SocialBackupJson
import org.junit.jupiter.api.Test

internal class SocialBackupPayloadTest {

    @Test
    fun `GIVEN a mnemonic WHEN encoded THEN it matches the cross-platform contract byte for byte`() {
        // Arrange & Act
        val encoded = SocialBackupPayload.encode(MNEMONIC.toCharArray(), isPassphraseRequired = false)

        // Assert
        assertThat(encoded?.decodeToString()).isEqualTo("""{"mnemonic":"$MNEMONIC","passphraseRequired":0}""")
    }

    @Test
    fun `GIVEN a passphrase-protected wallet WHEN encoded THEN the flag is set`() {
        // Arrange & Act
        val encoded = SocialBackupPayload.encode(MNEMONIC.toCharArray(), isPassphraseRequired = true)

        // Assert
        assertThat(encoded?.decodeToString()).isEqualTo("""{"mnemonic":"$MNEMONIC","passphraseRequired":1}""")
    }

    @Test
    fun `GIVEN a value JSON would have to escape WHEN encoded THEN it is refused`() {
        // Arrange & Act & Assert — no BIP39 word contains these, so such input is not a mnemonic
        assertThat(SocialBackupPayload.encode("""one "two"""".toCharArray(), isPassphraseRequired = false)).isNull()
        assertThat(SocialBackupPayload.encode("one\ntwo".toCharArray(), isPassphraseRequired = false)).isNull()
    }

    @Test
    fun `GIVEN an encoded payload WHEN decoded THEN the mnemonic and the flag survive`() {
        // Arrange
        val encoded = requireNotNull(SocialBackupPayload.encode(MNEMONIC.toCharArray(), isPassphraseRequired = true))

        // Act
        val decoded = SocialBackupJson.decodeFromString<SocialBackupPayload>(encoded.decodeToString())

        // Assert
        assertThat(String(decoded.mnemonic)).isEqualTo(MNEMONIC)
        assertThat(decoded.isPassphraseRequired).isTrue()
    }

    @Test
    fun `GIVEN a flag outside the contract WHEN decoded THEN the payload reports it as unusable`() {
        // Arrange
        val raw = """{"mnemonic":"$MNEMONIC","passphraseRequired":7}"""

        // Act
        val decoded = SocialBackupJson.decodeFromString<SocialBackupPayload>(raw)

        // Assert
        assertThat(decoded.isPassphraseRequired).isNull()
    }

    private companion object {
        const val MNEMONIC = "abandon abandon abandon abandon abandon abandon " +
            "abandon abandon abandon abandon abandon about"
    }
}