package com.tangem.spend.pushProvisioning.noop

import com.google.common.truth.Truth.assertThat
import com.tangem.spend.pushProvisioning.GoogleWalletProvisioner.AddResult
import com.tangem.spend.pushProvisioning.GoogleWalletProvisioner.Card
import com.tangem.spend.pushProvisioning.GoogleWalletProvisioner.CardStatus
import com.tangem.spend.pushProvisioning.GoogleWalletProvisioner.Environment
import com.tangem.spend.pushProvisioning.GoogleWalletProvisioner.FailureReason
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class NoopGoogleWalletProvisionerTest {

    private val provisioner = NoopGoogleWalletProvisioner()

    @Test
    fun `GIVEN build without the SDK WHEN environment read THEN no environment`() {
        assertThat(provisioner.environment).isNull()
    }

    @Test
    fun `GIVEN build without the SDK WHEN addition state read THEN no card is being added`() {
        assertThat(provisioner.isAdditionInProgress).isFalse()
    }

    @Test
    fun `GIVEN build without the SDK WHEN getCardStatus THEN card is unsupported`() = runTest {
        // Act
        val actual = provisioner.getCardStatus(LAST_FOUR_DIGITS)

        // Assert
        assertThat(actual).isEqualTo(CardStatus.Unsupported)
    }

    @Test
    fun `GIVEN build without the SDK WHEN card added or resumed THEN unsupported failure returned`() = runTest {
        // Arrange
        val expected = AddResult.Failed(
            reason = FailureReason.UNSUPPORTED,
            description = "Push provisioning is not included in this build",
        )

        // Act
        val actual = listOf(
            provisioner.addCard(CARD),
            provisioner.resumeAddingCard(lastFourDigits = LAST_FOUR_DIGITS, displayName = DISPLAY_NAME),
        )

        // Assert
        assertThat(actual).containsExactly(expected, expected)
    }

    private companion object {
        const val LAST_FOUR_DIGITS = "1234"
        const val DISPLAY_NAME = "Tangem Pay"

        val CARD = Card(
            processorCardId = "processor-card-id",
            cardSecret = "card-secret",
            lastFourDigits = LAST_FOUR_DIGITS,
            displayName = DISPLAY_NAME,
            environment = Environment.TEST,
        )
    }
}