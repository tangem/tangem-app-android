package com.tangem.features.polymarket.impl.placeprediction.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.network.Network
import com.tangem.domain.polymarket.PolymarketCollateralCurrencyFactory
import com.tangem.domain.polymarket.interactor.PredictionSignRequest
import com.tangem.domain.polymarket.model.PolymarketPositionsError
import com.tangem.domain.polymarket.model.PolymarketAuthError
import com.tangem.domain.polymarket.model.PolymarketBalanceAllowance
import com.tangem.domain.polymarket.model.PolymarketSigningError
import com.tangem.domain.polymarket.model.PredictionOrderFailure
import com.tangem.domain.polymarket.model.PredictionOrderSide
import com.tangem.domain.polymarket.model.PredictionOrderValidationError
import com.tangem.domain.polymarket.model.PredictionOrderState
import com.tangem.domain.polymarket.model.PredictionOrderStatus
import com.tangem.domain.polymarket.model.PredictionPlaceOrderError
import com.tangem.domain.polymarket.model.PredictionQuoteStatus
import com.tangem.domain.polymarket.usecase.DelayedPredictionOrderOutcome
import com.tangem.features.polymarket.impl.navigation.PolymarketRoute
import com.tangem.features.polymarket.impl.common.FakeSlotHost
import com.tangem.features.polymarket.impl.placeprediction.PlacePredictionRoute
import com.tangem.features.polymarket.impl.placeprediction.entity.DEFAULT_SLIPPAGE_PERCENT
import com.tangem.features.polymarket.impl.placeprediction.entity.OrderSideUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PlaceResultUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SellPositionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SharesProceedsUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PredictionNotificationUM
import com.tangem.features.polymarket.impl.placeprediction.entity.QuoteUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SlippageUM
import com.tangem.features.polymarket.impl.placeprediction.model.converter.PredictionFeeSheetUMConverter
import com.tangem.features.polymarket.impl.placeprediction.entity.SubmitUM
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.clearMocks
import io.mockk.coVerify
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal


@OptIn(ExperimentalCoroutinesApi::class)
internal class PlacePredictionSummaryModelTest : PlacePredictionModelTestBase() {

    @Nested
    inner class Submission {

        @Test
        fun `GIVEN submit started WHEN poll interval elapses THEN quote NOT requested`() = runTest {
            // Arrange
            val amountModel = createAmountModel(testScope = this)
            advanceUntilIdle()
            amountModel.onResume()
            amountModel.onAmountChange(value = "10")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
            val model = createSummaryModel(testScope = this, amountModel = amountModel)

            // Act
            model.onPlaceClick()
            advanceTimeBy(delayTimeMillis = QUOTE_POLL_INTERVAL_MILLIS * 3)

            // Assert
            coVerify(exactly = 1) { getQuoteUseCase(request = any()) }
            assertThat(model.uiState.value.submit).isNotEqualTo(SubmitUM.Idle)

            model.onDestroy()
        }

        @Test
        fun `GIVEN a priced quote WHEN place clicked twice THEN the order is signed once`() = runTest {
            // Arrange
            val amountModel = createAmountModel(testScope = this)
            advanceUntilIdle()
            amountModel.onResume()
            amountModel.onAmountChange(value = "10")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
            val model = createSummaryModel(testScope = this, amountModel = amountModel)

            // Act
            model.onPlaceClick()
            model.onPlaceClick()
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) { signOrderInteractor(request = any()) }
            coVerify(exactly = 1) { placeOrderUseCase(any(), any(), any()) }

            model.onDestroy()
        }

        @Test
        fun `GIVEN the order fills WHEN place clicked THEN the status step replaces the stack`() = runTest {
            // Arrange
            val amountModel = createAmountModel(testScope = this)
            advanceUntilIdle()
            amountModel.onResume()
            amountModel.onAmountChange(value = "10")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
            val model = createSummaryModel(testScope = this, amountModel = amountModel)
            var submitWhilePosting: SubmitUM? = null
            coEvery { placeOrderUseCase(any(), any(), any()) } answers {
                submitWhilePosting = model.uiState.value.submit
                createPlaceResult().right()
            }

            // Act
            model.onPlaceClick()
            advanceUntilIdle()

            // Assert
            assertThat(submitWhilePosting).isEqualTo(SubmitUM.Posting)
            val result = (model.uiState.value.submit as SubmitUM.Result).result
            assertThat(result).isInstanceOf(PlaceResultUM.Filled::class.java)
            assertThat((result as PlaceResultUM.Filled).collateralAmount).isEqualTo(BigDecimal("10"))
            verify { router.replaceAll(any<PlacePredictionRoute.Status>(), onComplete = any()) }

            model.onDestroy()
        }

        @Test
        fun `GIVEN the order fills WHEN place clicked THEN the balance window opens`() = runTest {
            // Arrange
            val amountModel = createAmountModel(testScope = this)
            advanceUntilIdle()
            amountModel.onResume()
            amountModel.onAmountChange(value = "10")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
            val model = createSummaryModel(testScope = this, amountModel = amountModel)

            // Act
            model.onPlaceClick()
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { fundingWindow.open(userWalletId = USER_WALLET_ID) }

            model.onDestroy()
        }

        @Test
        fun `GIVEN the exchange found no match WHEN place clicked THEN no balance window opens`() = runTest {
            // Arrange
            val amountModel = createAmountModel(testScope = this)
            advanceUntilIdle()
            amountModel.onResume()
            amountModel.onAmountChange(value = "10")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
            val model = createSummaryModel(testScope = this, amountModel = amountModel)
            coEvery { placeOrderUseCase(any(), any(), any()) } returns
                PredictionPlaceOrderError.NoLiquidity(detail = "no orders found to match with FAK order").left()

            // Act
            model.onPlaceClick()
            advanceUntilIdle()

            // Assert
            verify(exactly = 0) { fundingWindow.open(userWalletId = any()) }

            model.onDestroy()
        }

        @Test
        fun `GIVEN no answer from the exchange WHEN place clicked THEN the balance window still opens`() = runTest {
            // Arrange
            val amountModel = createAmountModel(testScope = this)
            advanceUntilIdle()
            amountModel.onResume()
            amountModel.onAmountChange(value = "10")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
            val model = createSummaryModel(testScope = this, amountModel = amountModel)
            coEvery { placeOrderUseCase(any(), any(), any()) } returns
                PredictionPlaceOrderError.Ambiguous(detail = "the exchange never answered").left()

            // Act
            model.onPlaceClick()
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { fundingWindow.open(userWalletId = USER_WALLET_ID) }

            model.onDestroy()
        }

        @Test
        fun `GIVEN the signature fails WHEN place clicked THEN no balance window opens`() = runTest {
            // Arrange
            val amountModel = createAmountModel(testScope = this)
            advanceUntilIdle()
            amountModel.onResume()
            amountModel.onAmountChange(value = "10")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
            val model = createSummaryModel(testScope = this, amountModel = amountModel)
            coEvery { signOrderInteractor(request = any()) } returns
                PredictionOrderFailure.Signing(error = PolymarketSigningError.UserCancelled).left()
            amountModel.onPause()

            // Act
            model.onPlaceClick()
            advanceUntilIdle()

            // Assert
            verify(exactly = 0) { fundingWindow.open(userWalletId = any()) }

            model.onDestroy()
        }

        @Test
        fun `GIVEN the user declines the signature WHEN place clicked THEN the flow returns to idle silently`() =
            runTest {
                // Arrange
                val amountModel = createAmountModel(testScope = this)
                advanceUntilIdle()
                amountModel.onResume()
                amountModel.onAmountChange(value = "10")
                advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
                val model = createSummaryModel(testScope = this, amountModel = amountModel)
                coEvery { signOrderInteractor(request = any()) } returns
                    PredictionOrderFailure.Signing(error = PolymarketSigningError.UserCancelled).left()
                amountModel.onPause()

                // Act
                model.onPlaceClick()
                advanceUntilIdle()

                // Assert
                assertThat(model.uiState.value.submit).isEqualTo(SubmitUM.Idle)
                verify(exactly = 0) { messageSender.send(any()) }
                coVerify(exactly = 0) { placeOrderUseCase(any(), any(), any()) }

                model.onDestroy()
            }

        @Test
        fun `GIVEN the order fails its own checks WHEN place clicked THEN nothing is posted`() = runTest {
            // Arrange
            val amountModel = createAmountModel(testScope = this)
            advanceUntilIdle()
            amountModel.onResume()
            amountModel.onAmountChange(value = "10")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
            val model = createSummaryModel(testScope = this, amountModel = amountModel)
            coEvery { signOrderInteractor(request = any()) } returns
                PredictionOrderFailure.Invalid(
                    error = PredictionOrderValidationError.StaleQuote(ageMillis = 120_000),
                ).left()
            amountModel.onPause()

            // Act
            model.onPlaceClick()
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.submit).isEqualTo(SubmitUM.Idle)
            coVerify(exactly = 0) { placeOrderUseCase(any(), any(), any()) }
            verify { messageSender.send(any()) }

            model.onDestroy()
        }

        @Test
        fun `GIVEN the exchange found no match WHEN place clicked THEN the flow ends on the no-liquidity result`() =
            runTest {
                // Arrange
                val amountModel = createAmountModel(testScope = this)
                advanceUntilIdle()
                amountModel.onResume()
                amountModel.onAmountChange(value = "10")
                advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
                val model = createSummaryModel(testScope = this, amountModel = amountModel)
                coEvery { placeOrderUseCase(any(), any(), any()) } returns
                    PredictionPlaceOrderError.NoLiquidity(detail = "no orders found to match with FAK order").left()

                // Act
                model.onPlaceClick()
                advanceUntilIdle()

                // Assert
                assertThat(model.uiState.value.submit).isInstanceOf(SubmitUM.Result::class.java)
                assertThat((model.uiState.value.submit as SubmitUM.Result).result)
                    .isInstanceOf(PlaceResultUM.NotFilled::class.java)
                verify { router.replaceAll(any<PlacePredictionRoute.Status>()) }
                verify(exactly = 0) { messageSender.send(any()) }

                model.onDestroy()
            }

        @Test
        fun `GIVEN a matched order filled no shares WHEN place clicked THEN the flow reports a failure`() = runTest {
            // Arrange
            val amountModel = createAmountModel(testScope = this)
            advanceUntilIdle()
            amountModel.onResume()
            amountModel.onAmountChange(value = "10")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
            val model = createSummaryModel(testScope = this, amountModel = amountModel)
            coEvery { placeOrderUseCase(any(), any(), any()) } returns
                createPlaceResult(takingAmount = BigDecimal.ZERO).right()
            amountModel.onPause()

            // Act
            model.onPlaceClick()
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.submit).isEqualTo(SubmitUM.Idle)
            verify { messageSender.send(any()) }
            verify(exactly = 0) { router.replaceAll(any<PlacePredictionRoute.Status>()) }

            model.onDestroy()
        }

        @Test
        fun `GIVEN no answer from the exchange WHEN place clicked THEN the outcome is submitted and not retried`() =
            runTest {
                // Arrange
                val amountModel = createAmountModel(testScope = this)
                advanceUntilIdle()
                amountModel.onResume()
                amountModel.onAmountChange(value = "10")
                advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
                val model = createSummaryModel(testScope = this, amountModel = amountModel)
                coEvery { placeOrderUseCase(any(), any(), any()) } returns
                    PredictionPlaceOrderError.Ambiguous(detail = "timeout").left()

                // Act
                model.onPlaceClick()
                advanceUntilIdle()

                // Assert
                assertThat(model.uiState.value.submit).isInstanceOf(SubmitUM.Result::class.java)
                assertThat((model.uiState.value.submit as SubmitUM.Result).result)
                    .isInstanceOf(PlaceResultUM.Submitted::class.java)
                coVerify(exactly = 1) { placeOrderUseCase(any(), any(), any()) }

                model.onDestroy()
            }

        @Test
        fun `GIVEN the exchange delays the order WHEN place clicked THEN the fill is read back before the result`() =
            runTest {
                // Arrange
                val amountModel = createAmountModel(testScope = this)
                advanceUntilIdle()
                amountModel.onResume()
                amountModel.onAmountChange(value = "10")
                advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
                val model = createSummaryModel(testScope = this, amountModel = amountModel)
                coEvery { placeOrderUseCase(any(), any(), any()) } returns createPlaceResult(status = "delayed").right()
                var submitWhileWaiting: SubmitUM? = null
                coEvery { awaitDelayedOrderUseCase(addresses = ADDRESSES, orderId = ORDER_ID) } answers {
                    submitWhileWaiting = model.uiState.value.submit
                    DelayedPredictionOrderOutcome.Matched(
                        order = PredictionOrderState(
                            status = PredictionOrderStatus.Matched,
                            price = BigDecimal("0.5"),
                            originalSize = BigDecimal("23.2"),
                            sizeMatched = BigDecimal("23.2"),
                        ),
                    )
                }

                // Act
                model.onPlaceClick()
                advanceUntilIdle()

                // Assert
                assertThat(submitWhileWaiting).isEqualTo(SubmitUM.AwaitingFill)
                assertThat((model.uiState.value.submit as SubmitUM.Result).result).isEqualTo(
                    PlaceResultUM.Filled(
                        placedAt = (model.uiState.value.submit as SubmitUM.Result).result.placedAt,
                        collateralAmount = BigDecimal("11.60"),
                        shares = BigDecimal("23.2"),
                    ),
                )
                verify { router.replaceAll(any<PlacePredictionRoute.Status>(), onComplete = any()) }

                model.onDestroy()
            }

        @Test
        fun `GIVEN the book answers that a delayed order did not match WHEN the wait ends THEN a dialog returns the summary to idle`() =
            runTest {
                // Arrange
                val amountModel = createAmountModel(testScope = this)
                advanceUntilIdle()
                amountModel.onResume()
                amountModel.onAmountChange(value = "10")
                advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
                val model = createSummaryModel(testScope = this, amountModel = amountModel)
                coEvery { placeOrderUseCase(any(), any(), any()) } returns createPlaceResult(status = "delayed").right()
                coEvery { awaitDelayedOrderUseCase(addresses = ADDRESSES, orderId = ORDER_ID) } returns
                    DelayedPredictionOrderOutcome.Failed(detail = "ORDER_STATUS_CANCELED")
                amountModel.onPause()

                // Act
                model.onPlaceClick()
                advanceUntilIdle()

                // Assert
                assertThat(model.uiState.value.submit).isEqualTo(SubmitUM.Idle)
                verify(exactly = 1) { messageSender.send(any()) }
                verify(exactly = 0) { router.replaceAll(any<PlacePredictionRoute.Status>(), onComplete = any()) }

                model.onDestroy()
            }

        @Test
        fun `GIVEN the reads of a delayed order run out WHEN the wait ends THEN a dialog returns the summary to idle`() =
            runTest {
                // Arrange
                val amountModel = createAmountModel(testScope = this)
                advanceUntilIdle()
                amountModel.onResume()
                amountModel.onAmountChange(value = "10")
                advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
                val model = createSummaryModel(testScope = this, amountModel = amountModel)
                coEvery { placeOrderUseCase(any(), any(), any()) } returns createPlaceResult(status = "delayed").right()
                coEvery { awaitDelayedOrderUseCase(addresses = ADDRESSES, orderId = ORDER_ID) } returns
                    DelayedPredictionOrderOutcome.Unconfirmed
                amountModel.onPause()

                // Act
                model.onPlaceClick()
                advanceUntilIdle()

                // Assert
                assertThat(model.uiState.value.submit).isEqualTo(SubmitUM.Idle)
                verify(exactly = 1) { messageSender.send(any()) }
                verify(exactly = 0) { router.replaceAll(any<PlacePredictionRoute.Status>(), onComplete = any()) }
                coVerify(exactly = 1) { placeOrderUseCase(any(), any(), any()) }

                model.onDestroy()
            }

        @Test
        fun `GIVEN the fill is being read back WHEN back or close is tapped THEN the flow stays put`() = runTest {
            // Arrange
            val amountModel = createAmountModel(testScope = this)
            advanceUntilIdle()
            amountModel.onResume()
            amountModel.onAmountChange(value = "10")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
            val model = createSummaryModel(testScope = this, amountModel = amountModel)
            coEvery { placeOrderUseCase(any(), any(), any()) } returns createPlaceResult(status = "delayed").right()
            coEvery { awaitDelayedOrderUseCase(addresses = ADDRESSES, orderId = ORDER_ID) } coAnswers {
                delay(timeMillis = 30_000)
                DelayedPredictionOrderOutcome.Unconfirmed
            }
            amountModel.onPause()
            model.onPlaceClick()
            advanceTimeBy(delayTimeMillis = 1_000)
            clearMocks(router)

            // Act
            model.onBackClick()
            model.onCloseClick()

            // Assert
            assertThat(model.isSubmitting()).isTrue()
            verify(exactly = 0) { router.pop(onComplete = any()) }
            verify(exactly = 0) { router.popTo(any<PolymarketRoute.Main>(), onComplete = any()) }
            advanceUntilIdle()
            assertThat(model.uiState.value.submit).isEqualTo(SubmitUM.Idle)

            model.onDestroy()
        }

        @Test
        fun `GIVEN the balance no longer covers the order WHEN place clicked THEN nothing is signed`() = runTest {
            // Arrange
            val amountModel = createAmountModel(testScope = this)
            advanceUntilIdle()
            amountModel.onResume()
            amountModel.onAmountChange(value = "10")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
            val model = createSummaryModel(testScope = this, amountModel = amountModel)
            coEvery { getBalanceInteractor(addresses = ADDRESSES) } returns
                PolymarketBalanceAllowance(balance = BigDecimal("1"), allowances = mapOf("0xspender" to BigDecimal("1000"))).right()
            amountModel.onPause()

            // Act
            model.onPlaceClick()
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.submit).isEqualTo(SubmitUM.Idle)
            coVerify(exactly = 0) { signOrderInteractor(request = any()) }

            model.onDestroy()
        }

        @Test
        fun `GIVEN a priced quote WHEN place clicked THEN the quote that was shown is the one signed`() = runTest {
            // Arrange
            val amountModel = createAmountModel(testScope = this)
            advanceUntilIdle()
            amountModel.onResume()
            amountModel.onAmountChange(value = "10")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
            val model = createSummaryModel(testScope = this, amountModel = amountModel)
            val request = slot<PredictionSignRequest>()
            coEvery { signOrderInteractor(request = capture(request)) } returns SIGNED_ORDER.right()

            // Act
            model.onPlaceClick()
            advanceUntilIdle()

            // Assert
            assertThat(request.captured.quote).isEqualTo(createQuote())
            assertThat(request.captured.assetId).isEqualTo(ASSET_ID)
            assertThat(request.captured.isNegRisk).isFalse()

            model.onDestroy()
        }

        @Test
        fun `GIVEN the amount step is left WHEN a failed attempt returns to idle THEN the quote is not refreshed`() = runTest {
            // Arrange
            val amountModel = createAmountModel(testScope = this)
            advanceUntilIdle()
            amountModel.onResume()
            amountModel.onAmountChange(value = "10")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
            val model = createSummaryModel(testScope = this, amountModel = amountModel)
            amountModel.onPause()
            coEvery { signOrderInteractor(request = any()) } returns
                PredictionOrderFailure.Signing(error = PolymarketSigningError.UserCancelled).left()

            // Act
            model.onPlaceClick()
            advanceTimeBy(delayTimeMillis = QUOTE_POLL_INTERVAL_MILLIS * 3)

            // Assert
            assertThat(model.uiState.value.submit).isEqualTo(SubmitUM.Idle)
            coVerify(exactly = 1) { getQuoteUseCase(request = any()) }

            model.onDestroy()
        }

        @Test
        fun `GIVEN the screen is hidden WHEN an attempt returns to idle THEN nothing is polled off-screen`() = runTest {
            // Arrange
            val amountModel = createAmountModel(testScope = this)
            advanceUntilIdle()
            amountModel.onResume()
            amountModel.onAmountChange(value = "10")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
            val model = createSummaryModel(testScope = this, amountModel = amountModel)
            coEvery { signOrderInteractor(request = any()) } returns
                PredictionOrderFailure.Signing(error = PolymarketSigningError.UserCancelled).left()
            amountModel.onPause()

            // Act
            model.onPlaceClick()
            advanceTimeBy(delayTimeMillis = QUOTE_POLL_INTERVAL_MILLIS * 3)

            // Assert
            coVerify(exactly = 1) { getQuoteUseCase(request = any()) }

            model.onDestroy()
        }

        @Test
        fun `GIVEN a submission in flight WHEN close is tapped THEN the flow refuses to leave`() = runTest {
            // Arrange
            val amountModel = createAmountModel(testScope = this)
            advanceUntilIdle()
            amountModel.onResume()
            amountModel.onAmountChange(value = "10")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
            val model = createSummaryModel(testScope = this, amountModel = amountModel)
            coEvery { placeOrderUseCase(any(), any(), any()) } coAnswers {
                // Act
                model.onCloseClick()
                model.onBackClick()
                createPlaceResult().right()
            }

            // Act
            model.onPlaceClick()
            advanceUntilIdle()

            // Assert
            verify(exactly = 0) { router.pop(onComplete = any()) }
            verify(exactly = 0) { router.popTo(route = any<PlacePredictionRoute>(), onComplete = any()) }
            assertThat(model.uiState.value.submit).isInstanceOf(SubmitUM.Result::class.java)

            model.onDestroy()
        }

        @Test
        fun `GIVEN the balance cannot be read WHEN place clicked THEN the user is told instead of nothing`() = runTest {
            // Arrange
            val amountModel = createAmountModel(testScope = this)
            advanceUntilIdle()
            amountModel.onResume()
            amountModel.onAmountChange(value = "10")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
            val model = createSummaryModel(testScope = this, amountModel = amountModel)
            coEvery { getBalanceInteractor(addresses = ADDRESSES) } returns PolymarketAuthError.KeyNotFound.left()
            amountModel.onPause()

            // Act
            model.onPlaceClick()
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.submit).isEqualTo(SubmitUM.Idle)
            verify { messageSender.send(any()) }
            coVerify(exactly = 0) { signOrderInteractor(request = any()) }

            model.onDestroy()
        }

        @Test
        fun `GIVEN a cheap quote but too few shares WHEN place clicked THEN the shares leg decides`() = runTest {
            // Arrange
            coEvery { getQuoteUseCase(request = any()) } returns
                createSellQuote(shares = BigDecimal("40")).copy(total = BigDecimal("0.5")).right()
            val amountModel = createAmountModel(testScope = this, params = SELL_PARAMS)
            advanceUntilIdle()
            amountModel.onResume()
            amountModel.onAmountChange(value = "40")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
            val model = createSummaryModel(testScope = this, amountModel = amountModel)
            coEvery { positionsRepository.getPosition(addresses = ADDRESSES, id = POSITION_ID) } returns
                createPosition(size = BigDecimal("39.99")).right()
            amountModel.onPause()

            // Act
            model.onPlaceClick()
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 0) { signOrderInteractor(request = any()) }
            coVerify(exactly = 0) { placeOrderUseCase(any(), any(), any()) }

            model.onDestroy()
        }

        @Test
        fun `GIVEN the market stopped accepting orders WHEN place clicked THEN nothing is signed AND the user is told`() =
            runTest {
                // Arrange
                coEvery { getQuoteUseCase(request = any()) } returns createSellQuote().right()
                val amountModel = createAmountModel(testScope = this, params = SELL_PARAMS)
                advanceUntilIdle()
                amountModel.onResume()
                amountModel.onAmountChange(value = "12.80")
                advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
                val model = createSummaryModel(testScope = this, amountModel = amountModel)
                coEvery { positionsRepository.getPosition(addresses = ADDRESSES, id = POSITION_ID) } returns
                    createPosition().copy(isAcceptingOrders = false).right()
                amountModel.onPause()

                // Act
                model.onPlaceClick()
                advanceUntilIdle()

                // Assert
                assertThat(model.uiState.value.submit).isEqualTo(SubmitUM.Idle)
                assertThat(model.uiState.value.notifications).contains(PredictionNotificationUM.MarketClosed)
                verify { messageSender.send(any()) }
                coVerify(exactly = 0) { signOrderInteractor(request = any()) }

                model.onDestroy()
            }
    }

    @Nested
    inner class FeeSheet {

        @Test
        fun `GIVEN a priced quote WHEN fee clicked THEN fee sheet opens with the breakdown`() = runTest {
            // Arrange
            val amountModel = createAmountModel(testScope = this)
            advanceUntilIdle()
            amountModel.onResume()
            amountModel.onAmountChange(value = "10")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
            val model = createSummaryModel(testScope = this, amountModel = amountModel)
            val slot = FakeSlotHost(model.feeSheetNavigation)

            val expected = PredictionFeeSheetUMConverter.convert(
                quote = createQuote(),
                slippage = SlippageUM.of(percent = DEFAULT_SLIPPAGE_PERCENT),
                tokenSymbol = PolymarketCollateralCurrencyFactory.COLLATERAL_SYMBOL,
            )

            // Act
            model.onFeeClick()

            // Assert
            assertThat(slot.configuration).isEqualTo(Unit)
            assertThat(model.feeSheetState.value).isEqualTo(expected)

            model.onDestroy()
        }

        @Test
        fun `GIVEN a submission in flight WHEN fee clicked THEN fee sheet stays closed`() = runTest {
            // Arrange
            val amountModel = createAmountModel(testScope = this)
            advanceUntilIdle()
            amountModel.onResume()
            amountModel.onAmountChange(value = "10")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
            val model = createSummaryModel(testScope = this, amountModel = amountModel)
            val slot = FakeSlotHost(model.feeSheetNavigation)
            coEvery { placeOrderUseCase(any(), any(), any()) } coAnswers {
                // Act
                model.onFeeClick()
                createPlaceResult().right()
            }

            // Act
            model.onPlaceClick()
            advanceUntilIdle()

            // Assert
            assertThat(slot.configuration).isNull()

            model.onDestroy()
        }

        @Test
        fun `GIVEN fee sheet open WHEN order submission starts THEN fee sheet closes`() = runTest {
            // Arrange
            val amountModel = createAmountModel(testScope = this)
            advanceUntilIdle()
            amountModel.onResume()
            amountModel.onAmountChange(value = "10")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
            val model = createSummaryModel(testScope = this, amountModel = amountModel)
            val slot = FakeSlotHost(model.feeSheetNavigation)
            model.onFeeClick()

            // Act
            model.onPlaceClick()
            advanceUntilIdle()

            // Assert
            assertThat(slot.configuration).isNull()

            model.onDestroy()
        }
    }

    @Nested
    inner class SellValidation {

        @Test
        fun `GIVEN a partial sale below the minimum order size WHEN quoted THEN it is priced AND signed`() =
            runTest {
                // Arrange
                coEvery { getQuoteUseCase(request = any()) } returns
                    createSellQuote(shares = BigDecimal("3")).copy(status = PredictionQuoteStatus.BELOW_MIN_ORDER_SIZE).right()
                val amountModel = createAmountModel(testScope = this, params = SELL_PARAMS)
                advanceUntilIdle()
                amountModel.onResume()
                amountModel.onAmountChange(value = "3")
                advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
                val model = createSummaryModel(testScope = this, amountModel = amountModel)

                // Act
                val quoted = model.uiState.value
                model.onPlaceClick()
                advanceUntilIdle()

                // Assert
                assertThat(quoted.isPrimaryButtonEnabled).isTrue()
                assertThat(quoted.market.outcomePriceCents).isEqualTo(42)
                coVerify(exactly = 1) { signOrderInteractor(request = any()) }

                model.onDestroy()
            }

        @Test
        fun `GIVEN a priced sell WHEN place clicked THEN the position is re-read AND the order is signed as a sell`() =
            runTest {
                // Arrange
                coEvery { getQuoteUseCase(request = any()) } returns createSellQuote().right()
                val amountModel = createAmountModel(testScope = this, params = SELL_PARAMS)
                advanceUntilIdle()
                amountModel.onResume()
                amountModel.onAmountChange(value = "12.80")
                advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
                val model = createSummaryModel(testScope = this, amountModel = amountModel)
                val request = slot<PredictionSignRequest>()
                coEvery { signOrderInteractor(request = capture(request)) } returns SIGNED_ORDER.right()

                clearMocks(positionsRepository, getBalanceInteractor, answers = false)

                // Act
                model.onPlaceClick()
                advanceUntilIdle()

                // Assert
                coVerify(exactly = 1) { positionsRepository.getPosition(addresses = ADDRESSES, id = POSITION_ID) }
                coVerify(exactly = 0) { getBalanceInteractor(addresses = any()) }
                assertThat(request.captured.side).isEqualTo(PredictionOrderSide.SELL)
                assertThat(request.captured.quote).isEqualTo(createSellQuote())

                model.onDestroy()
            }

        @Test
        fun `GIVEN the position shrank below the quoted shares WHEN place clicked THEN nothing is signed AND the user is told`() =
            runTest {
                // Arrange
                coEvery { getQuoteUseCase(request = any()) } returns createSellQuote(shares = BigDecimal("12.8")).right()
                val amountModel = createAmountModel(testScope = this, params = SELL_PARAMS)
                advanceUntilIdle()
                amountModel.onResume()
                amountModel.onAmountChange(value = "12.80")
                advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
                val model = createSummaryModel(testScope = this, amountModel = amountModel)
                coEvery { positionsRepository.getPosition(addresses = ADDRESSES, id = POSITION_ID) } returns
                    createPosition(size = BigDecimal("12.799")).right()
                amountModel.onPause()

                // Act
                model.onPlaceClick()
                advanceUntilIdle()

                // Assert
                assertThat(model.uiState.value.submit).isEqualTo(SubmitUM.Idle)
                assertThat((model.uiState.value.order as OrderSideUM.Sell).position)
                    .isEqualTo(SellPositionUM.Held(available = BigDecimal("12.79")))
                assertThat(model.uiState.value.amountValue).isEqualTo("12.80")
                assertThat(model.uiState.value.isPrimaryButtonEnabled).isFalse()
                verify { messageSender.send(any()) }
                coVerify(exactly = 0) { signOrderInteractor(request = any()) }

                model.onDestroy()
            }

        @Test
        fun `GIVEN the position is gone WHEN place clicked THEN nothing is signed AND the user is told`() = runTest {
            // Arrange
            coEvery { getQuoteUseCase(request = any()) } returns createSellQuote().right()
            val amountModel = createAmountModel(testScope = this, params = SELL_PARAMS)
            advanceUntilIdle()
            amountModel.onResume()
            amountModel.onAmountChange(value = "12.80")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
            val model = createSummaryModel(testScope = this, amountModel = amountModel)
            coEvery { positionsRepository.getPosition(addresses = ADDRESSES, id = POSITION_ID) } returns null.right()
            amountModel.onPause()

            // Act
            model.onPlaceClick()
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.submit).isEqualTo(SubmitUM.Idle)
            assertThat((model.uiState.value.order as OrderSideUM.Sell).position).isEqualTo(SellPositionUM.NotHeld)
            assertThat(model.uiState.value.isPrimaryButtonEnabled).isFalse()
            verify { messageSender.send(any()) }
            coVerify(exactly = 0) { signOrderInteractor(request = any()) }

            model.onDestroy()
        }

        @Test
        fun `GIVEN the position cannot be re-read WHEN place clicked THEN the user is told instead of nothing`() = runTest {
            // Arrange
            coEvery { getQuoteUseCase(request = any()) } returns createSellQuote().right()
            val amountModel = createAmountModel(testScope = this, params = SELL_PARAMS)
            advanceUntilIdle()
            amountModel.onResume()
            amountModel.onAmountChange(value = "12.80")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
            val model = createSummaryModel(testScope = this, amountModel = amountModel)
            coEvery { positionsRepository.getPosition(addresses = ADDRESSES, id = POSITION_ID) } returns
                PolymarketPositionsError.Network.left()
            amountModel.onPause()

            // Act
            model.onPlaceClick()
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.submit).isEqualTo(SubmitUM.Idle)
            verify { messageSender.send(any()) }
            coVerify(exactly = 0) { signOrderInteractor(request = any()) }

            model.onDestroy()
        }

        @Test
        fun `GIVEN a priced sell WHEN the position turns out to be gone THEN the stale proceeds go with it`() =
            runTest {
                // Arrange
                coEvery { getQuoteUseCase(request = any()) } returns createSellQuote().right()
                val amountModel = createAmountModel(testScope = this, params = SELL_PARAMS)
                advanceUntilIdle()
                amountModel.onResume()
                amountModel.onAmountChange(value = "12.80")
                advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
                val model = createSummaryModel(testScope = this, amountModel = amountModel)
                coEvery { positionsRepository.getPosition(addresses = ADDRESSES, id = POSITION_ID) } returns null.right()
                amountModel.onPause()

                // Act
                model.onPlaceClick()
                advanceUntilIdle()

                // Assert
                assertThat(model.uiState.value.quote).isEqualTo(QuoteUM.Empty)
                assertThat((model.uiState.value.order as OrderSideUM.Sell).proceeds)
                    .isEqualTo(SharesProceedsUM.Unavailable)

                model.onDestroy()
            }
    }
}