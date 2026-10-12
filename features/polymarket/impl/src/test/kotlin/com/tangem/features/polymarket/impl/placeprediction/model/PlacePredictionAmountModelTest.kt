package com.tangem.features.polymarket.impl.placeprediction.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PolymarketOnboardingError
import com.tangem.domain.polymarket.model.PolymarketPositionsError
import com.tangem.domain.polymarket.model.PolymarketAuthError
import com.tangem.domain.polymarket.model.PredictionOrderQuoteError
import com.tangem.domain.polymarket.model.PredictionOrderQuoteRequest
import com.tangem.domain.polymarket.model.PredictionOrderSide
import com.tangem.domain.polymarket.model.PredictionPriceError
import com.tangem.domain.polymarket.model.PredictionQuoteStatus
import com.tangem.features.polymarket.impl.placeprediction.PlacePredictionRoute
import com.tangem.features.polymarket.impl.placeprediction.entity.DEFAULT_SLIPPAGE_PERCENT
import com.tangem.features.polymarket.impl.placeprediction.entity.AmountCaptionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PaymentAmountErrorUM
import com.tangem.features.polymarket.impl.placeprediction.entity.OrderSideUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SellPositionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SharesProceedsUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PredictionNotificationUM
import com.tangem.features.polymarket.impl.placeprediction.entity.TradingPermissionUM
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
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlinx.coroutines.test.runCurrent


@OptIn(ExperimentalCoroutinesApi::class)
internal class PlacePredictionAmountModelTest : PlacePredictionModelTestBase() {

    @Nested
    inner class Quotes {

        @Test
        fun `GIVEN event loads WHEN model created THEN market header filled`() = runTest {
            // Act
            val model = createAmountModel(testScope = this)
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.market.title).isEqualTo("Market question")
            assertThat(model.uiState.value.market.outcomeTitle).isEqualTo("Yes")
            assertThat(model.uiState.value.market.outcomePriceCents).isEqualTo(42)
            assertThat((model.uiState.value.order as OrderSideUM.Buy).balance).isEqualTo(BigDecimal("100"))

            model.onDestroy()
        }

        @Test
        fun `GIVEN amount entered WHEN debounce elapses THEN quote requested once`() = runTest {
            // Arrange
            val model = createAmountModel(testScope = this)
            advanceUntilIdle()
            model.onResume()

            // Act
            model.onAmountChange(value = "1")
            model.onAmountChange(value = "10")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)

            // Assert
            coVerify(exactly = 1) {
                getQuoteUseCase(
                    request = PredictionOrderQuoteRequest(
                        marketId = MARKET_ID,
                        assetId = ASSET_ID,
                        side = PredictionOrderSide.BUY,
                        amount = BigDecimal("10"),
                        slippagePercent = DEFAULT_SLIPPAGE_PERCENT,
                    ),
                )
            }
            assertThat(model.uiState.value.quote).isInstanceOf(QuoteUM.Content::class.java)

            model.onDestroy()
        }

        @Test
        fun `GIVEN quote loaded WHEN poll interval elapses THEN quote requested again`() = runTest {
            // Arrange
            val model = createAmountModel(testScope = this)
            advanceUntilIdle()
            model.onResume()
            model.onAmountChange(value = "10")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)

            // Act
            advanceTimeBy(delayTimeMillis = QUOTE_POLL_INTERVAL_MILLIS + 1)

            // Assert
            coVerify(exactly = 2) { getQuoteUseCase(request = any()) }

            model.onDestroy()
        }

        @Test
        fun `GIVEN the market takes no orders WHEN an amount is typed THEN no quote is requested`() = runTest {
            // Arrange
            coEvery { getEventUseCase(eventId = any()) } returns createEvent(isAcceptingOrders = false).right()
            val model = createAmountModel(testScope = this)
            advanceUntilIdle()
            model.onResume()

            // Act
            model.onAmountChange(value = "10")
            advanceTimeBy(delayTimeMillis = QUOTE_POLL_INTERVAL_MILLIS * 3)

            // Assert
            coVerify(exactly = 0) { getQuoteUseCase(request = any()) }
            assertThat(model.uiState.value.notifications).contains(PredictionNotificationUM.MarketClosed)
            assertThat(model.uiState.value.isPrimaryButtonEnabled).isFalse()

            model.onDestroy()
        }

        @Test
        fun `GIVEN an amount typed before the market lands WHEN it takes no orders THEN no quote is requested`() =
            runTest {
                // Arrange
                coEvery { getEventUseCase(eventId = any()) } returns createEvent(isAcceptingOrders = false).right()
                val model = createAmountModel(testScope = this)
                model.onResume()

                // Act
                model.onAmountChange(value = "10")
                runCurrent()
                advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + QUOTE_POLL_INTERVAL_MILLIS * 3)

                // Assert
                coVerify(exactly = 0) { getQuoteUseCase(request = any()) }

                model.onDestroy()
            }

        @Test
        fun `GIVEN the market is closed WHEN poll interval elapses THEN quote NOT requested again`() = runTest {
            // Arrange
            val model = createAmountModel(testScope = this)
            advanceUntilIdle()
            model.onResume()
            coEvery { getQuoteUseCase(request = any()) } returns
                createQuote(status = PredictionQuoteStatus.MARKET_CLOSED).right()

            // Act
            model.onAmountChange(value = "10")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
            advanceTimeBy(delayTimeMillis = QUOTE_POLL_INTERVAL_MILLIS * 3)

            // Assert
            coVerify(exactly = 1) { getQuoteUseCase(request = any()) }
            assertThat(model.uiState.value.quote).isEqualTo(QuoteUM.Unavailable(PredictionQuoteStatus.MARKET_CLOSED))

            model.onDestroy()
        }

        @Test
        fun `GIVEN amount cleared WHEN state updated THEN quote is Empty AND no request made`() = runTest {
            // Arrange
            val model = createAmountModel(testScope = this)
            advanceUntilIdle()
            model.onResume()
            model.onAmountChange(value = "10")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)

            // Act
            model.onAmountChange(value = "")
            advanceTimeBy(delayTimeMillis = QUOTE_POLL_INTERVAL_MILLIS * 2)

            // Assert
            coVerify(exactly = 1) { getQuoteUseCase(request = any()) }
            assertThat(model.uiState.value.quote).isEqualTo(QuoteUM.Empty)
            assertThat(model.uiState.value.isPrimaryButtonEnabled).isFalse()

            model.onDestroy()
        }

        @Test
        fun `GIVEN balance below quote total WHEN quote loaded THEN button stays disabled`() = runTest {
            // Arrange
            coEvery { getQuoteUseCase(request = any()) } returns createQuote(total = BigDecimal("120")).right()
            val model = createAmountModel(testScope = this)
            advanceUntilIdle()
            model.onResume()

            // Act
            model.onAmountChange(value = "100")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)

            // Assert
            assertThat((model.uiState.value.order as OrderSideUM.Buy).hasSufficientBalance).isFalse()
            assertThat(model.uiState.value.isPrimaryButtonEnabled).isFalse()

            model.onDestroy()
        }

        @Test
        fun `GIVEN quote fails WHEN retry clicked THEN quote requested again AND content shown`() = runTest {
            // Arrange
            coEvery { getQuoteUseCase(request = any()) } returnsMany listOf(
                PredictionOrderQuoteError.Network.left(),
                createQuote().right(),
            )
            val model = createAmountModel(testScope = this)
            advanceUntilIdle()
            model.onResume()
            model.onAmountChange(value = "10")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)

            // Act
            assertThat(model.uiState.value.quote).isInstanceOf(QuoteUM.Error::class.java)
            model.onQuoteRetryClick()
            advanceTimeBy(delayTimeMillis = 1)

            // Assert
            assertThat(model.uiState.value.quote).isInstanceOf(QuoteUM.Content::class.java)

            model.onDestroy()
        }

        @Test
        fun `GIVEN balance unavailable WHEN model created THEN balance stays unknown AND button disabled`() = runTest {
            // Arrange
            coEvery { getBalanceInteractor(addresses = ADDRESSES) } returns PolymarketAuthError.KeyNotFound.left()

            // Act
            val model = createAmountModel(testScope = this)
            advanceUntilIdle()
            model.onResume()
            model.onAmountChange(value = "10")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)

            // Assert
            assertThat((model.uiState.value.order as OrderSideUM.Buy).balance).isNull()
            assertThat(model.uiState.value.collateral.tokenSymbol).isEqualTo("pUSD")
            assertThat(model.uiState.value.amountCaption).isNotInstanceOf(AmountCaptionUM.Shortfall::class.java)
            assertThat(model.uiState.value.isPrimaryButtonEnabled).isFalse()

            model.onDestroy()
        }

        @Test
        fun `GIVEN no liquidity WHEN quote loaded THEN figures are not shown AND button disabled`() = runTest {
            // Arrange
            coEvery { getQuoteUseCase(request = any()) } returns
                createQuote(status = PredictionQuoteStatus.INSUFFICIENT_LIQUIDITY).right()
            val model = createAmountModel(testScope = this)
            advanceUntilIdle()
            model.onResume()

            // Act
            model.onAmountChange(value = "10")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)

            // Assert
            assertThat(model.uiState.value.quote)
                .isEqualTo(QuoteUM.Unavailable(status = PredictionQuoteStatus.INSUFFICIENT_LIQUIDITY))
            assertThat(model.uiState.value.isPrimaryButtonEnabled).isFalse()

            model.onDestroy()
        }

        @Test
        fun `GIVEN below min order size WHEN quote loaded THEN figures are kept AND button disabled`() = runTest {
            // Arrange
            coEvery { getQuoteUseCase(request = any()) } returns
                createQuote(status = PredictionQuoteStatus.BELOW_MIN_ORDER_SIZE).right()
            val model = createAmountModel(testScope = this)
            advanceUntilIdle()
            model.onResume()

            // Act
            model.onAmountChange(value = "10")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)

            // Assert
            assertThat(model.uiState.value.quote).isInstanceOf(QuoteUM.Content::class.java)
            assertThat(model.uiState.value.isPrimaryButtonEnabled).isFalse()

            model.onDestroy()
        }

        @Test
        fun `GIVEN below min order amount WHEN quote loaded THEN figures are kept AND button disabled`() = runTest {
            // Arrange
            coEvery { getQuoteUseCase(request = any()) } returns
                createQuote(status = PredictionQuoteStatus.BELOW_MIN_ORDER_AMOUNT).right()
            val model = createAmountModel(testScope = this)
            advanceUntilIdle()
            model.onResume()

            // Act
            model.onAmountChange(value = "10")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)

            // Assert
            assertThat(model.uiState.value.quote).isInstanceOf(QuoteUM.Content::class.java)
            assertThat(model.uiState.value.isPrimaryButtonEnabled).isFalse()

            model.onDestroy()
        }

        @Test
        fun `GIVEN the first quote states a minimum the sum is below WHEN it arrives THEN the price is polled`() =
            runTest {
                // Arrange
                coEvery { getQuoteUseCase(request = any()) } returns
                    createQuote(status = PredictionQuoteStatus.BELOW_MIN_ORDER_SIZE).right()
                val model = createAmountModel(testScope = this)
                advanceUntilIdle()
                model.onResume()

                // Act
                model.onAmountChange(value = "1")
                advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
                advanceTimeBy(delayTimeMillis = QUOTE_POLL_INTERVAL_MILLIS + 1)

                // Assert
                coVerify(exactly = 1) { getQuoteUseCase(request = any()) }
                coVerify(atLeast = 1) { getPriceUseCase(assetId = ASSET_ID, side = PredictionOrderSide.SELL) }

                model.onDestroy()
            }

        @Test
        fun `GIVEN a stated minimum WHEN a smaller sum is entered THEN no quote is requested`() = runTest {
            // Arrange
            val model = createAmountModel(testScope = this)
            advanceUntilIdle()
            model.onResume()
            model.onAmountChange(value = "10")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)

            // Act
            model.onAmountChange(value = "1")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)

            // Assert
            coVerify(exactly = 1) { getQuoteUseCase(request = any()) }
            assertThat(model.uiState.value.quote).isEqualTo(QuoteUM.Empty)
            assertThat(model.uiState.value.amountCaption)
                .isEqualTo(AmountCaptionUM.BelowMinimum(minAmount = BigDecimal("2.15")))
            assertThat(model.uiState.value.isPrimaryButtonEnabled).isFalse()

            model.onDestroy()
        }

        @Test
        fun `GIVEN amount below one cent WHEN entered THEN quote is Empty AND no request made`() = runTest {
            // Arrange
            val model = createAmountModel(testScope = this)
            advanceUntilIdle()
            model.onResume()

            // Act
            model.onAmountChange(value = "0.001")
            advanceTimeBy(delayTimeMillis = QUOTE_POLL_INTERVAL_MILLIS)

            // Assert
            coVerify(exactly = 0) { getQuoteUseCase(request = any()) }
            assertThat(model.uiState.value.quote).isEqualTo(QuoteUM.Empty)

            model.onDestroy()
        }

        @Test
        fun `GIVEN quote loaded WHEN read THEN headline is the expectation`() = runTest {
            // Arrange
            val model = createAmountModel(testScope = this)
            advanceUntilIdle()
            model.onResume()

            // Act
            model.onAmountChange(value = "10")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)

            // Assert
            val quote = model.uiState.value.quote as QuoteUM.Content
            assertThat(quote.expected).isEqualTo(BigDecimal("23.8"))

            model.onDestroy()
        }

        @Test
        fun `GIVEN liquidity disappears WHEN next tick arrives THEN button is gated on the latest quote`() = runTest {
            // Arrange
            coEvery { getQuoteUseCase(request = any()) } returnsMany listOf(
                createQuote().right(),
                createQuote(status = PredictionQuoteStatus.INSUFFICIENT_LIQUIDITY).right(),
            )
            val model = createAmountModel(testScope = this)
            advanceUntilIdle()
            model.onResume()
            model.onAmountChange(value = "10")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
            assertThat(model.uiState.value.isPrimaryButtonEnabled).isTrue()

            // Act
            advanceTimeBy(delayTimeMillis = QUOTE_POLL_INTERVAL_MILLIS + 1)

            // Assert
            assertThat(model.uiState.value.quote).isInstanceOf(QuoteUM.Unavailable::class.java)
            assertThat(model.uiState.value.isPrimaryButtonEnabled).isFalse()

            model.onDestroy()
        }

        @Test
        fun `GIVEN the total exceeds the balance WHEN quote loaded THEN the shortfall is named under the amount`() = runTest {
            // Arrange
            coEvery { getQuoteUseCase(request = any()) } returns createQuote(total = BigDecimal("120")).right()
            val model = createAmountModel(testScope = this)
            advanceUntilIdle()
            model.onResume()

            // Act
            model.onAmountChange(value = "100")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)

            // Assert
            assertThat(model.uiState.value.amountCaption)
                .isEqualTo(AmountCaptionUM.Shortfall(error = PaymentAmountErrorUM.NotEnoughForFees))
            assertThat(model.uiState.value.isPrimaryButtonEnabled).isFalse()

            model.onDestroy()
        }

        @Test
        fun `GIVEN the screen is hidden WHEN interval elapses THEN no quote is requested`() = runTest {
            // Arrange
            val model = createAmountModel(testScope = this)
            advanceUntilIdle()
            model.onResume()
            model.onAmountChange(value = "10")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)

            // Act
            model.onPause()
            advanceTimeBy(delayTimeMillis = QUOTE_POLL_INTERVAL_MILLIS * 3)

            // Assert
            coVerify(exactly = 1) { getQuoteUseCase(request = any()) }

            model.onDestroy()
        }

        @Test
        fun `GIVEN a priced quote WHEN the amount step is left THEN the quote is not refreshed`() = runTest {
            // Arrange
            val model = createAmountModel(testScope = this)
            advanceUntilIdle()
            model.onResume()
            model.onAmountChange(value = "10")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
            val shown = model.uiState.value.quote

            // Act
            model.onPause()
            advanceTimeBy(delayTimeMillis = QUOTE_POLL_INTERVAL_MILLIS * 3)

            // Assert
            assertThat(model.uiState.value.quote).isEqualTo(shown)
            coVerify(exactly = 1) { getQuoteUseCase(request = any()) }

            model.onDestroy()
        }

        @Test
        fun `GIVEN a refresh in flight WHEN the amount step is left THEN the figures shown stay AND the answer never lands`() =
            runTest {
                // Arrange
                coEvery { getQuoteUseCase(request = any()) } returns createQuote().right() coAndThen {
                    delay(timeMillis = 1_000)
                    createQuote(total = BigDecimal("99")).right()
                }
                val model = createAmountModel(testScope = this)
                advanceUntilIdle()
                model.onResume()
                model.onAmountChange(value = "10")
                advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
                val shown = model.uiState.value.quote
                advanceTimeBy(delayTimeMillis = QUOTE_POLL_INTERVAL_MILLIS + 1)

                // Act
                model.onPause()
                advanceUntilIdle()

                // Assert
                assertThat(model.uiState.value.quote).isEqualTo(shown)
                coVerify(exactly = 2) { getQuoteUseCase(request = any()) }

                model.onDestroy()
            }

        @Test
        fun `GIVEN the amount step was left WHEN it is shown again THEN the quote refreshes again at once`() = runTest {
            // Arrange
            val model = createAmountModel(testScope = this)
            advanceUntilIdle()
            model.onResume()
            model.onAmountChange(value = "10")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
            model.onPause()
            advanceTimeBy(delayTimeMillis = QUOTE_POLL_INTERVAL_MILLIS * 3)

            // Act
            model.onResume()
            advanceTimeBy(delayTimeMillis = 1)

            // Assert
            coVerify(exactly = 2) { getQuoteUseCase(request = any()) }

            model.onDestroy()
        }

        @Test
        fun `GIVEN empty amount WHEN screen shown THEN book price polls into the header`() = runTest {
            // Arrange
            val model = createAmountModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.onResume()
            advanceTimeBy(delayTimeMillis = 1)

            // Assert
            assertThat(model.uiState.value.market.outcomePriceCents).isEqualTo(48)
            coVerify(exactly = 1) { getPriceUseCase(assetId = ASSET_ID, side = PredictionOrderSide.SELL) }

            advanceTimeBy(delayTimeMillis = QUOTE_POLL_INTERVAL_MILLIS + 1)
            coVerify(exactly = 2) { getPriceUseCase(assetId = ASSET_ID, side = PredictionOrderSide.SELL) }

            model.onDestroy()
        }

        @Test
        fun `GIVEN closed market WHEN price polled THEN market closed banner AND polling stops`() = runTest {
            // Arrange
            coEvery { getPriceUseCase(assetId = any(), side = any()) } returns PredictionPriceError.MarketClosed.left()
            val model = createAmountModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.onResume()
            advanceTimeBy(delayTimeMillis = QUOTE_POLL_INTERVAL_MILLIS * 2)

            // Assert
            val state = model.uiState.value
            assertThat(state.notifications).contains(PredictionNotificationUM.MarketClosed)
            assertThat(state.market.isPriceUnavailable).isTrue()
            coVerify(exactly = 1) { getPriceUseCase(assetId = any(), side = any()) }

            model.onDestroy()
        }

        @Test
        fun `GIVEN transport failure WHEN price polled THEN last price kept AND polling continues`() = runTest {
            // Arrange
            coEvery { getPriceUseCase(assetId = any(), side = any()) } returnsMany listOf(
                BigDecimal("0.48").right(),
                PredictionPriceError.Network.left(),
                BigDecimal("0.52").right(),
            )
            val model = createAmountModel(testScope = this)
            advanceUntilIdle()
            model.onResume()
            advanceTimeBy(delayTimeMillis = 1)

            // Act
            advanceTimeBy(delayTimeMillis = QUOTE_POLL_INTERVAL_MILLIS + 1)

            // Assert
            assertThat(model.uiState.value.market.outcomePriceCents).isEqualTo(48)
            assertThat(model.uiState.value.notifications).isEmpty()

            advanceTimeBy(delayTimeMillis = QUOTE_POLL_INTERVAL_MILLIS + 1)
            assertThat(model.uiState.value.market.outcomePriceCents).isEqualTo(52)

            model.onDestroy()
        }

        @Test
        fun `GIVEN amount entered and cleared WHEN polling THEN price loop yields to quote loop and resumes`() = runTest {
            // Arrange
            coEvery { getQuoteUseCase(request = any()) } returnsMany listOf(
                createQuote().copy(averagePrice = BigDecimal("0.61")).right(),
                createQuote().copy(status = PredictionQuoteStatus.PARTIAL, averagePrice = BigDecimal("0.64")).right(),
            )
            val model = createAmountModel(testScope = this)
            advanceUntilIdle()
            model.onResume()
            advanceTimeBy(delayTimeMillis = 1)

            // Act
            model.onAmountChange(value = "10")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
            assertThat(model.uiState.value.market.outcomePriceCents).isEqualTo(61)

            advanceTimeBy(delayTimeMillis = QUOTE_POLL_INTERVAL_MILLIS + 1)
            assertThat(model.uiState.value.market.outcomePriceCents).isEqualTo(64)

            // Assert
            coVerify(exactly = 1) { getPriceUseCase(assetId = any(), side = any()) }

            model.onAmountChange(value = "")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
            coVerify(exactly = 2) { getPriceUseCase(assetId = any(), side = any()) }
            assertThat(model.uiState.value.market.outcomePriceCents).isEqualTo(48)

            model.onDestroy()
        }

        @Test
        fun `GIVEN a priced quote WHEN refresh fails or liquidity is unavailable THEN header keeps the last price`() = runTest {
            coEvery { getQuoteUseCase(request = any()) } returnsMany listOf(
                createQuote().copy(averagePrice = BigDecimal("0.61")).right(),
                PredictionOrderQuoteError.Network.left(),
                createQuote().copy(
                    status = PredictionQuoteStatus.INSUFFICIENT_LIQUIDITY,
                    averagePrice = BigDecimal.ZERO,
                ).right(),
            )
            val model = createAmountModel(testScope = this)
            advanceUntilIdle()
            model.onResume()
            model.onAmountChange(value = "10")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)

            advanceTimeBy(delayTimeMillis = QUOTE_POLL_INTERVAL_MILLIS + 1)
            assertThat(model.uiState.value.market.outcomePriceCents).isEqualTo(61)

            advanceTimeBy(delayTimeMillis = QUOTE_POLL_INTERVAL_MILLIS + 1)
            assertThat(model.uiState.value.market.outcomePriceCents).isEqualTo(61)

            model.onDestroy()
        }
    }

    @Nested
    inner class Navigation {

        @Test
        fun `GIVEN slippage confirmed in the sheet WHEN applied THEN quote uses it immediately`() = runTest {
            // Arrange
            val model = createAmountModel(testScope = this)
            advanceUntilIdle()
            model.onAmountChange(value = "10")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)

            // Act
            val route = slot<PlacePredictionRoute.Slippage>()
            every { router.push(capture(route), any()) } returns Unit
            model.onSlippageClick()
            model.onPause()
            route.captured.onConfirm(BigDecimal("1.5"))
            model.onResume()
            advanceTimeBy(delayTimeMillis = 1)

            // Assert
            coVerify(exactly = 1) {
                getQuoteUseCase(request = match { it.slippagePercent == BigDecimal("1.5") })
            }
            assertThat(model.uiState.value.slippage.isDefault).isFalse()

            model.onDestroy()
        }

        @Test
        fun `GIVEN a priced amount WHEN next clicked THEN summary receives the accepted order`() = runTest {
            // Arrange
            val model = createAmountModel(testScope = this)
            advanceUntilIdle()
            model.onResume()
            model.onAmountChange(value = "10")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
            val state = model.uiState.value
            val route = slot<PlacePredictionRoute.Summary>()
            every { router.push(capture(route), any()) } returns Unit

            // Act
            model.onNextClick()

            // Assert
            assertThat(route.captured.order.state).isEqualTo(state)
            assertThat(route.captured.order.quote).isEqualTo((state.quote as QuoteUM.Content).signable)

            model.onDestroy()
        }

        @Test
        fun `GIVEN the amount screen WHEN close clicked THEN stack is emptied AND flow popped`() = runTest {
            // Arrange
            val model = createAmountModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.onCloseClick()

            // Assert
            verify { router.popTo(route = PlacePredictionRoute.Amount, onComplete = any()) }
            verify { router.pop(onComplete = any()) }

            model.onDestroy()
        }
    }

    @Nested
    inner class TradingAllowance {

        @Test
        fun `GIVEN the region cannot be read WHEN the screen opens THEN no restriction is claimed`() = runTest {
            // Arrange
            coEvery { checkGeoblockUseCase() } returns PolymarketOnboardingError.Network.left()

            // Act
            val model = createAmountModel(testScope = this)
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.tradingPermission).isEqualTo(TradingPermissionUM.Unknown)
            assertThat(model.uiState.value.notifications).doesNotContain(PredictionNotificationUM.RegionRestricted)
            model.onDestroy()
        }

        @Test
        fun `GIVEN the region could not be read WHEN the screen is shown again THEN it is read again`() = runTest {
            // Arrange
            coEvery { checkGeoblockUseCase() } returns PolymarketOnboardingError.Network.left()
            val model = createAmountModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.onResume()
            runCurrent()

            // Assert
            coVerify(exactly = 2) { checkGeoblockUseCase() }
            model.onDestroy()
        }

        @Test
        fun `GIVEN the region was read WHEN the screen is shown again THEN it is not read again`() = runTest {
            // Arrange
            val model = createAmountModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.onResume()
            runCurrent()

            // Assert
            coVerify(exactly = 1) { checkGeoblockUseCase() }
            model.onDestroy()
        }
    }

    @Nested
    inner class PositionReads {

        @Test
        fun `GIVEN sell params WHEN model created THEN the position fills the screen AND no event is read`() = runTest {
            // Act
            val model = createAmountModel(testScope = this, params = SELL_PARAMS)
            advanceUntilIdle()

            // Assert
            val order = model.uiState.value.order as OrderSideUM.Sell
            assertThat(order.position).isEqualTo(SellPositionUM.Held(available = BigDecimal("51.23")))
            assertThat(order.balance).isEqualTo(BigDecimal("100"))
            assertThat(model.uiState.value.market.title).isEqualTo("Will the event happen?")
            assertThat(model.uiState.value.market.outcomeTitle).isEqualTo("Yes")
            assertThat(model.uiState.value.market.entryPriceCents).isEqualTo(35)
            assertThat(model.uiState.value.collateral.tokenSymbol).isEqualTo("pUSD")
            coVerify(exactly = 0) { getEventUseCase(eventId = any()) }

            model.onDestroy()
        }

        @Test
        fun `GIVEN a position too small to quote WHEN screen shown THEN the live price is the best bid`() = runTest {
            // Arrange
            coEvery { positionsRepository.getPosition(addresses = any(), id = any()) } returns
                createPosition(size = BigDecimal("0.005")).right()
            val model = createAmountModel(testScope = this, params = SELL_PARAMS)
            advanceUntilIdle()

            // Act
            model.onResume()
            runCurrent()

            // Assert
            coVerify(exactly = 1) { getPriceUseCase(assetId = ASSET_ID, side = PredictionOrderSide.BUY) }

            model.onDestroy()
        }

        @Test
        fun `GIVEN a held position WHEN model created THEN the whole position is selected`() = runTest {
            // Act
            val model = createAmountModel(testScope = this, params = SELL_PARAMS)
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.amountValue).isEqualTo("51.23")

            model.onDestroy()
        }

        @Test
        fun `GIVEN shares selected WHEN debounce elapses THEN a sell quote is requested for them`() = runTest {
            // Arrange
            coEvery { getQuoteUseCase(request = any()) } returns createSellQuote().right()
            val model = createAmountModel(testScope = this, params = SELL_PARAMS)
            advanceUntilIdle()
            model.onResume()

            // Act
            model.onAmountChange(value = "12.80")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)

            // Assert
            coVerify(exactly = 1) {
                getQuoteUseCase(
                    request = PredictionOrderQuoteRequest(
                        marketId = MARKET_ID,
                        assetId = ASSET_ID,
                        side = PredictionOrderSide.SELL,
                        amount = BigDecimal("12.80"),
                        slippagePercent = model.uiState.value.slippage.percent,
                    ),
                )
            }
            val order = model.uiState.value.order as OrderSideUM.Sell
            assertThat(order.proceeds).isEqualTo(
                SharesProceedsUM.Content(expected = BigDecimal("11.01")),
            )
            assertThat(model.uiState.value.isPrimaryButtonEnabled).isTrue()

            model.onDestroy()
        }

        @Test
        fun `GIVEN the position cannot be read WHEN model created THEN nothing is sellable`() = runTest {
            // Arrange
            coEvery { positionsRepository.getPosition(addresses = ADDRESSES, id = POSITION_ID) } returns null.right()

            // Act
            val model = createAmountModel(testScope = this, params = SELL_PARAMS)
            advanceUntilIdle()
            model.onResume()
            model.onAmountChange(value = "1")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)

            // Assert
            assertThat((model.uiState.value.order as OrderSideUM.Sell).position).isEqualTo(SellPositionUM.NotHeld)
            assertThat(model.uiState.value.isPrimaryButtonEnabled).isFalse()
            assertThat(model.uiState.value.notifications)
                .contains(PredictionNotificationUM.PositionUnavailable)

            model.onDestroy()
        }

        @Test
        fun `GIVEN an unreadable position WHEN the screen is shown again THEN the position is read again`() = runTest {
            // Arrange
            coEvery { positionsRepository.getPosition(addresses = ADDRESSES, id = POSITION_ID) } returns
                PolymarketPositionsError.Network.left()
            val model = createAmountModel(testScope = this, params = SELL_PARAMS)
            advanceUntilIdle()
            coEvery { positionsRepository.getPosition(addresses = ADDRESSES, id = POSITION_ID) } returns
                createPosition().right()

            // Act
            model.onResume()
            runCurrent()
            model.onPause()

            // Assert
            val order = model.uiState.value.order as OrderSideUM.Sell
            assertThat(order.position).isEqualTo(SellPositionUM.Held(available = BigDecimal("51.23")))
            assertThat(model.uiState.value.notifications)
                .doesNotContain(PredictionNotificationUM.PositionUnavailable)

            model.onDestroy()
        }

        @Test
        fun `GIVEN an unreadable position WHEN retry clicked THEN the position is read again`() = runTest {
            // Arrange
            coEvery { positionsRepository.getPosition(addresses = ADDRESSES, id = POSITION_ID) } returns
                PolymarketPositionsError.Network.left()
            val model = createAmountModel(testScope = this, params = SELL_PARAMS)
            advanceUntilIdle()
            coEvery { positionsRepository.getPosition(addresses = ADDRESSES, id = POSITION_ID) } returns
                createPosition().right()

            // Act
            model.onQuoteRetryClick()
            advanceUntilIdle()

            // Assert
            assertThat((model.uiState.value.order as OrderSideUM.Sell).position)
                .isEqualTo(SellPositionUM.Held(available = BigDecimal("51.23")))

            model.onDestroy()
        }

        @Test
        fun `GIVEN the position cannot be read WHEN screen shown THEN no price is polled`() = runTest {
            // Arrange
            coEvery { positionsRepository.getPosition(addresses = ADDRESSES, id = POSITION_ID) } returns
                PolymarketPositionsError.Network.left()
            val model = createAmountModel(testScope = this, params = SELL_PARAMS)
            advanceUntilIdle()

            // Act
            model.onResume()
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)

            // Assert
            coVerify(exactly = 0) { getPriceUseCase(assetId = any(), side = any()) }
            coVerify(exactly = 0) { getQuoteUseCase(request = any()) }
            assertThat(model.uiState.value.notifications)
                .contains(PredictionNotificationUM.PositionUnavailable)

            model.onDestroy()
        }

        @Test
        fun `GIVEN no stored addresses WHEN model created THEN the user is told instead of nothing`() = runTest {
            // Arrange
            coEvery { deriveAddressesUseCase.stored(userWalletId = USER_WALLET_ID) } returns null

            // Act
            val model = createAmountModel(testScope = this, params = SELL_PARAMS)
            advanceUntilIdle()
            model.onResume()
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)

            // Assert
            assertThat(model.uiState.value.notifications)
                .contains(PredictionNotificationUM.PositionUnavailable)
            assertThat(model.uiState.value.isPrimaryButtonEnabled).isFalse()
            coVerify(exactly = 0) { getPriceUseCase(assetId = any(), side = any()) }

            model.onDestroy()
        }
    }
}