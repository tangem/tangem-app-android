package com.tangem.tap.data.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.offramp.model.PendingOfframp
import com.tangem.tap.data.model.PendingOfframpEntry
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
            PendingOfframpEntry("id-1", "0011", "bitcoin", 1L),
            PendingOfframpEntry("id-2", "0022", "ethereum", 2L),
        )

        // Act
        val result = converter.convertList(entries)

        // Assert
        assertThat(result).containsExactly(
            PendingOfframp("id-1", UserWalletId("0011"), "bitcoin", 1L),
            PendingOfframp("id-2", UserWalletId("0022"), "ethereum", 2L),
        ).inOrder()
    }
}