package com.tangem.features.polymarket.impl.placeprediction.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.polymarket.interactor.PredictionSignRequest
import com.tangem.domain.polymarket.model.PredictionOrderFailure
import com.tangem.domain.polymarket.model.PredictionOrderValidationError
import com.tangem.features.polymarket.impl.common.FakeSlotHost
import com.tangem.features.polymarket.impl.placeprediction.PlacePredictionRoute
import com.tangem.features.polymarket.impl.placeprediction.entity.SubmitUM
import com.tangem.features.polymarket.impl.placeprediction.entity.QuoteUM
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
internal class PlacePredictionSnapshotTest : PlacePredictionModelTestBase() {

    @Test
    fun `GIVEN a refresh in flight WHEN next is tapped THEN summary signs the snapshot from the tap`() = runTest {
        // Arrange
        coEvery { getQuoteUseCase(any()) } returns createQuote().right() coAndThen {
            delay(1_000)
            createQuote(total = BigDecimal("99")).right()
        }
        val amount = createAmountModel(this)
        advanceUntilIdle()
        amount.onResume()
        amount.onAmountChange("10")
        advanceTimeBy(QUOTE_DEBOUNCE_MILLIS + 1)
        val accepted = amount.uiState.value
        advanceTimeBy(QUOTE_POLL_INTERVAL_MILLIS + 1)
        val request = slot<PredictionSignRequest>()
        coEvery { signOrderInteractor(capture(request)) } returns SIGNED_ORDER.right()

        // Act
        val summary = createSummaryModel(this, amount)
        advanceTimeBy(QUOTE_POLL_INTERVAL_MILLIS * 3)

        // Assert
        assertThat(summary.uiState.value).isEqualTo(accepted)
        summary.onPlaceClick()
        advanceUntilIdle()
        assertThat(request.captured.quote).isEqualTo((accepted.quote as QuoteUM.Content).signable.quote)
        assertThat(request.captured.quoteReceivedAtMillis)
            .isEqualTo((accepted.quote as QuoteUM.Content).signable.receivedAtMillis)
        coVerify(exactly = 2) { getQuoteUseCase(any()) }
    }

    @Test
    fun `GIVEN a prepared summary WHEN amount is edited independently THEN its figures and fees stay unchanged`() = runTest {
        // Arrange
        val amount = createAmountModel(this)
        advanceUntilIdle()
        amount.onResume()
        amount.onAmountChange("10")
        advanceTimeBy(QUOTE_DEBOUNCE_MILLIS + 1)
        val summary = createSummaryModel(this, amount)
        val accepted = summary.uiState.value
        val fees = summary.feeSheetState.value
        val sheet = FakeSlotHost(summary.feeSheetNavigation)
        summary.onFeeClick()

        // Act
        amount.onAmountChange("20")
        advanceTimeBy(QUOTE_POLL_INTERVAL_MILLIS * 3)

        // Assert
        assertThat(summary.uiState.value).isEqualTo(accepted)
        assertThat(summary.feeSheetState.value).isEqualTo(fees)
        assertThat(sheet.configuration).isEqualTo(Unit)
        coVerify(exactly = 1) { getQuoteUseCase(any()) }
    }

    @Test
    fun `GIVEN new slippage WHEN its quote is pending THEN the previous quote cannot reach summary`() = runTest {
        // Arrange
        val amount = createAmountModel(this)
        advanceUntilIdle()
        amount.onResume()
        amount.onAmountChange("10")
        advanceTimeBy(QUOTE_DEBOUNCE_MILLIS + 1)
        val route = slot<PlacePredictionRoute.Slippage>()
        every { router.push(capture(route), any()) } returns Unit
        amount.onSlippageClick()
        amount.onPause()
        coEvery { getQuoteUseCase(any()) } coAnswers {
            delay(1_000)
            createQuote().right()
        }

        // Act
        route.captured.onConfirm(BigDecimal("1.5"))
        amount.onResume()
        runCurrent()
        amount.onNextClick()

        // Assert
        assertThat(amount.uiState.value.quote).isEqualTo(QuoteUM.Loading)
        assertThat(amount.uiState.value.isPrimaryButtonEnabled).isFalse()
        verify(exactly = 0) { router.push(any<PlacePredictionRoute.Summary>(), any()) }
        advanceTimeBy(1_001)
        val prepared = prepareOrder(amount)
        assertThat(prepared.state.slippage.percent).isEqualTo(BigDecimal("1.5"))
        coVerify(exactly = 1) { getQuoteUseCase(match { it.slippagePercent == BigDecimal("1.5") }) }
    }

    @Test
    fun `GIVEN no quote WHEN next is tapped THEN summary does not open`() = runTest {
        // Arrange
        val amount = createAmountModel(this)
        advanceUntilIdle()

        // Act
        amount.onNextClick()

        // Assert
        verify(exactly = 0) { router.push(any(), any()) }
    }

    @Test
    fun `GIVEN an expired quote WHEN confirmed THEN the generic error keeps the snapshot on summary`() = runTest {
        // Arrange
        val amount = createAmountModel(this)
        advanceUntilIdle()
        amount.onResume()
        amount.onAmountChange("10")
        advanceTimeBy(QUOTE_DEBOUNCE_MILLIS + 1)
        val summary = createSummaryModel(this, amount)
        coEvery { signOrderInteractor(any()) } returns PredictionOrderFailure.Invalid(
            PredictionOrderValidationError.StaleQuote(ageMillis = 60_001),
        ).left()
        val accepted = summary.uiState.value
        val dialog = slot<DialogMessage>()
        every { messageSender.send(capture(dialog)) } returns Unit

        // Act
        summary.onPlaceClick()
        advanceUntilIdle()
        dialog.captured.secondAction?.onClick?.invoke()
        advanceTimeBy(QUOTE_POLL_INTERVAL_MILLIS * 3)

        // Assert
        assertThat(dialog.captured.title).isEqualTo(resourceReference(R.string.common_something_went_wrong))
        assertThat(dialog.captured.message).isEqualTo(resourceReference(R.string.prediction_place_error_description))
        assertThat(dialog.captured.firstAction.title).isEqualTo(resourceReference(R.string.common_support))
        assertThat(dialog.captured.secondAction).isNotNull()
        assertThat(summary.uiState.value).isEqualTo(accepted.copy(submit = SubmitUM.Idle))
        verify(exactly = 0) { router.popTo(any<PlacePredictionRoute>(), any()) }
        coVerify(exactly = 0) { placeOrderUseCase(any(), any(), any()) }
        coVerify(exactly = 1) { getQuoteUseCase(any()) }
    }
}