package com.tangem.domain.models.wallet

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class UserWalletIdHashTest {

    @Test
    fun `GIVEN wallet id WHEN hashedStringValue THEN sha256 of its bytes in uppercase hex`() {
        // Arrange
        val userWalletId = UserWalletId(stringValue = "00".repeat(31) + "01")

        // Act
        val actual = userWalletId.hashedStringValue

        // Assert
        assertThat(actual).isEqualTo("EC4916DD28FC4C10D78E287CA5D9CC51EE1AE73CBFDE08C6B37324CBFAAC8BC5")
    }

    @Test
    fun `GIVEN lowercase and uppercase spelling of one id WHEN hashedStringValue THEN hashes match`() {
        // Arrange
        val lowercase = UserWalletId(stringValue = "0a1b2c")
        val uppercase = UserWalletId(stringValue = "0A1B2C")

        // Act
        val actual = lowercase.hashedStringValue

        // Assert
        assertThat(actual).isEqualTo(uppercase.hashedStringValue)
    }
}