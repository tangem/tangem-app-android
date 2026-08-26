package com.tangem.datasource.api.tangemTech.models.account

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class SaveWalletAccountsV1RequestTest {

    @Test
    fun `GIVEN body with a personal and a joint row WHEN of THEN only the personal row survives without a type`() {
        // Arrange
        val personal = SaveWalletAccountsResponse.AccountDTO(
            id = "A".repeat(64),
            name = null,
            derivationIndex = 0,
            icon = "Wallet",
            iconColor = "DullLavender",
            type = WalletAccountDTO.Type.CRYPTO.value,
        )
        val joint = personal.copy(id = "B".repeat(64), name = "Family", type = WalletAccountDTO.Type.JOINT.value)

        // Act
        val actual = SaveWalletAccountsV1Request.of(SaveWalletAccountsResponse(accounts = listOf(personal, joint)))

        // Assert
        val expected = SaveWalletAccountsV1Request(
            accounts = listOf(
                SaveWalletAccountsV1Request.AccountDTO(
                    id = personal.id,
                    name = null,
                    derivationIndex = 0,
                    icon = "Wallet",
                    iconColor = "DullLavender",
                ),
            ),
        )
        assertThat(actual).isEqualTo(expected)
    }
}