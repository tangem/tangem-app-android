package com.tangem.features.send.send.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.features.send.api.SendComponent
import com.tangem.features.send.api.analytics.CommonSendAnalyticEvents
import com.tangem.features.send.api.entity.PredefinedValues
import com.tangem.features.send.common.CommonSendRoute
import com.tangem.features.send.common.ui.state.ConfirmUM
import com.tangem.features.send.send.SendModelTestBase
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeSelectorUM as FeeSelectorUMRedesigned

@OptIn(ExperimentalCoroutinesApi::class)
internal class SendModelTest : SendModelTestBase() {

    @Nested
    inner class OnNextClick {

        @Test
        fun `GIVEN amount route AND predefined main screen QR WHEN onNextClick THEN push Confirm`() = runTest {
            // Arrange
            val model = createSendModel(this)
            model.predefinedValues = PredefinedValues.Content.QrCode(
                amount = "1.0",
                address = "addr123",
                memo = null,
                source = PredefinedValues.Source.MAIN_SCREEN,
            )

            // Act
            model.onNextClick(CommonSendRoute.Amount(isEditMode = false))

            // Assert
            verify(exactly = 1) { router.push(CommonSendRoute.Confirm, any()) }
        }

        @Test
        fun `GIVEN amount route AND NOT main screen QR WHEN onNextClick THEN push Destination`() = runTest {
            // Arrange
            val model = createSendModel(this)
            model.predefinedValues = PredefinedValues.Empty

            // Act
            model.onNextClick(CommonSendRoute.Amount(isEditMode = false))

            // Assert
            verify(exactly = 1) { router.push(CommonSendRoute.Destination(isEditMode = false), any()) }
        }

        @Test
        fun `GIVEN destination route WHEN onNextClick THEN push Confirm`() = runTest {
            // Arrange
            val model = createSendModel(this)

            // Act
            model.onNextClick(CommonSendRoute.Destination(isEditMode = false))

            // Assert
            verify(exactly = 1) { router.push(CommonSendRoute.Confirm, any()) }
        }

        @Test
        fun `GIVEN route in edit mode WHEN onNextClick THEN pop without push`() = runTest {
            // Arrange
            val model = createSendModel(this)

            // Act
            model.onNextClick(CommonSendRoute.Amount(isEditMode = true))

            // Assert
            verify(exactly = 1) { router.pop(any()) }
            verify(exactly = 0) { router.push(any(), any()) }
        }

        @Test
        fun `GIVEN confirm route WHEN onNextClick THEN push ConfirmSuccess`() =
            runTest {
                // Arrange
                // CommonSendRoute.Confirm.isEditMode == false, so onNextClick pushes ConfirmSuccess.
                val model = createSendModel(this)

                // Act
                model.onNextClick(CommonSendRoute.Confirm)

                // Assert
                verify(exactly = 1) { router.push(CommonSendRoute.ConfirmSuccess, any()) }
                verify(exactly = 0) { router.pop(any()) }
            }
    }

    @Nested
    inner class ConsumeEntryType {

        @Test
        fun `GIVEN entry type QR WHEN consumeEntryType first call THEN return QR`() = runTest {
            // Arrange
            val params = defaultSendParams().copy(entryType = SendComponent.EntryType.QR)
            val model = createSendModel(this, MutableParamsContainer(params))

            // Act
            val result = model.consumeEntryType()

            // Assert
            assertThat(result).isEqualTo(CommonSendAnalyticEvents.SendEntryType.QR)
        }

        @Test
        fun `GIVEN entry type QR WHEN consumeEntryType called twice THEN second returns Manual`() = runTest {
            // Arrange
            val params = defaultSendParams().copy(entryType = SendComponent.EntryType.QR)
            val model = createSendModel(this, MutableParamsContainer(params))

            // Act
            val first = model.consumeEntryType()
            val second = model.consumeEntryType()

            // Assert
            assertThat(first).isEqualTo(CommonSendAnalyticEvents.SendEntryType.QR)
            assertThat(second).isEqualTo(CommonSendAnalyticEvents.SendEntryType.Manual)
        }

        @Test
        fun `GIVEN entry type Manual WHEN consumeEntryType THEN return Manual`() = runTest {
            // Arrange
            val params = defaultSendParams().copy(entryType = SendComponent.EntryType.Manual)
            val model = createSendModel(this, MutableParamsContainer(params))

            // Act
            val result = model.consumeEntryType()

            // Assert
            assertThat(result).isEqualTo(CommonSendAnalyticEvents.SendEntryType.Manual)
        }
    }

    @Nested
    inner class LoadFee {

        @Test
        fun `GIVEN transaction created WHEN loadFee THEN return fee from use case`() = runTest {
            // Arrange
            val model = createSendModel(this)
            advanceUntilIdle()
            model.predefinedValues = deeplink(amount = "1.0")
            val expectedFee = mockk<TransactionFee>(relaxed = true)
            coEvery { getFeeUseCase(any(), any(), any()) } returns expectedFee.right()

            // Act
            val result = model.loadFee()

            // Assert
            assertThat(result).isEqualTo(expectedFee.right())
        }

        @Test
        fun `GIVEN transaction creation fails WHEN loadFee THEN return DataError`() = runTest {
            // Arrange
            val model = createSendModel(this)
            advanceUntilIdle()
            model.predefinedValues = deeplink(amount = "1.0")
            coEvery {
                createTransferTransactionUseCase(any(), any<String>(), any(), any(), any(), any())
            } returns IllegalStateException("boom").left()

            // Act
            val result = model.loadFee()

            // Assert
            assertThat(result.leftOrNull()).isInstanceOf(GetFeeError.DataError::class.java)
        }

        @Test
        fun `GIVEN fee use case fails WHEN loadFee THEN return that error`() = runTest {
            // Arrange
            val model = createSendModel(this)
            advanceUntilIdle()
            model.predefinedValues = deeplink(amount = "1.0")
            coEvery { getFeeUseCase(any(), any(), any()) } returns GetFeeError.UnknownError.left()

            // Act
            val result = model.loadFee()

            // Assert
            assertThat(result).isEqualTo(GetFeeError.UnknownError.left())
        }
    }

    @Nested
    inner class OnBackClick {

        @Test
        fun `GIVEN amount route non-edit WHEN onBackClick THEN send analytics and pop`() = runTest {
            // Arrange
            val model = createSendModel(this)

            // Act
            model.onBackClick(CommonSendRoute.Amount(isEditMode = false))

            // Assert
            verify(exactly = 1) { analyticsEventHandler.send(any<CommonSendAnalyticEvents.CloseButtonClicked>()) }
            verify(exactly = 1) { router.pop(any()) }
        }

        @Test
        fun `GIVEN destination route edit WHEN onBackClick THEN pop without analytics`() = runTest {
            // Arrange
            val model = createSendModel(this)

            // Act
            model.onBackClick(CommonSendRoute.Destination(isEditMode = true))

            // Assert
            verify(exactly = 0) { analyticsEventHandler.send(any<CommonSendAnalyticEvents.CloseButtonClicked>()) }
            verify(exactly = 1) { router.pop(any()) }
        }
    }

    @Nested
    inner class ResetSendNavigation {

        @Test
        fun `GIVEN any state WHEN resetSendNavigation THEN reset states and popTo Amount`() = runTest {
            // Arrange
            val model = createSendModel(this)

            // Act
            model.resetSendNavigation()

            // Assert
            val state = model.uiState.value
            assertThat(state.feeSelectorUM).isEqualTo(FeeSelectorUMRedesigned.Loading)
            assertThat(state.confirmUM).isEqualTo(ConfirmUM.Empty)
            assertThat(state.confirmData).isNull()
            verify(exactly = 1) { router.popTo(CommonSendRoute.Amount(isEditMode = false), any()) }
        }
    }

    private fun deeplink(amount: String) = PredefinedValues.Content.Deeplink(
        amount = amount,
        address = "addr123",
        memo = null,
        transactionId = "tx123",
    )
}