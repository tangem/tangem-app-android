package com.tangem.features.polymarket.impl.placeprediction.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.Model
import com.tangem.domain.polymarket.model.PolymarketSigningError
import com.tangem.domain.polymarket.model.PredictionOrderFailure
import com.tangem.domain.polymarket.model.PredictionOrderFees
import com.tangem.domain.models.StatusSource
import com.tangem.features.polymarket.impl.common.FakeSlotHost
import com.tangem.features.polymarket.impl.placeprediction.PlacePredictionRoute
import com.tangem.features.polymarket.impl.placeprediction.entity.QuoteUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SubmitUM
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
internal class PlacePredictionOrderModelTest : PlacePredictionModelTestBase() {

    @Nested
    inner class Place {

        @Test
        fun `GIVEN a priced quote WHEN place clicked THEN the status step replaces the stack`() = runOrderTest {
            // Arrange
            val model = createQuotedModel()

            // Act
            model.onPlaceClick()
            runCurrent()

            // Assert
            coVerify(exactly = 1) { signOrderInteractor(request = any()) }
            verify(exactly = 1) {
                router.replaceAll(match<PlacePredictionRoute.Status> { it.side == BUY_PARAMS.side }, onComplete = any())
            }
        }

        @Test
        fun `GIVEN the screen leaves on success WHEN the order is placed THEN no quote is requested after it`() =
            runOrderTest {
                // Arrange
                val dispatcher = UnconfinedTestDispatcher(scheduler = testScheduler)
                val dispatchers = TestingCoroutineDispatcherProvider(
                    main = dispatcher,
                    mainImmediate = dispatcher,
                    io = dispatcher,
                    default = dispatcher,
                    single = dispatcher,
                )
                val model = createOrderModel(testScope = this, dispatchers = dispatchers)
                every { router.replaceAll(any(), onComplete = any()) } answers {
                    model.onPause()
                    model.onDestroy()
                }
                advanceUntilIdle()
                model.onResume()
                model.onAmountChange(value = "10")
                advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)

                // Act
                model.onPlaceClick()
                advanceTimeBy(delayTimeMillis = QUOTE_POLL_INTERVAL_MILLIS * 3)

                // Assert
                verify(exactly = 1) { router.replaceAll(any(), onComplete = any()) }
                coVerify(exactly = 1) { getQuoteUseCase(request = any()) }
            }

        @Test
        fun `GIVEN the quote is not ready WHEN place clicked THEN nothing is signed`() = runOrderTest {
            // Arrange
            val model = createOrderModel(testScope = this)
            advanceUntilIdle()
            model.onResume()
            model.onAmountChange(value = "10")

            // Act
            model.onPlaceClick()
            runCurrent()

            // Assert
            assertThat(model.uiState.value.submit).isEqualTo(SubmitUM.Idle)
            coVerify(exactly = 0) { signOrderInteractor(request = any()) }
            verify(exactly = 0) { router.replaceAll(any(), onComplete = any()) }
        }

        @Test
        fun `GIVEN signing is in flight WHEN place clicked again THEN the order is signed once`() = runOrderTest {
            // Arrange
            coEvery { signOrderInteractor(request = any()) } coAnswers {
                delay(timeMillis = SIGNING_MILLIS)
                SIGNED_ORDER.right()
            }
            val model = createQuotedModel()

            // Act
            model.onPlaceClick()
            model.onPlaceClick()
            advanceTimeBy(delayTimeMillis = SIGNING_MILLIS + 1)

            // Assert
            coVerify(exactly = 1) { signOrderInteractor(request = any()) }
            coVerify(exactly = 1) { placeOrderUseCase(any(), any(), any()) }
        }

        @Test
        fun `GIVEN place clicked WHEN the click returns THEN signing has already started`() = runOrderTest {
            // Arrange
            coEvery { signOrderInteractor(request = any()) } coAnswers {
                delay(timeMillis = SIGNING_MILLIS)
                SIGNED_ORDER.right()
            }
            val model = createQuotedModel()

            // Act
            model.onPlaceClick()

            // Assert
            assertThat(model.uiState.value.submit).isEqualTo(SubmitUM.Signing)
            assertThat(model.isSubmitting()).isTrue()
        }

        @Test
        fun `GIVEN the signature is declined WHEN place clicked THEN the quote loop resumes`() = runOrderTest {
            // Arrange
            coEvery { signOrderInteractor(request = any()) } returns
                PredictionOrderFailure.Signing(error = PolymarketSigningError.UserCancelled).left()
            val model = createQuotedModel()

            // Act
            model.onPlaceClick()
            runCurrent()

            // Assert
            assertThat(model.uiState.value.submit).isEqualTo(SubmitUM.Idle)
            coVerify(exactly = 2) { getQuoteUseCase(request = any()) }
            verify(exactly = 0) { router.replaceAll(any(), onComplete = any()) }
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(FrozenInput::class)
        fun `GIVEN signing is in flight WHEN an input is used THEN the order stays as it was`(input: FrozenInput) =
            runOrderTest {
                // Arrange
                coEvery { signOrderInteractor(request = any()) } coAnswers {
                    delay(timeMillis = SIGNING_MILLIS)
                    SIGNED_ORDER.right()
                }
                val model = createQuotedModel()
                val fundingSheet = FakeSlotHost(model.fundingSheetNavigation)
                model.onPlaceClick()

                // Act
                input.apply(model)
                runCurrent()

                // Assert
                assertThat(model.uiState.value.amountValue).isEqualTo("10")
                assertThat(fundingSheet.configuration).isNull()
                verify(exactly = 0) { router.push(any(), any()) }
                verify(exactly = 0) { messageSender.send(any()) }
                coVerify(exactly = 1) { getQuoteUseCase(request = any()) }
            }

        @Test
        fun `GIVEN signing is in flight WHEN close clicked THEN the flow stays`() = runOrderTest {
            // Arrange
            coEvery { signOrderInteractor(request = any()) } coAnswers {
                delay(timeMillis = SIGNING_MILLIS)
                SIGNED_ORDER.right()
            }
            val model = createQuotedModel()
            model.onPlaceClick()

            // Act
            model.onCloseClick()
            model.onBackClick()

            // Assert
            verify(exactly = 0) { router.pop(any()) }
        }
    }

    @Nested
    inner class RepeatedAmount {

        @Test
        fun `GIVEN a priced amount WHEN the same amount is entered again THEN the quote stays priced`() =
            runOrderTest {
                // Arrange
                val model = createQuotedModel()

                // Act
                model.onAmountChange(value = "10")

                // Assert
                assertThat(model.uiState.value.quote).isInstanceOf(QuoteUM.Content::class.java)
                advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
                coVerify(exactly = 1) { getQuoteUseCase(request = any()) }
            }

        @Test
        fun `GIVEN max is priced WHEN max clicked again THEN the quote stays priced`() = runOrderTest {
            // Arrange
            val model = createQuotedModel()
            model.onMaxClick()
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)

            // Act
            model.onMaxClick()

            // Assert
            assertThat(model.uiState.value.amountValue).isEqualTo("90")
            assertThat(model.uiState.value.quote).isInstanceOf(QuoteUM.Content::class.java)
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
            coVerify(exactly = 2) { getQuoteUseCase(request = any()) }
        }
    }

    @Nested
    inner class Presets {

        @Test
        fun `GIVEN a buy WHEN presets are read THEN they are 100, 500, 1000 and 5000`() = runOrderTest {
            // Arrange
            val model = createOrderModel(testScope = this)

            // Act
            val amounts = model.amountPresets.map { it.amount }

            // Assert
            assertThat(amounts).containsExactly("100", "500", "1000", "5000").inOrder()
        }

        @Test
        fun `GIVEN a sell WHEN presets are read THEN there are none`() = runOrderTest {
            // Arrange
            val model = createOrderModel(testScope = this, params = SELL_PARAMS)

            // Act
            val presets = model.amountPresets

            // Assert
            assertThat(presets).isEmpty()
        }

        @Test
        fun `GIVEN a buy WHEN a preset clicked THEN its amount is entered`() = runOrderTest {
            // Arrange
            val model = createOrderModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.onPresetClick(preset = model.amountPresets[2])

            // Assert
            assertThat(model.uiState.value.amountValue).isEqualTo("1000")
        }
    }

    @Nested
    inner class Max {

        @ParameterizedTest(name = "balance {0} without a quote enters {1}")
        @CsvSource(
            "100, 90",
            "10.019, 9.01",
            "0.019, 0.01",
        )
        fun `GIVEN no quote WHEN max clicked THEN nine tenths of the balance floored to cents is entered`(
            balance: String,
            expected: String,
        ) = runOrderTest {
            // Arrange
            statusFlow.value = activeStatus(balance = BigDecimal(balance))
            val model = createOrderModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.onMaxClick()

            // Assert
            assertThat(model.uiState.value.amountValue).isEqualTo(expected)
        }

        @ParameterizedTest(name = "fees {0} on notional {1} enter {2}")
        @CsvSource(
            "0.4, 10, 90",
            "20, 100, 83.33",
            "1.2, 10, 89.28",
            "0, 10, 90",
        )
        fun `GIVEN a quote WHEN max clicked THEN the lower of the balance cap and the fee cap is entered`(
            feeTotal: String,
            notional: String,
            expected: String,
        ) = runOrderTest {
            // Arrange
            coEvery { getQuoteUseCase(request = any()) } returns createQuote().copy(
                notional = BigDecimal(notional),
                fees = PredictionOrderFees(
                    market = BigDecimal(feeTotal),
                    builder = BigDecimal.ZERO,
                    total = BigDecimal(feeTotal),
                ),
            ).right()
            val model = createQuotedModel()

            // Act
            model.onMaxClick()

            // Assert
            assertThat(model.uiState.value.amountValue).isEqualTo(expected)
        }

        @Test
        fun `GIVEN a balance below a cent after the cap WHEN max clicked THEN nothing is entered`() = runOrderTest {
            // Arrange
            statusFlow.value = activeStatus(balance = BigDecimal("0.011"))
            val model = createOrderModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.onMaxClick()

            // Assert
            assertThat(model.uiState.value.amountValue).isEmpty()
        }

        @Test
        fun `GIVEN the balance is not confirmed WHEN max clicked THEN nothing is entered`() = runOrderTest {
            // Arrange
            statusFlow.value = activeStatus(source = StatusSource.CACHE)
            val model = createOrderModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.onMaxClick()

            // Assert
            assertThat(model.uiState.value.amountValue).isEmpty()
        }

        @Test
        fun `GIVEN a held position WHEN max clicked THEN the whole position is entered`() = runOrderTest {
            // Arrange
            coEvery { getQuoteUseCase(request = any()) } returns createSellQuote().right()
            val model = createOrderModel(testScope = this, params = SELL_PARAMS)
            advanceUntilIdle()

            // Act
            model.onMaxClick()

            // Assert
            assertThat(model.uiState.value.amountValue).isEqualTo("51.234567")
        }
    }

    @Nested
    inner class Lifecycle {

        @Test
        fun `GIVEN a model WHEN resumed THEN the price is polled until paused`() = runOrderTest {
            // Arrange
            val model = createOrderModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.onResume()
            runCurrent()
            model.onPause()
            advanceTimeBy(delayTimeMillis = QUOTE_POLL_INTERVAL_MILLIS * 3)

            // Assert
            coVerify(exactly = 1) { getPriceUseCase(assetId = any(), side = any()) }
        }

        @Test
        fun `GIVEN a model WHEN not resumed THEN nothing is polled`() = runOrderTest {
            // Arrange
            val model = createOrderModel(testScope = this)

            // Act
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 0) { getPriceUseCase(assetId = any(), side = any()) }
            assertThat(model.isSubmitting()).isFalse()
        }
    }

    private fun runOrderTest(block: suspend TestScope.() -> Unit) = runTest {
        try {
            block()
        } finally {
            models.forEach(Model::onDestroy)
        }
    }

    private fun TestScope.createQuotedModel(): PlacePredictionOrderModel {
        val model = createOrderModel(testScope = this)
        advanceUntilIdle()
        model.onResume()
        model.onAmountChange(value = "10")
        advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
        return model
    }

    enum class FrozenInput(val apply: (PlacePredictionOrderModel) -> Unit) {
        AmountChange(apply = { it.onAmountChange(value = "25") }),
        Preset(apply = { it.onPresetClick(preset = it.amountPresets.first()) }),
        Max(apply = PlacePredictionOrderModel::onMaxClick),
        Slippage(apply = PlacePredictionOrderModel::onSlippageClick),
        AddFunds(apply = PlacePredictionOrderModel::onAddFundsClick),
        Swap(apply = PlacePredictionOrderModel::onSwapClick),
        Receive(apply = PlacePredictionOrderModel::onReceiveClick),
    }

    private companion object {
        const val SIGNING_MILLIS = 1_000L
    }
}