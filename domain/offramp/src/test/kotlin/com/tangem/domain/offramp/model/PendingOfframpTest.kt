package com.tangem.domain.offramp.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import org.junit.jupiter.api.Test

internal class PendingOfframpTest {

    @Test
    fun `GIVEN record younger than expiry WHEN isExpired THEN returns false`() {
        // Arrange
        val now = 10_000_000L
        val offramp = createOfframp(createdAt = now - PendingOfframp.EXPIRY_MS + 1)

        // Act & Assert
        assertThat(offramp.isExpired(now)).isFalse()
    }

    @Test
    fun `GIVEN record exactly at expiry WHEN isExpired THEN returns true`() {
        // Arrange
        val now = 10_000_000L
        val offramp = createOfframp(createdAt = now - PendingOfframp.EXPIRY_MS)

        // Act & Assert
        assertThat(offramp.isExpired(now)).isTrue()
    }

    @Test
    fun `GIVEN record older than expiry WHEN isExpired THEN returns true`() {
        // Arrange
        val now = 10_000_000L
        val offramp = createOfframp(createdAt = now - PendingOfframp.EXPIRY_MS - 1)

        // Act & Assert
        assertThat(offramp.isExpired(now)).isTrue()
    }

    private fun createOfframp(createdAt: Long) = PendingOfframp(
        requestId = "request-id",
        userWalletId = UserWalletId("0011223344556677"),
        currencyId = "bitcoin",
        createdAt = createdAt,
    )
}