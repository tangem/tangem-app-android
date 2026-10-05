package com.tangem.data.offramp.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.data.offramp.model.PendingOfframpEntry
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.offramp.model.PendingOfframp
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PendingOfframpEntryConverterTest {

    private val converter = PendingOfframpEntryConverter()

    @Test
    fun `GIVEN entry WHEN convert THEN maps all fields and wraps wallet id`() {
        // Arrange
        val entry = PendingOfframpEntry(
            requestId = "request-id-001",
            userWalletId = "0011223344556677",
            currencyId = "bitcoin",
            createdAt = 1_700_000_000_000L,
        )

        // Act
        val result = converter.convert(entry)

        // Assert
        val expected = PendingOfframp(
            requestId = "request-id-001",
            userWalletId = UserWalletId(stringValue = "0011223344556677"),
            currencyId = "bitcoin",
            createdAt = 1_700_000_000_000L,
        )
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `GIVEN entries WHEN convertList THEN converts each preserving order`() {
        // Arrange
        val entries = listOf(
            PendingOfframpEntry(requestId = "id-1", userWalletId = "0011", currencyId = "bitcoin", createdAt = 1L),
            PendingOfframpEntry(requestId = "id-2", userWalletId = "0022", currencyId = "ethereum", createdAt = 2L),
        )

        // Act
        val result = converter.convertList(entries)

        // Assert
        assertThat(result).containsExactly(
            PendingOfframp(
                requestId = "id-1",
                userWalletId = UserWalletId("0011"),
                currencyId = "bitcoin",
                createdAt = 1L,
            ),
            PendingOfframp(
                requestId = "id-2",
                userWalletId = UserWalletId("0022"),
                currencyId = "ethereum",
                createdAt = 2L,
            ),
        ).inOrder()
    }
}