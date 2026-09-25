package com.tangem.domain.models.wallet

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class UserWalletIdTest {

    @Test
    fun `GIVEN hex id WHEN fromStringOrNull THEN returns the id`() {
        // Arrange
        val raw = "0123456789abcdef0123456789ABCDEF0123456789abcdef0123456789abcdef"

        // Act
        val actual = UserWalletId.fromStringOrNull(raw)

        // Assert
        assertThat(actual).isEqualTo(UserWalletId(raw))
    }

    @Test
    fun `GIVEN non-hex or blank id WHEN fromStringOrNull THEN returns null without throwing`() {
        // Arrange
        val inputs = listOf("zz", "0x", "not-a-wallet-id", "", "  ", null)

        // Act
        val actual = inputs.map(UserWalletId::fromStringOrNull)

        // Assert
        assertThat(actual).containsExactly(null, null, null, null, null, null)
    }
}
