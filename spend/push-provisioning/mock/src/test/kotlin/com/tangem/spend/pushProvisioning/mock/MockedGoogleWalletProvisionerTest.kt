package com.tangem.spend.pushProvisioning.mock

import com.google.common.truth.Truth.assertThat
import com.tangem.spend.pushProvisioning.GoogleWalletProvisioner.AddResult
import com.tangem.spend.pushProvisioning.GoogleWalletProvisioner.Card
import com.tangem.spend.pushProvisioning.GoogleWalletProvisioner.CardStatus
import com.tangem.spend.pushProvisioning.GoogleWalletProvisioner.Environment
import com.tangem.spend.pushProvisioning.GoogleWalletProvisioner.FailureReason
import com.tangem.test.core.ProvideTestModels
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class MockedGoogleWalletProvisionerTest {

    @AfterEach
    fun clearUiTestProperties() {
        System.clearProperty(ADD_RESULT_KEY)
        System.clearProperty(CARD_STATUS_KEY)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class AddCard {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN mocked add result WHEN card added THEN result and status follow it`(model: AddModel) = runTest {
            // Arrange
            System.setProperty(ADD_RESULT_KEY, model.addResult)
            val provisioner = MockedGoogleWalletProvisioner()

            // Act
            val result = provisioner.addCard(CARD)
            val status = provisioner.getCardStatus(LAST_FOUR_DIGITS)

            // Assert
            assertThat(result to status).isEqualTo(model.expectedResult to model.expectedStatus)
        }

        private fun provideTestModels() = listOf(
            AddModel(addResult = "", expectedResult = AddResult.Added, expectedStatus = CardStatus.Added),
            AddModel(
                addResult = "already_added",
                expectedResult = AddResult.AlreadyAdded,
                expectedStatus = CardStatus.Added,
            ),
            AddModel(
                addResult = "cancelled",
                expectedResult = AddResult.Cancelled,
                expectedStatus = CardStatus.NotAdded,
            ),
            AddModel(
                addResult = "network_error",
                expectedResult = AddResult.Failed(
                    reason = FailureReason.NETWORK_ERROR,
                    description = "Mocked network error",
                ),
                expectedStatus = CardStatus.NotAdded,
            ),
            AddModel(
                addResult = "declined",
                expectedResult = AddResult.Failed(
                    reason = FailureReason.TOKENIZATION_DECLINED,
                    description = "Mocked decline",
                ),
                expectedStatus = CardStatus.NotAdded,
            ),
            AddModel(
                addResult = "rate_limited",
                expectedResult = AddResult.Failed(
                    reason = FailureReason.TOO_MANY_ATTEMPTS,
                    description = "Mocked rate limit",
                ),
                expectedStatus = CardStatus.NotAdded,
            ),
            AddModel(
                addResult = "unsupported",
                expectedResult = AddResult.Failed(
                    reason = FailureReason.UNSUPPORTED,
                    description = "Mocked unsupported",
                ),
                expectedStatus = CardStatus.NotAdded,
            ),
            AddModel(
                addResult = "failed",
                expectedResult = AddResult.Failed(
                    reason = FailureReason.MEAWALLET_ERROR,
                    description = "Mocked failure",
                ),
                expectedStatus = CardStatus.NotAdded,
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CardStatusOverride {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN mocked card status WHEN card not added THEN status follows it`(model: StatusModel) = runTest {
            // Arrange
            System.setProperty(CARD_STATUS_KEY, model.cardStatus)
            val provisioner = MockedGoogleWalletProvisioner()

            // Act
            val actual = provisioner.getCardStatus(LAST_FOUR_DIGITS)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            StatusModel(cardStatus = "", expected = CardStatus.NotAdded),
            StatusModel(cardStatus = "unsupported", expected = CardStatus.Unsupported),
            StatusModel(cardStatus = "unknown", expected = CardStatus.Unknown),
            StatusModel(cardStatus = "incomplete", expected = CardStatus.Incomplete),
        )

        @Test
        fun `GIVEN incomplete card status WHEN adding resumed THEN card status is added`() = runTest {
            // Arrange
            System.setProperty(CARD_STATUS_KEY, "incomplete")
            val provisioner = MockedGoogleWalletProvisioner()

            // Act
            provisioner.resumeAddingCard(lastFourDigits = LAST_FOUR_DIGITS, displayName = DISPLAY_NAME)

            // Assert
            assertThat(provisioner.getCardStatus(LAST_FOUR_DIGITS)).isEqualTo(CardStatus.Added)
        }
    }

    internal data class AddModel(val addResult: String, val expectedResult: AddResult, val expectedStatus: CardStatus)

    internal data class StatusModel(val cardStatus: String, val expected: CardStatus)

    private companion object {
        const val ADD_RESULT_KEY = "uitest.tangempay.google_wallet_add_result"
        const val CARD_STATUS_KEY = "uitest.tangempay.google_wallet_card_status"
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