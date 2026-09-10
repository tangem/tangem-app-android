package com.tangem.domain.sociallogin.models

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class SocialBackupSecretTest {

    @Test
    fun `GIVEN secret WHEN wiped THEN mnemonic is blanked`() {
        // Arrange
        val secret = SocialBackupSecret(mnemonic = "abandon ability".toCharArray(), isPassphraseRequired = false)

        // Act
        secret.wipe()

        // Assert
        assertThat(String(secret.mnemonic)).isEqualTo("               ")
    }

    @Test
    fun `GIVEN equal content WHEN compared THEN secrets are equal`() {
        // Arrange
        val secret = SocialBackupSecret(mnemonic = "abandon".toCharArray(), isPassphraseRequired = true)
        val sameSecret = SocialBackupSecret(mnemonic = "abandon".toCharArray(), isPassphraseRequired = true)
        val otherSecret = SocialBackupSecret(mnemonic = "abandon".toCharArray(), isPassphraseRequired = false)

        // Assert
        assertThat(secret).isEqualTo(sameSecret)
        assertThat(secret.hashCode()).isEqualTo(sameSecret.hashCode())
        assertThat(secret).isNotEqualTo(otherSecret)
    }

    @Test
    fun `GIVEN secret WHEN printed THEN mnemonic is masked`() {
        // Arrange
        val secret = SocialBackupSecret(mnemonic = "abandon ability".toCharArray(), isPassphraseRequired = true)

        // Assert
        assertThat(secret.toString())
            .isEqualTo("SocialBackupSecret(mnemonic=***, isPassphraseRequired=true)")
    }
}