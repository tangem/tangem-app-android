package com.tangem.features.polymarket.impl.crypto.model

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketCryptoPriceFeed
import com.tangem.domain.polymarket.model.PolymarketCryptoPricePoint
import com.tangem.domain.polymarket.model.PolymarketCryptoPriceSeries
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketEventError
import com.tangem.domain.polymarket.model.PolymarketLiveQuote
import com.tangem.domain.polymarket.model.PolymarketOutcome
import com.tangem.domain.polymarket.usecase.GetPolymarketEventUseCase
import com.tangem.domain.polymarket.usecase.ObservePolymarketCryptoPricesUseCase
import com.tangem.domain.polymarket.usecase.ObservePolymarketLiveQuotesUseCase
import com.tangem.features.polymarket.impl.createPolymarketEvent
import com.tangem.features.polymarket.impl.createPolymarketOutcome
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
internal class PolymarketCryptoEventControllerTest {

    private val getEvent: GetPolymarketEventUseCase = mockk()
    private val priceFlow = MutableStateFlow(PolymarketCryptoPriceSeries())
    private val observePrices: ObservePolymarketCryptoPricesUseCase = mockk {
        every { this@mockk(any(), any()) } returns priceFlow
    }
    private val quoteFlow = MutableStateFlow(emptyMap<String, PolymarketLiveQuote>())
    private val observeQuotes: ObservePolymarketLiveQuotesUseCase = mockk {
        every { this@mockk(any()) } returns quoteFlow
    }
    private val readIds = mutableListOf<String>()
    private var reports = 0

    /** Where the backend's clock stands when a test begins. */
    private var startMillis = duringCryptoWindow(0)

    @Nested
    inner class Load {

        @Test
        fun `GIVEN Up or Down event WHEN loaded THEN it is shown and nothing runs until the screen is in front`() =
            runTest {
                // Arrange
                val event = answer(index = 0)
                val controller = controller()

                // Act
                controller.load()
                advanceTimeBy(30_000)
                runCurrent()

                // Assert
                assertThat(controller.state.value).isEqualTo(
                    PolymarketCryptoEventState.Content(
                        windows = requireNotNull(PolymarketCryptoWindows.of(event)),
                        nowMillis = duringCryptoWindow(0),
                        points = emptyList(),
                        isPriceUnavailable = false,
                        quotes = emptyMap(),
                    ),
                )
                assertThat(reads()).isEqualTo(1)
                verify(exactly = 0) { observePrices(any(), any()) }
                verify(exactly = 0) { observeQuotes(any()) }
            }

        @Test
        fun `GIVEN first read fails WHEN it succeeds two seconds later THEN the event is shown with no failure`() =
            runTest {
                // Arrange
                answers(index = 0) {
                    if (reads() == 1) PolymarketEventError.Network.left() else event(index = 0).right()
                }
                val controller = controller()

                // Act
                controller.load()
                advanceTimeBy(1_999)
                runCurrent()
                val beforeRetry = controller.state.value
                advanceTimeBy(1)
                runCurrent()

                // Assert
                assertThat(beforeRetry).isEqualTo(PolymarketCryptoEventState.Loading)
                assertThat(controller.content().windows.shown).isEqualTo(event(index = 0))
            }

        @Test
        fun `GIVEN event cannot be read WHEN loaded THEN it fails and loading again shows it`() = runTest {
            // Arrange
            fails(index = 0)
            val controller = controller()
            controller.load()
            advanceTimeBy(2_000)
            runCurrent()
            val failed = controller.state.value
            answer(index = 0)

            // Act
            controller.load()
            runCurrent()

            // Assert
            assertThat(failed).isEqualTo(PolymarketCryptoEventState.Failed)
            assertThat(controller.content().windows.shown).isEqualTo(event(index = 0))
        }

        @Test
        fun `GIVEN screen already in front WHEN the event is loaded THEN everything starts with no second read`() =
            runTest {
                // Arrange
                answers(index = 0) {
                    delay(1_000)
                    event(index = 0).right()
                }
                val controller = controller()
                controller.onResume()

                // Act
                controller.load()
                advanceTimeBy(1_000)
                runCurrent()
                val collectors = priceFlow.subscriptionCount.value to quoteFlow.subscriptionCount.value
                advanceTimeBy(9_999)
                runCurrent()
                val readsBeforeTenSeconds = reads()
                advanceTimeBy(1)
                runCurrent()

                // Assert
                assertThat(collectors).isEqualTo(1 to 1)
                assertThat(listOf(readsBeforeTenSeconds, reads())).containsExactly(1, 2).inOrder()
                assertThat(controller.content().nowMillis).isEqualTo(duringCryptoWindow(0) + 11_000)
            }

        @Test
        fun `GIVEN event names no price feed WHEN loaded THEN the price is unavailable before the screen shows`() =
            runTest {
                // Arrange
                answers(index = 0) { createCryptoEvent(index = 0).right() }
                val controller = controller()

                // Act
                controller.load()
                runCurrent()

                // Assert
                assertThat(controller.content().isPriceUnavailable).isTrue()
            }

        @Test
        fun `GIVEN ordinary event WHEN loaded THEN it is told to be for another screen`() = runTest {
            // Arrange
            answers(index = 0) { createPolymarketEvent(id = cryptoWindowId(0)).right() }
            val controller = controller()

            // Act
            controller.load()
            runCurrent()

            // Assert
            assertThat(controller.state.value).isEqualTo(PolymarketCryptoEventState.NotUpDown)
        }
    }

    @Nested
    inner class InFront {

        @Test
        fun `GIVEN live window WHEN the screen comes in front THEN it is read at once and every ten seconds`() =
            runTest {
                // Arrange
                answer(index = 0)
                val controller = loaded()

                // Act
                controller.onResume()
                runCurrent()
                val readsAtOnce = reads()
                advanceTimeBy(9_999)
                runCurrent()
                val readsBeforeTenSeconds = reads()
                advanceTimeBy(1)
                runCurrent()
                val readsAtTenSeconds = reads()
                advanceTimeBy(10_000)
                runCurrent()

                // Assert
                assertThat(listOf(readsAtOnce, readsBeforeTenSeconds, readsAtTenSeconds, reads()))
                    .containsExactly(2, 2, 3, 4)
                    .inOrder()
            }

        @Test
        fun `GIVEN screen in front WHEN seconds pass THEN the clock of the state moves with them`() = runTest {
            // Arrange
            answer(index = 0)
            val controller = loaded()
            controller.onResume()
            runCurrent()

            // Act
            advanceTimeBy(3_000)
            runCurrent()

            // Assert
            assertThat(controller.content().nowMillis).isEqualTo(duringCryptoWindow(0) + 3_000)
        }

        @Test
        fun `GIVEN screen in front WHEN prices and quotes come THEN they are in the state`() = runTest {
            // Arrange
            answer(index = 0)
            val controller = loaded()
            controller.onResume()
            runCurrent()
            val quotes = mapOf("up-0" to PolymarketLiveQuote(assetId = "up-0", bestAsk = BigDecimal("0.52")))

            // Act
            priceFlow.value = live(points(1_000, 2_000))
            quoteFlow.value = quotes
            advanceTimeBy(300)
            runCurrent()

            // Assert
            assertThat(controller.content().points).isEqualTo(points(1_000, 2_000))
            assertThat(controller.content().quotes).isEqualTo(quotes)
            verify(exactly = 1) { observePrices(WALLET, FEED) }
            verify(exactly = 1) { observeQuotes(setOf("up-0", "down-0")) }
        }

        @Test
        fun `GIVEN nothing on the chart WHEN the first price comes alone THEN it is shown`() = runTest {
            // Arrange
            answer(index = 0)
            val controller = loaded()
            controller.onResume()
            runCurrent()

            // Act
            priceFlow.value = live(points(1_000))
            runCurrent()

            // Assert
            assertThat(controller.content().points).isEqualTo(points(1_000))
        }

        @Test
        fun `GIVEN outcomes without asset ids WHEN in front THEN no quotes are asked for`() = runTest {
            // Arrange
            answer(
                index = 0,
                outcomes = listOf(
                    createPolymarketOutcome(assetId = "", title = "Up"),
                    createPolymarketOutcome(assetId = " ", title = "Down"),
                ),
            )
            val controller = loaded()

            // Act
            controller.onResume()
            advanceTimeBy(5_000)
            runCurrent()

            // Assert
            verify(exactly = 0) { observeQuotes(any()) }
        }

        @Test
        fun `GIVEN prices on screen WHEN the shown window comes with another feed THEN the chart starts over`() =
            runTest {
                // Arrange
                val otherFeed = FEED.copy(symbol = "ethusd")
                answer(index = 0)
                val controller = loaded()
                controller.onResume()
                priceFlow.value = live(points(1_000, 2_000))
                runCurrent()
                priceFlow.value = PolymarketCryptoPriceSeries()
                answers(index = 0) { createCryptoEvent(index = 0, priceFeed = otherFeed).right() }

                // Act
                advanceTimeBy(10_000)
                runCurrent()

                // Assert
                assertThat(controller.content().points).isEmpty()
                verify(exactly = 1) { observePrices(WALLET, otherFeed) }
                assertThat(priceFlow.subscriptionCount.value).isEqualTo(1)
            }

        @Test
        fun `GIVEN event names no price feed WHEN in front THEN the price is unavailable and nothing is streamed`() =
            runTest {
                // Arrange
                answers(index = 0) { createCryptoEvent(index = 0).right() }
                val controller = loaded()

                // Act
                controller.onResume()
                runCurrent()

                // Assert
                assertThat(controller.content().isPriceUnavailable).isTrue()
                verify(exactly = 0) { observePrices(any(), any()) }
            }

        @Test
        fun `GIVEN prices on screen WHEN the stream gives up THEN the price is unavailable and the points stay`() =
            runTest {
                // Arrange
                answer(index = 0)
                val controller = loaded()
                controller.onResume()
                priceFlow.value = live(points(1_000, 2_000))
                runCurrent()

                // Act
                priceFlow.value = PolymarketCryptoPriceSeries(status = PolymarketCryptoPriceSeries.Status.UNAVAILABLE)
                runCurrent()

                // Assert
                assertThat(controller.content().points).isEqualTo(points(1_000, 2_000))
                assertThat(controller.content().isPriceUnavailable).isTrue()
            }

        @Test
        fun `GIVEN refresh keeps failing WHEN a read succeeds in between THEN stale data is reported once per run`() =
            runTest {
                // Arrange
                answer(index = 0)
                val controller = loaded()
                fails(index = 0)

                // Act
                controller.onResume()
                advanceTimeBy(14_000)
                runCurrent()
                val reportsAfterTwoFailures = reports
                answer(index = 0)
                advanceTimeBy(10_000)
                runCurrent()
                fails(index = 0)
                advanceTimeBy(12_000)
                runCurrent()

                // Assert
                assertThat(reportsAfterTwoFailures).isEqualTo(1)
                assertThat(reports).isEqualTo(2)
                assertThat(controller.content().windows.shown).isEqualTo(event(index = 0))
            }
    }

    @Nested
    inner class Paused {

        @Test
        fun `GIVEN stream gave up WHEN the screen comes back THEN the price stays unavailable until it answers`() =
            runTest {
                // Arrange
                answer(index = 0)
                val controller = loaded()
                controller.onResume()
                priceFlow.value = PolymarketCryptoPriceSeries(
                    points = points(1_000, 2_000),
                    status = PolymarketCryptoPriceSeries.Status.UNAVAILABLE,
                )
                runCurrent()
                controller.onPause()
                priceFlow.value = PolymarketCryptoPriceSeries()

                // Act
                controller.onResume()
                runCurrent()
                val whileWaiting = controller.content().isPriceUnavailable
                priceFlow.value = live(points(2_000, 3_000))
                runCurrent()

                // Assert
                assertThat(whileWaiting).isTrue()
                assertThat(controller.content().isPriceUnavailable).isFalse()
                assertThat(controller.content().points).isEqualTo(points(1_000, 2_000, 3_000))
            }

        @Test
        fun `GIVEN screen in front and a switch under way WHEN paused THEN nothing is read or streamed any more`() =
            runTest {
                // Arrange
                answer(index = 0)
                answers(index = 1) {
                    delay(60_000)
                    event(index = 1).right()
                }
                val controller = loaded()
                controller.onResume()
                runCurrent()
                controller.selectWindow(cryptoWindowId(1))
                runCurrent()
                val readsBeforePause = reads()

                // Act
                controller.onPause()
                advanceTimeBy(120_000)
                runCurrent()

                // Assert
                assertThat(reads()).isEqualTo(readsBeforePause)
                assertThat(priceFlow.subscriptionCount.value).isEqualTo(0)
                assertThat(quoteFlow.subscriptionCount.value).isEqualTo(0)
                assertThat(controller.content().windows.switchingTo).isNull()
                assertThat(controller.content().windows.shown).isEqualTo(event(index = 0))
            }

        @Test
        fun `GIVEN paused and brought back twice WHEN ten seconds pass THEN reads and streams are not multiplied`() =
            runTest {
                // Arrange
                answer(index = 0)
                val controller = loaded()
                repeat(times = 3) {
                    controller.onResume()
                    runCurrent()
                    controller.onPause()
                    runCurrent()
                }
                controller.onResume()
                runCurrent()
                val readsOnReturn = reads()

                // Act
                advanceTimeBy(10_000)
                runCurrent()

                // Assert
                assertThat(readsOnReturn).isEqualTo(5)
                assertThat(reads()).isEqualTo(6)
                assertThat(priceFlow.subscriptionCount.value).isEqualTo(1)
                assertThat(quoteFlow.subscriptionCount.value).isEqualTo(1)
            }

        @Test
        fun `GIVEN prices on screen WHEN the screen comes back THEN ticks join them until the snapshot`() = runTest {
            // Arrange
            answer(index = 0)
            val controller = loaded()
            controller.onResume()
            priceFlow.value = live(points(1_000, 2_000))
            runCurrent()
            controller.onPause()
            priceFlow.value = PolymarketCryptoPriceSeries()

            // Act
            controller.onResume()
            runCurrent()
            val whileWaiting = controller.content().points
            priceFlow.value = live(points(3_000))
            runCurrent()
            val afterFirstTick = controller.content().points
            priceFlow.value = live(points(3_000, 4_000))
            runCurrent()
            val afterSecondTick = controller.content().points
            priceFlow.value = live(points(500, 1_500, 2_500, 3_500, 4_500))
            runCurrent()

            // Assert
            assertThat(listOf(whileWaiting, afterFirstTick, afterSecondTick))
                .containsExactly(
                    points(1_000, 2_000),
                    points(1_000, 2_000, 3_000),
                    points(1_000, 2_000, 3_000, 4_000),
                )
                .inOrder()
            assertThat(controller.content().points).isEqualTo(points(500, 1_500, 2_500, 3_500, 4_500))
        }

        @Test
        fun `GIVEN prices long on screen WHEN a fresh one comes THEN those a series no longer reaches are dropped`() =
            runTest {
                // Arrange
                answer(index = 0)
                val controller = loaded()
                controller.onResume()
                priceFlow.value = live(points(1_000, 80_000))
                runCurrent()
                controller.onPause()
                priceFlow.value = PolymarketCryptoPriceSeries()
                controller.onResume()
                runCurrent()

                // Act
                priceFlow.value = live(points(200_000))
                runCurrent()

                // Assert
                assertThat(controller.content().points).isEqualTo(points(80_000, 200_000))
            }

        @Test
        fun `GIVEN screen not in front WHEN a window is chosen THEN nothing happens`() = runTest {
            // Arrange
            answer(index = 0)
            answer(index = 1)
            val controller = loaded()

            // Act
            controller.selectWindow(cryptoWindowId(1))
            runCurrent()

            // Assert
            assertThat(controller.content().windows.switchingTo).isNull()
            assertThat(readIds).containsExactly(cryptoWindowId(0))
        }
    }

    @Nested
    inner class Bounds {

        @Test
        fun `GIVEN live window on screen WHEN it ends THEN it is read after two seconds and then every five`() =
            runTest {
                // Arrange
                startMillis = cryptoWindowStart(1) - 5_000
                answer(index = 0)
                val controller = loaded()
                controller.onResume()
                runCurrent()
                val readsWhileLive = reads()

                // Act
                advanceTimeBy(6_999)
                runCurrent()
                val readsBeforeTwoSeconds = reads()
                advanceTimeBy(1)
                runCurrent()
                val readsAtTwoSeconds = reads()
                advanceTimeBy(4_999)
                runCurrent()
                val readsBeforeRetry = reads()
                advanceTimeBy(1)
                runCurrent()

                // Assert
                assertThat(listOf(readsWhileLive, readsBeforeTwoSeconds, readsAtTwoSeconds, readsBeforeRetry, reads()))
                    .containsExactly(2, 2, 3, 3, 4)
                    .inOrder()
            }

        @Test
        fun `GIVEN ended window is read again and again WHEN it comes settled THEN it is read no more`() = runTest {
            // Arrange
            startMillis = cryptoWindowStart(1) - 5_000
            answer(index = 0)
            val controller = loaded()
            controller.onResume()
            advanceTimeBy(7_000)
            runCurrent()
            val settled = answer(index = 0, finalPrice = BigDecimal("105"), outcomes = settledOutcomes(index = 0))
            advanceTimeBy(5_000)
            runCurrent()
            val readsWhenSettled = reads()

            // Act
            advanceTimeBy(900_000)
            runCurrent()

            // Assert
            assertThat(reads()).isEqualTo(readsWhenSettled)
            assertThat(controller.content().windows.shown).isEqualTo(settled)
            assertThat(controller.content().winner?.outcome?.assetId).isEqualTo("up-0")
        }

        @Test
        fun `GIVEN ended window never gets its result WHEN ten minutes pass THEN reading it is given up`() = runTest {
            // Arrange
            startMillis = cryptoWindowStart(1) - 5_000
            answer(index = 0)
            val controller = loaded()
            controller.onResume()
            advanceTimeBy(5_000 + 600_000)
            runCurrent()
            val readsAtTenMinutes = reads()

            // Act
            advanceTimeBy(3_600_000)
            runCurrent()

            // Assert
            assertThat(reads()).isEqualTo(readsAtTenMinutes)
        }

        @Test
        fun `GIVEN read after the end is slow WHEN retries come due THEN it is left to finish`() = runTest {
            // Arrange
            startMillis = cryptoWindowStart(1) - 5_000
            answer(index = 0)
            val controller = loaded()
            controller.onResume()
            runCurrent()
            val readsWhileLive = reads()
            answers(index = 0) {
                delay(12_000)
                event(index = 0, title = "Slow").right()
            }

            // Act
            advanceTimeBy(7_000 + 11_999)
            runCurrent()
            val readsWhileSlow = reads()
            advanceTimeBy(1)
            runCurrent()

            // Assert
            assertThat(readsWhileSlow - readsWhileLive).isEqualTo(1)
            assertThat(controller.content().windows.shown.title).isEqualTo("Slow")
        }

        @Test
        fun `GIVEN upcoming window on screen WHEN it starts THEN it is read two seconds later for the price to beat`() =
            runTest {
                // Arrange
                startMillis = cryptoWindowStart(1) - 5_000
                answer(index = 1, priceToBeat = null)
                val controller = loaded(index = 1)
                controller.onResume()
                runCurrent()
                val readsBeforeStart = reads()

                // Act
                advanceTimeBy(6_999)
                runCurrent()
                val readsBeforeTwoSeconds = reads()
                advanceTimeBy(1)
                runCurrent()

                // Assert
                assertThat(listOf(readsBeforeStart, readsBeforeTwoSeconds, reads())).containsExactly(2, 2, 3).inOrder()
            }

        @Test
        fun `GIVEN window ends WHEN its stream goes on THEN later prices are dropped and both streams are stopped`() =
            runTest {
                // Arrange
                val endMillis = cryptoWindowStart(1)
                startMillis = endMillis - 3_000
                answer(index = 0)
                val controller = loaded()
                controller.onResume()
                quoteFlow.value = mapOf("up-0" to PolymarketLiveQuote(assetId = "up-0", bestAsk = BigDecimal("0.52")))
                advanceTimeBy(300)
                runCurrent()
                val quotesWhileLive = controller.content().quotes

                // Act
                advanceTimeBy(2_700)
                priceFlow.value = live(points(endMillis - 1_000, endMillis, endMillis + 1_000))
                runCurrent()
                val quoteCollectorsAtEnd = quoteFlow.subscriptionCount.value
                val priceCollectorsAtEnd = priceFlow.subscriptionCount.value
                advanceTimeBy(5_000)
                runCurrent()

                // Assert
                assertThat(quotesWhileLive).isNotEmpty()
                assertThat(controller.content().quotes).isEmpty()
                assertThat(controller.content().points).isEqualTo(points(endMillis - 1_000, endMillis))
                assertThat(listOf(quoteCollectorsAtEnd, priceCollectorsAtEnd, priceFlow.subscriptionCount.value))
                    .containsExactly(0, 1, 0)
                    .inOrder()
            }
    }

    @Nested
    inner class Switch {

        @Test
        fun `GIVEN live window WHEN a tab is chosen THEN it is marked at once and shown when its answer comes`() =
            runTest {
                // Arrange
                answer(index = 0)
                answers(index = 1) {
                    delay(1_000)
                    event(index = 1).right()
                }
                val controller = loaded()
                controller.onResume()
                priceFlow.value = live(points(1_000, 2_000))
                runCurrent()

                // Act
                controller.selectWindow(cryptoWindowId(1))
                runCurrent()
                val whileSwitching = controller.content().windows
                advanceTimeBy(1_000)
                runCurrent()

                // Assert
                assertThat(whileSwitching.switchingTo).isEqualTo(cryptoWindowId(1))
                assertThat(whileSwitching.shown).isEqualTo(event(index = 0))
                assertThat(controller.content().windows.shown).isEqualTo(event(index = 1))
                assertThat(controller.content().windows.switchingTo).isNull()
            }

        @Test
        fun `GIVEN window of the same feed WHEN switched to THEN the chart goes on and quotes follow its market`() =
            runTest {
                // Arrange
                answer(index = 0)
                answer(index = 1)
                val controller = loaded()
                controller.onResume()
                priceFlow.value = live(points(1_000, 2_000))
                runCurrent()

                // Act
                controller.selectWindow(cryptoWindowId(1))
                runCurrent()

                // Assert
                assertThat(controller.content().points).isEqualTo(points(1_000, 2_000))
                verify(exactly = 1) { observePrices(any(), any()) }
                verify(exactly = 1) { observeQuotes(setOf("up-1", "down-1")) }
                assertThat(quoteFlow.subscriptionCount.value).isEqualTo(1)
            }

        @Test
        fun `GIVEN window of another feed WHEN switched THEN the chart starts over from that feed`() = runTest {
            // Arrange
            val otherFeed = FEED.copy(symbol = "ethusd")
            answer(index = 0)
            answers(index = 1) { createCryptoEvent(index = 1, priceFeed = otherFeed).right() }
            val controller = loaded()
            controller.onResume()
            priceFlow.value = live(points(1_000, 2_000))
            runCurrent()
            priceFlow.value = PolymarketCryptoPriceSeries()

            // Act
            controller.selectWindow(cryptoWindowId(1))
            runCurrent()

            // Assert
            assertThat(controller.content().points).isEmpty()
            verify(exactly = 1) { observePrices(WALLET, otherFeed) }
            assertThat(priceFlow.subscriptionCount.value).isEqualTo(1)
        }

        @Test
        fun `GIVEN window that has ended WHEN switched to THEN the chart is cleared and nothing is streamed`() =
            runTest {
                // Arrange
                startMillis = duringCryptoWindow(1)
                answer(index = 0)
                answer(index = 1)
                val controller = loaded(index = 1)
                controller.onResume()
                priceFlow.value = live(points(cryptoWindowStart(1), cryptoWindowStart(1) + 1_000))
                runCurrent()

                // Act
                controller.selectWindow(cryptoWindowId(0))
                runCurrent()

                // Assert
                assertThat(controller.content().windows.shown).isEqualTo(event(index = 0))
                assertThat(controller.content().points).isEmpty()
                assertThat(priceFlow.subscriptionCount.value).isEqualTo(0)
                assertThat(quoteFlow.subscriptionCount.value).isEqualTo(0)
            }

        @Test
        fun `GIVEN settled window on screen WHEN moved to the live one THEN that one is read every ten seconds`() =
            runTest {
                // Arrange
                startMillis = duringCryptoWindow(1)
                answer(index = 0, finalPrice = BigDecimal("105"), outcomes = settledOutcomes(index = 0))
                answer(index = 1)
                val controller = loaded()
                controller.onResume()
                advanceTimeBy(30_000)
                runCurrent()

                // Act
                controller.goToLive()
                advanceTimeBy(9_999)
                runCurrent()
                val readsBeforeTenSeconds = reads(index = 1)
                advanceTimeBy(1)
                runCurrent()
                val readsAtTenSeconds = reads(index = 1)
                advanceTimeBy(10_000)
                runCurrent()

                // Assert
                assertThat(listOf(readsBeforeTenSeconds, readsAtTenSeconds, reads(index = 1)))
                    .containsExactly(1, 2, 3)
                    .inOrder()
            }

        @Test
        fun `GIVEN settled window on screen WHEN moved to one without its price THEN retries count from the move`() =
            runTest {
                // Arrange
                startMillis = duringCryptoWindow(1)
                answer(index = 0, finalPrice = BigDecimal("105"), outcomes = settledOutcomes(index = 0))
                answer(index = 1, priceToBeat = null)
                val controller = loaded()
                controller.onResume()
                advanceTimeBy(30_000)
                runCurrent()

                // Act
                controller.goToLive()
                advanceTimeBy(4_999)
                runCurrent()
                val readsBeforeRetry = reads(index = 1)
                advanceTimeBy(1)
                runCurrent()

                // Assert
                assertThat(listOf(readsBeforeRetry, reads(index = 1))).containsExactly(1, 2).inOrder()
            }

        @Test
        fun `GIVEN refresh under way WHEN a switch cuts it short THEN the new window is still read after its start`() =
            runTest {
                // Arrange
                startMillis = cryptoWindowStart(1) - 3_000
                answer(index = 0)
                answer(index = 1, priceToBeat = null)
                val controller = loaded()
                answers(index = 0) {
                    delay(60_000)
                    event(index = 0).right()
                }
                controller.onResume()
                runCurrent()

                // Act
                controller.selectWindow(cryptoWindowId(1))
                advanceTimeBy(4_999)
                runCurrent()
                val readsBeforeTwoSeconds = reads(index = 1)
                advanceTimeBy(1)
                runCurrent()

                // Assert
                assertThat(listOf(readsBeforeTwoSeconds, reads(index = 1))).containsExactly(1, 2).inOrder()
            }

        @Test
        fun `GIVEN windows chosen one after another WHEN the first would fail last THEN only the last is shown`() =
            runTest {
                // Arrange
                answer(index = 0)
                answers(index = 1) {
                    delay(5_000)
                    PolymarketEventError.Network.left()
                }
                answers(index = 2) {
                    delay(1_000)
                    event(index = 2).right()
                }
                val controller = loaded()
                controller.onResume()
                runCurrent()

                // Act
                controller.selectWindow(cryptoWindowId(1))
                advanceTimeBy(500)
                controller.selectWindow(cryptoWindowId(2))
                advanceTimeBy(1_000)
                runCurrent()
                val afterLastChoice = controller.content().windows.shown
                advanceTimeBy(8_000)
                runCurrent()

                // Assert
                assertThat(afterLastChoice).isEqualTo(event(index = 2))
                assertThat(controller.content().windows.shown).isEqualTo(event(index = 2))
                assertThat(reports).isEqualTo(0)
            }

        @Test
        fun `GIVEN chosen window cannot be read WHEN its load fails THEN the shown one stays and it is reported`() =
            runTest {
                // Arrange
                answer(index = 0)
                fails(index = 1)
                val controller = loaded()
                controller.onResume()
                runCurrent()

                // Act
                controller.selectWindow(cryptoWindowId(1))
                advanceTimeBy(2_000)
                runCurrent()

                // Assert
                assertThat(controller.content().windows.shown).isEqualTo(event(index = 0))
                assertThat(controller.content().windows.switchingTo).isNull()
                assertThat(reports).isEqualTo(1)
            }

        @Test
        fun `GIVEN refresh failed and was reported WHEN a switch succeeds THEN the next failure is reported again`() =
            runTest {
                // Arrange
                answer(index = 0)
                answer(index = 1)
                val controller = loaded()
                fails(index = 0)
                controller.onResume()
                advanceTimeBy(2_000)
                runCurrent()
                val reportsOnFirstWindow = reports
                controller.selectWindow(cryptoWindowId(1))
                runCurrent()
                fails(index = 1)

                // Act
                advanceTimeBy(12_000)
                runCurrent()

                // Assert
                assertThat(controller.content().windows.shown).isEqualTo(event(index = 1))
                assertThat(listOf(reportsOnFirstWindow, reports)).containsExactly(1, 2).inOrder()
            }

        @Test
        fun `GIVEN switch under way WHEN the shown window is chosen THEN the load is given up`() = runTest {
            // Arrange
            answer(index = 0)
            answers(index = 1) {
                delay(1_000)
                PolymarketEventError.Network.left()
            }
            val controller = loaded()
            controller.onResume()
            runCurrent()
            controller.selectWindow(cryptoWindowId(1))
            runCurrent()

            // Act
            controller.selectWindow(cryptoWindowId(0))
            advanceTimeBy(5_000)
            runCurrent()

            // Assert
            assertThat(controller.content().windows.shown).isEqualTo(event(index = 0))
            assertThat(controller.content().windows.switchingTo).isNull()
            assertThat(reports).isEqualTo(0)
        }

        @Test
        fun `GIVEN ended window on screen WHEN going to live THEN the window live by the clock is shown`() = runTest {
            // Arrange
            startMillis = duringCryptoWindow(1)
            answer(index = 0)
            answer(index = 1)
            val controller = loaded()
            controller.onResume()
            runCurrent()

            // Act
            controller.goToLive()
            runCurrent()

            // Assert
            assertThat(controller.content().windows.shown).isEqualTo(event(index = 1))
        }

        @Test
        fun `GIVEN old window whose word on the live one has aged WHEN going to live THEN the live one is reached`() =
            runTest {
                // Arrange
                startMillis = duringCryptoWindow(40)
                answer(index = 0, following = 5, liveEventId = cryptoWindowId(30))
                answer(index = 30, liveEventId = cryptoWindowId(40))
                answer(index = 40)
                val controller = loaded()
                controller.onResume()
                runCurrent()

                // Act
                controller.goToLive()
                runCurrent()

                // Assert
                assertThat(controller.content().windows.shown).isEqualTo(event(index = 40))
                assertThat(controller.content().windows.switchingTo).isNull()
                coVerifyOrder {
                    getEvent(WALLET, cryptoWindowId(30))
                    getEvent(WALLET, cryptoWindowId(40))
                }
            }
    }

    private fun TestScope.controller(index: Int = 0) = PolymarketCryptoEventController(
        scope = backgroundScope,
        userWalletId = WALLET,
        eventId = cryptoWindowId(index),
        getEvent = getEvent,
        observePrices = observePrices,
        observeQuotes = observeQuotes,
        clock = { startMillis + testScheduler.currentTime },
        onStaleData = { reports++ },
    )

    private fun TestScope.loaded(index: Int = 0): PolymarketCryptoEventController {
        return controller(index).apply {
            load()
            runCurrent()
        }
    }

    private fun PolymarketCryptoEventController.content() = state.value as PolymarketCryptoEventState.Content

    private fun event(
        index: Int,
        following: Int = 20,
        title: String = "Bitcoin Up or Down $index",
        priceToBeat: BigDecimal? = BigDecimal("100"),
        finalPrice: BigDecimal? = null,
        liveEventId: String? = null,
        outcomes: List<PolymarketOutcome>? = null,
    ): PolymarketEvent {
        val default = createCryptoEvent(
            index = index,
            following = following,
            title = title,
            priceToBeat = priceToBeat,
            finalPrice = finalPrice,
            liveEventId = liveEventId,
            priceFeed = FEED,
        )
        if (outcomes == null) return default

        return default.copy(markets = default.markets.map { it.copy(outcomes = outcomes) })
    }

    /** Makes the backend answer for window `w<index>` with the event that is returned. */
    private fun answer(
        index: Int,
        following: Int = 20,
        priceToBeat: BigDecimal? = BigDecimal("100"),
        finalPrice: BigDecimal? = null,
        liveEventId: String? = null,
        outcomes: List<PolymarketOutcome>? = null,
    ): PolymarketEvent {
        val event = event(
            index = index,
            following = following,
            priceToBeat = priceToBeat,
            finalPrice = finalPrice,
            liveEventId = liveEventId,
            outcomes = outcomes,
        )
        answers(index) { event.right() }
        return event
    }

    private fun fails(index: Int) = answers(index) { PolymarketEventError.Network.left() }

    /** Every read of window `w<index>` that begins is counted, also one that is cancelled before its answer. */
    private fun answers(index: Int, answer: suspend () -> Either<PolymarketEventError, PolymarketEvent>) {
        val id = cryptoWindowId(index)
        coEvery { getEvent(WALLET, id) } coAnswers {
            readIds += id
            answer()
        }
    }

    private fun reads(): Int = readIds.size

    private fun reads(index: Int): Int = readIds.count { it == cryptoWindowId(index) }

    private fun settledOutcomes(index: Int) = listOf(
        createPolymarketOutcome(assetId = "up-$index", title = "Up", probability = BigDecimal.ONE),
        createPolymarketOutcome(assetId = "down-$index", title = "Down", probability = BigDecimal.ZERO),
    )

    private fun live(points: List<PolymarketCryptoPricePoint>) = PolymarketCryptoPriceSeries(
        points = points,
        status = PolymarketCryptoPriceSeries.Status.LIVE,
    )

    private fun points(vararg timestamps: Long) = timestamps.map {
        PolymarketCryptoPricePoint(timestampMillis = it, price = BigDecimal("100"))
    }

    private companion object {
        val WALLET = UserWalletId("011")
        val FEED = PolymarketCryptoPriceFeed(
            channel = "price.crypto",
            symbol = "btcusd",
            provider = null,
            windowSeconds = null,
        )
    }
}