package com.tangem.domain.models.wallet

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class UserWalletIdFromHexTest {

    @Test
    fun `GIVEN hex string WHEN fromHexOrNull THEN returns wallet id`() {
        // Act
        val actual = UserWalletId.fromHexOrNull("0A1B2C")

        // Assert
        assertThat(actual).isEqualTo(UserWalletId("0A1B2C"))
    }

    @Test
    fun `GIVEN non-hex string WHEN fromHexOrNull THEN returns null`() {
        // Act
        val actual = UserWalletId.fromHexOrNull("not-a-hex-id")

        // Assert
        assertThat(actual).isNull()
    }
}