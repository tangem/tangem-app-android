package com.tangem.spend.pushProvisioning

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class GoogleWalletProvisionerCardTest {

    @Test
    fun `GIVEN card WHEN toString THEN card secret is not exposed`() {
        // Arrange
        val card = GoogleWalletProvisioner.Card(
            processorCardId = "processor-card-id",
            cardSecret = "time-based-secret",
            lastFourDigits = "1234",
            displayName = "Tangem Pay",
        )

        // Act
        val actual = card.toString()

        // Assert
        assertThat(actual).doesNotContain("time-based-secret")
    }
}