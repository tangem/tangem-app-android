package com.tangem.features.earn.impl.overview

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.earn.api.overview.EarnRateType
import com.tangem.features.earn.product.api.product.EarnPositionSummary
import com.tangem.features.earn.product.api.product.EarnProduct
import com.tangem.features.earn.product.api.product.EarnProductSummary
import com.tangem.features.earn.product.api.product.EarnRateSource
import com.tangem.features.earn.product.api.product.ProductAvailability
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.TestFlowProducerTools
import com.tangem.test.core.assertSome
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import com.tangem.test.core.getEmittedValues
import com.tangem.utils.logging.Severity
import com.tangem.utils.logging.TangemLogger
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import java.util.Locale

private const val PRODUCER_RETRY_DELAY_MS = 10_000L

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultEarnProductSummaryProducerTest {

    private val logWriter: TangemLogger.LogWriter = mockk(relaxed = true)

    private val badgeConverter = EarnRateBadgeConverter()

    private var previousLocale: Locale = Locale.getDefault()

    @BeforeEach
    fun setUp() {
        previousLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
        clearMocks(logWriter)
    }

    @AfterEach
    fun tearDown() {
        Locale.setDefault(previousLocale)
        TangemLogger.setLogWriters(emptyList())
    }

    @Test
    fun `GIVEN one product has not answered and another has WHEN produce THEN CACHE frames include answered`() =
        runTest {
            // Arrange
            val answeredSummary = createSummary(productId = ANSWERED_ID, rate = createRate("0.04"))
            val pending = TestEarnProduct(id = PENDING_ID, rank = 0, summariesFlow = MutableSharedFlow())
            val answered = TestEarnProduct(
                id = ANSWERED_ID,
                rank = 1,
                summariesFlow = flowOf(mapOf(ETH_ID to answeredSummary)),
            )
            val producer = createSummaryProducer(products = setOf(pending, answered))

            // Act
            val frames = getEmittedValues(producer.produce())
            runCurrent()

            // Assert
            val expected = EarnProductSummaries(
                products = mapOf(ETH_ID to listOf(answeredSummary)),
                rateSources = emptyMap(),
                source = StatusSource.CACHE,
            )
            assertThat(frames.first().source).isEqualTo(StatusSource.CACHE)
            assertThat(frames.last()).isEqualTo(expected)
        }

    @Test
    fun `GIVEN a product flow throws WHEN produce THEN source is ONLY_CACHE and other products stay`() = runTest {
        // Arrange
        val healthySummary = createSummary(productId = HEALTHY_ID)
        val failing = TestEarnProduct(
            id = FAILING_ID,
            rank = 0,
            summariesFlow = throwingSummaries(IllegalStateException("boom")),
        )
        val healthy = TestEarnProduct(
            id = HEALTHY_ID,
            rank = 1,
            summariesFlow = flowOf(mapOf(ETH_ID to healthySummary)),
        )
        val producer = createSummaryProducer(products = setOf(failing, healthy))

        // Act
        val frames = getEmittedValues(producer.produce())
        runCurrent()

        // Assert
        val expected = EarnProductSummaries(
            products = mapOf(ETH_ID to listOf(healthySummary)),
            rateSources = emptyMap(),
            source = StatusSource.ONLY_CACHE,
        )
        assertThat(frames.last()).isEqualTo(expected)
    }

    @Test
    fun `GIVEN a product flow throws WHEN RETRY_DELAY passes THEN the product is queried again`() = runTest {
        // Arrange
        val failing = TestEarnProduct(
            id = FAILING_ID,
            summariesFlow = throwingSummaries(IllegalStateException("boom")),
        )
        val producer = createSummaryProducer(products = setOf(failing))
        getEmittedValues(producer.produce())

        // Act
        advanceTimeBy(PRODUCER_RETRY_DELAY_MS - 1)
        runCurrent()
        val collectCountBeforeDelay = failing.collectCount
        advanceTimeBy(2)
        runCurrent()

        // Assert
        assertThat(collectCountBeforeDelay).isEqualTo(1)
        assertThat(failing.collectCount).isEqualTo(2)
    }

    @Test
    fun `GIVEN a producer of another wallet WHEN produce THEN products and rate sources get that wallet id`() =
        runTest {
            // Arrange
            val product = TestEarnProduct(id = PRODUCT_ID)
            val rateSource = TestEarnRateSource(id = YIELD_ID)
            val producer = createSummaryProducer(
                products = setOf(product),
                rateSources = setOf(rateSource),
                userWalletId = OTHER_WALLET_ID,
            )

            // Act
            getEmittedValues(producer.produce())
            runCurrent()

            // Assert
            assertThat(product.summariesWalletIds).containsExactly(OTHER_WALLET_ID)
            assertThat(rateSource.ratesWalletIds).containsExactly(OTHER_WALLET_ID)
        }

    @Test
    fun `GIVEN a product keeps failing WHEN retried twice THEN the failure is logged once`() = runTest {
        // Arrange
        val cause = IllegalStateException("boom")
        every { logWriter.isLoggable(any(), any()) } returns true
        TangemLogger.setLogWriters(listOf(logWriter))
        val failing = TestEarnProduct(id = FAILING_ID, summariesFlow = throwingSummaries(cause))
        val producer = createSummaryProducer(products = setOf(failing))

        // Act
        getEmittedValues(producer.produce())
        runCurrent()
        advanceTimeBy(PRODUCER_RETRY_DELAY_MS + 1)
        runCurrent()
        advanceTimeBy(PRODUCER_RETRY_DELAY_MS)
        runCurrent()

        // Assert
        assertThat(failing.collectCount).isEqualTo(3)
        verify(exactly = 1) {
            logWriter.write(Severity.Error, any(), "Earn summaries failed: ${FAILING_ID.value}", cause, any())
        }
    }

    @Test
    fun `GIVEN a feed that fails, recovers and fails again WHEN retried THEN each series of failures is logged once`() =
        runTest {
            // Arrange
            val message = "Earn summaries failed: ${FAILING_ID.value}"
            val firstCause = IllegalStateException("first")
            val secondCause = IllegalStateException("second")
            val thirdCause = IllegalStateException("third")
            installLogWriter()
            val secondCollectionFailureGate = MutableStateFlow(false)
            var collections = 0
            val failing = TestEarnProduct(
                id = FAILING_ID,
                summariesFlow = flow {
                    when (collections++) {
                        0 -> throw firstCause
                        1 -> {
                            emit(mapOf(ETH_ID to createSummary(productId = FAILING_ID)))
                            secondCollectionFailureGate.first { it }
                            throw secondCause
                        }
                        else -> throw thirdCause
                    }
                },
            )
            val producer = createSummaryProducer(products = setOf(failing))

            // Act
            getEmittedValues(producer.produce())
            runCurrent()
            advanceTimeBy(PRODUCER_RETRY_DELAY_MS + 1)
            runCurrent()
            secondCollectionFailureGate.value = true
            runCurrent()
            advanceTimeBy(PRODUCER_RETRY_DELAY_MS + 1)
            runCurrent()

            // Assert
            assertThat(failing.collectCount).isEqualTo(3)
            verify(exactly = 2) { logWriter.write(Severity.Error, any(), message, any(), any()) }
            verify(exactly = 1) { logWriter.write(Severity.Error, any(), message, firstCause, any()) }
            verify(exactly = 1) { logWriter.write(Severity.Error, any(), message, secondCause, any()) }
            verify(exactly = 0) { logWriter.write(Severity.Error, any(), message, thirdCause, any()) }
        }

    @Test
    fun `GIVEN a product failed once WHEN it answers after RETRY_DELAY THEN its summaries return as ACTUAL`() =
        runTest {
            // Arrange
            val summary = createSummary(productId = RECOVERING_ID)
            var attempts = 0
            val recovering = TestEarnProduct(
                id = RECOVERING_ID,
                summariesFlow = flow {
                    if (attempts++ == 0) throw IllegalStateException("boom")
                    emit(mapOf(ETH_ID to summary))
                },
            )
            val producer = createSummaryProducer(products = setOf(recovering))
            val frames = getEmittedValues(producer.produce())
            runCurrent()

            // Act
            advanceTimeBy(PRODUCER_RETRY_DELAY_MS + 1)
            runCurrent()

            // Assert
            val expected = EarnProductSummaries(
                products = mapOf(ETH_ID to listOf(summary)),
                rateSources = emptyMap(),
                source = StatusSource.ACTUAL,
            )
            assertThat(frames.last()).isEqualTo(expected)
        }

    @Test
    fun `GIVEN a loaded product WHEN its flow throws THEN previous summaries stay as cache until the retry succeeds`() =
        runTest {
            // Arrange
            val loadedSummary = createSummary(
                productId = FAILING_ID,
                position = EarnPositionSummary(cryptoAmount = BigDecimal.ONE),
            )
            val healthySummary = createSummary(productId = HEALTHY_ID)
            val failureGate = MutableStateFlow(false)
            var collections = 0
            val failing = TestEarnProduct(
                id = FAILING_ID,
                rank = 0,
                summariesFlow = flow {
                    emit(mapOf(ETH_ID to loadedSummary))
                    if (collections++ == 0) {
                        failureGate.first { it }
                        throw IllegalStateException("boom")
                    }
                },
            )
            val healthy = TestEarnProduct(
                id = HEALTHY_ID,
                rank = 1,
                summariesFlow = flowOf(mapOf(ETH_ID to healthySummary)),
            )
            val producer = createSummaryProducer(products = setOf(failing, healthy))
            val frames = getEmittedValues(producer.produce())
            runCurrent()
            val whileLoaded = frames.last()

            // Act
            failureGate.value = true
            runCurrent()
            val afterFailure = frames.last()
            advanceTimeBy(PRODUCER_RETRY_DELAY_MS + 1)
            runCurrent()
            val afterRetry = frames.last()

            // Assert
            val loaded = EarnProductSummaries(
                products = mapOf(ETH_ID to listOf(loadedSummary, healthySummary)),
                rateSources = emptyMap(),
                source = StatusSource.ACTUAL,
            )
            val cachedLoadedSummary = loadedSummary.copy(source = StatusSource.ONLY_CACHE)
            val cached = EarnProductSummaries(
                products = mapOf(ETH_ID to listOf(cachedLoadedSummary, healthySummary)),
                rateSources = emptyMap(),
                source = StatusSource.ONLY_CACHE,
            )
            assertThat(whileLoaded).isEqualTo(loaded)
            assertThat(afterFailure).isEqualTo(cached)
            assertThat(afterRetry).isEqualTo(loaded)
        }

    @Test
    fun `GIVEN a product that loaded an empty map WHEN its flow throws THEN the source is ONLY_CACHE`() = runTest {
        // Arrange
        val healthySummary = createSummary(productId = HEALTHY_ID)
        val failureGate = MutableStateFlow(false)
        var collections = 0
        val failing = TestEarnProduct(
            id = FAILING_ID,
            rank = 0,
            summariesFlow = flow {
                emit(emptyMap())
                if (collections++ == 0) {
                    failureGate.first { it }
                    throw IllegalStateException("boom")
                }
            },
        )
        val healthy = TestEarnProduct(
            id = HEALTHY_ID,
            rank = 1,
            summariesFlow = flowOf(mapOf(ETH_ID to healthySummary)),
        )
        val producer = createSummaryProducer(products = setOf(failing, healthy))
        val frames = getEmittedValues(producer.produce())
        runCurrent()
        val whileLoaded = frames.last()

        // Act
        failureGate.value = true
        runCurrent()

        // Assert
        val products = mapOf(ETH_ID to listOf(healthySummary))
        assertThat(whileLoaded).isEqualTo(EarnProductSummaries(products, emptyMap(), StatusSource.ACTUAL))
        assertThat(frames.last()).isEqualTo(EarnProductSummaries(products, emptyMap(), StatusSource.ONLY_CACHE))
    }

    @Test
    fun `GIVEN a loaded rate source WHEN its flow throws THEN its summary and route stay as cache`() = runTest {
        // Arrange
        val route = TestRoute("staking")
        val entry = createRateEntry(rate = createRate("0.04"), isActive = true, route = route)
        val failureGate = MutableStateFlow(false)
        var collections = 0
        val rateSource = TestEarnRateSource(
            id = YIELD_ID,
            ratesFlow = flow {
                emit(mapOf(ETH_ID to entry))
                if (collections++ == 0) {
                    failureGate.first { it }
                    throw IllegalStateException("boom")
                }
            },
        )
        val producer = createSummaryProducer(rateSources = setOf(rateSource))
        val frames = getEmittedValues(producer.produce())
        runCurrent()

        // Act
        failureGate.value = true
        runCurrent()

        // Assert
        val actual = frames.last()
        val summary = actual.rateSources.getValue(ETH_ID).single()
        assertThat(actual.source).isEqualTo(StatusSource.ONLY_CACHE)
        assertThat(summary.route).isEqualTo(route)
        assertThat(summary.source).isEqualTo(StatusSource.ONLY_CACHE)
        assertThat(summary.rate).isEqualTo(createRate("0.04"))
        assertThat(summary.position).isEqualTo(EarnPositionSummary(cryptoAmount = null))
    }

    @Test
    fun `GIVEN a product that completes without a value WHEN produce THEN the source is ACTUAL`() = runTest {
        // Arrange
        val healthySummary = createSummary(productId = HEALTHY_ID)
        val silent = TestEarnProduct(id = FAILING_ID, rank = 0, summariesFlow = emptyFlow())
        val healthy = TestEarnProduct(
            id = HEALTHY_ID,
            rank = 1,
            summariesFlow = flowOf(mapOf(ETH_ID to healthySummary)),
        )
        val producer = createSummaryProducer(products = setOf(silent, healthy))

        // Act
        val frames = getEmittedValues(producer.produce())
        runCurrent()

        // Assert
        val expected = EarnProductSummaries(
            products = mapOf(ETH_ID to listOf(healthySummary)),
            rateSources = emptyMap(),
            source = StatusSource.ACTUAL,
        )
        assertThat(frames.last()).isEqualTo(expected)
    }

    @Test
    fun `GIVEN a rate source that completes without a value WHEN produce THEN the source is ACTUAL`() = runTest {
        // Arrange
        val rateSource = TestEarnRateSource(id = YIELD_ID, ratesFlow = emptyFlow())
        val producer = createSummaryProducer(rateSources = setOf(rateSource))

        // Act
        val frames = getEmittedValues(producer.produce())
        runCurrent()

        // Assert
        val expected = EarnProductSummaries(products = emptyMap(), rateSources = emptyMap(), source = StatusSource.ACTUAL)
        assertThat(frames.last()).isEqualTo(expected)
    }

    @Test
    fun `GIVEN a failed product WHEN its retry completes without a value THEN the cache is dropped`() = runTest {
        // Arrange
        val loadedSummary = createSummary(
            productId = FAILING_ID,
            position = EarnPositionSummary(cryptoAmount = BigDecimal.ONE),
        )
        val failureGate = MutableStateFlow(false)
        var collections = 0
        val failing = TestEarnProduct(
            id = FAILING_ID,
            rank = 0,
            summariesFlow = flow {
                if (collections++ == 0) {
                    emit(mapOf(ETH_ID to loadedSummary))
                    failureGate.first { it }
                    throw IllegalStateException("boom")
                }
            },
        )
        val producer = createSummaryProducer(products = setOf(failing))
        val frames = getEmittedValues(producer.produce())
        runCurrent()
        failureGate.value = true
        runCurrent()
        val afterFailure = frames.last()

        // Act
        advanceTimeBy(PRODUCER_RETRY_DELAY_MS + 1)
        runCurrent()

        // Assert
        val answeredEmpty = EarnProductSummaries(products = emptyMap(), rateSources = emptyMap(), source = StatusSource.ACTUAL)
        assertThat(afterFailure.source).isEqualTo(StatusSource.ONLY_CACHE)
        assertThat(frames.last()).isEqualTo(answeredEmpty)
    }

    @Test
    fun `GIVEN several products for one currency WHEN produce THEN sorted by active then rate then rank`() = runTest {
        // Arrange
        val active = EarnPositionSummary(cryptoAmount = BigDecimal.ONE)
        val inactiveLow = createSummary(productId = EarnProductId("inactive_low"), rate = createRate("0.03"))
        val inactiveBest = createSummary(productId = EarnProductId("inactive_best"), rate = createRate("0.08"))
        val activeLow = createSummary(
            productId = EarnProductId("active_low"),
            rate = createRate("0.02"),
            position = active,
        )
        val inactiveTied = createSummary(productId = EarnProductId("inactive_tied"), rate = createRate("0.08"))
        val activeHigh = createSummary(
            productId = EarnProductId("active_high"),
            rate = createRate("0.05"),
            position = active,
        )
        val noRate = createSummary(productId = EarnProductId("no_rate"), rate = null)
        val products = listOf(inactiveLow, inactiveBest, activeLow, inactiveTied, activeHigh, noRate)
            .mapIndexed { rank, summary ->
                TestEarnProduct(
                    id = summary.productId,
                    rank = rank,
                    summariesFlow = flowOf(mapOf(ETH_ID to summary)),
                )
            }
        val producer = createSummaryProducer(products = products.toSet())

        // Act
        val frames = getEmittedValues(producer.produce())
        runCurrent()

        // Assert
        val expected = EarnProductSummaries(
            products = mapOf(
                ETH_ID to listOf(activeHigh, activeLow, inactiveBest, inactiveTied, inactiveLow, noRate),
            ),
            rateSources = emptyMap(),
            source = StatusSource.ACTUAL,
        )
        assertThat(frames.last()).isEqualTo(expected)
    }

    @Test
    fun `GIVEN no products and no rate sources WHEN produce THEN ACTUAL with both maps empty`() = runTest {
        // Arrange
        val producer = createSummaryProducer()

        // Act
        val frames = getEmittedValues(producer.produce())
        runCurrent()

        // Assert
        val expected = EarnProductSummaries(
            products = emptyMap(),
            rateSources = emptyMap(),
            source = StatusSource.ACTUAL,
        )
        assertThat(frames).containsExactly(expected)
    }

    @Test
    fun `GIVEN a producer WHEN fallback is read THEN it is empty summaries with ONLY_CACHE`() = runTest {
        // Arrange
        val producer = createSummaryProducer()

        // Act
        val actual = producer.fallback

        // Assert
        val expected = EarnProductSummaries(
            products = emptyMap(),
            rateSources = emptyMap(),
            source = StatusSource.ONLY_CACHE,
        )
        assertSome(actual, expected)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SourceMerging {

        @Test
        fun `GIVEN all products ACTUAL WHEN one reports ONLY_CACHE THEN merged source turns ONLY_CACHE`() = runTest {
            // Arrange
            val steady = createSummary(productId = STEADY_ID, source = StatusSource.ACTUAL)
            val degrading = MutableStateFlow(
                mapOf(ETH_ID to createSummary(productId = DEGRADING_ID, source = StatusSource.ACTUAL)),
            )
            val products = setOf(
                TestEarnProduct(id = STEADY_ID, rank = 0, summariesFlow = flowOf(mapOf(ETH_ID to steady))),
                TestEarnProduct(id = DEGRADING_ID, rank = 1, summariesFlow = degrading),
            )
            val producer = createSummaryProducer(products = products)
            val frames = getEmittedValues(producer.produce())
            runCurrent()
            val sourceWhileAllActual = frames.last().source

            // Act
            val degraded = createSummary(productId = DEGRADING_ID, source = StatusSource.ONLY_CACHE)
            degrading.value = mapOf(ETH_ID to degraded)
            runCurrent()

            // Assert
            val expected = EarnProductSummaries(
                products = mapOf(ETH_ID to listOf(steady, degraded)),
                rateSources = emptyMap(),
                source = StatusSource.ONLY_CACHE,
            )
            assertThat(sourceWhileAllActual).isEqualTo(StatusSource.ACTUAL)
            assertThat(frames.last()).isEqualTo(expected)
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN products with sources WHEN produce THEN merged source is the weakest of them`(model: SourceModel) =
            runTest {
                // Arrange
                val first = createSummary(productId = STEADY_ID, source = model.first)
                val second = createSummary(productId = DEGRADING_ID, source = model.second)
                val products = setOf(
                    TestEarnProduct(id = STEADY_ID, rank = 0, summariesFlow = flowOf(mapOf(ETH_ID to first))),
                    TestEarnProduct(id = DEGRADING_ID, rank = 1, summariesFlow = flowOf(mapOf(ETH_ID to second))),
                )
                val producer = createSummaryProducer(products = products)

                // Act
                val frames = getEmittedValues(producer.produce())
                runCurrent()

                // Assert
                val expected = EarnProductSummaries(
                    products = mapOf(ETH_ID to listOf(first, second)),
                    rateSources = emptyMap(),
                    source = model.expected,
                )
                assertThat(frames.last()).isEqualTo(expected)
            }

        @Test
        fun `GIVEN a product answered without summaries WHEN produce THEN the answer counts as ACTUAL`() = runTest {
            // Arrange
            val answeredEmpty = TestEarnProduct(
                id = STEADY_ID,
                summariesFlow = flowOf(emptyMap<CryptoCurrency.ID, EarnProductSummary>()),
            )
            val producer = createSummaryProducer(products = setOf(answeredEmpty))

            // Act
            val frames = getEmittedValues(producer.produce())
            runCurrent()

            // Assert
            val expected = EarnProductSummaries(
                products = emptyMap(),
                rateSources = emptyMap(),
                source = StatusSource.ACTUAL,
            )
            assertThat(frames.last()).isEqualTo(expected)
        }

        private fun provideTestModels() = listOf(
            SourceModel(first = StatusSource.ACTUAL, second = StatusSource.ACTUAL, expected = StatusSource.ACTUAL),
            SourceModel(first = StatusSource.ACTUAL, second = StatusSource.CACHE, expected = StatusSource.CACHE),
            SourceModel(
                first = StatusSource.ACTUAL,
                second = StatusSource.ONLY_CACHE,
                expected = StatusSource.ONLY_CACHE,
            ),
            SourceModel(
                first = StatusSource.CACHE,
                second = StatusSource.ONLY_CACHE,
                expected = StatusSource.ONLY_CACHE,
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class RateSources {

        @Test
        fun `GIVEN a product and a rate source for one currency WHEN produce THEN each stays in its own map`() =
            runTest {
                // Arrange
                val productSummary = createSummary(productId = PRODUCT_ID, rate = createRate("0.04"))
                val product = TestEarnProduct(
                    id = PRODUCT_ID,
                    summariesFlow = flowOf(mapOf(ETH_ID to productSummary)),
                )
                val rate = createRate(fraction = "0.07", type = EarnRateType.APR)
                val route = TestRoute("yield")
                val rateSource = TestEarnRateSource(
                    id = YIELD_ID,
                    ratesFlow = flowOf(mapOf(ETH_ID to createRateEntry(rate = rate, route = route))),
                )
                val producer = createSummaryProducer(products = setOf(product), rateSources = setOf(rateSource))

                // Act
                val frames = getEmittedValues(producer.produce())
                runCurrent()

                // Assert
                val actual = frames.last()
                val expected = EarnProductSummaries(
                    products = mapOf(ETH_ID to listOf(productSummary)),
                    rateSources = mapOf(
                        ETH_ID to listOf(
                            createSummary(
                                productId = YIELD_ID,
                                rate = rate,
                                position = null,
                                route = route,
                                rateText = badgeConverter.convert(rate),
                                risk = null,
                                onClickAction = actual.rateSources.getValue(ETH_ID).single().onClickAction,
                            ),
                        ),
                    ),
                    source = StatusSource.ACTUAL,
                )
                val actualAction = actual.rateSources.getValue(ETH_ID).single().onClickAction
                assertThat(actual).isEqualTo(expected)
                assertThat(actualAction.id).isEqualTo("none")
                assertThat(actualAction.label).isEqualTo(TextReference.EMPTY)
            }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN a rate entry with a route WHEN produce THEN no risk, badge text, route kept, position if active`(
            model: RateEntryModel,
        ) = runTest {
            // Arrange
            val rate = createRate(fraction = "0.07", type = EarnRateType.APR)
            val route = TestRoute("yield")
            val entry = createRateEntry(rate = rate, isActive = model.isActive, route = route)
            val rateSource = TestEarnRateSource(id = YIELD_ID, ratesFlow = flowOf(mapOf(ETH_ID to entry)))
            val producer = createSummaryProducer(rateSources = setOf(rateSource))

            // Act
            val frames = getEmittedValues(producer.produce())
            runCurrent()

            // Assert
            val actual = frames.last()
            val expected = EarnProductSummaries(
                products = emptyMap(),
                rateSources = mapOf(
                    ETH_ID to listOf(
                        EarnProductSummary(
                            productId = YIELD_ID,
                            rateText = badgeConverter.convert(rate),
                            risk = null,
                            availability = ProductAvailability.Available,
                            rate = rate,
                            position = model.expectedPosition,
                            source = StatusSource.ACTUAL,
                            onClickAction = actual.rateSources.getValue(ETH_ID).single().onClickAction,
                            route = route,
                        ),
                    ),
                ),
                source = StatusSource.ACTUAL,
            )
            val actualAction = actual.rateSources.getValue(ETH_ID).single().onClickAction
            assertThat(actual).isEqualTo(expected)
            assertThat(actualAction.id).isEqualTo("none")
            assertThat(actualAction.label).isEqualTo(TextReference.EMPTY)
        }

        @Test
        fun `GIVEN several rate sources for one currency WHEN produce THEN sorted by active then rate then rank`() =
            runTest {
                // Arrange
                val entries = listOf(
                    EarnProductId("rank_0_rate_9") to createRateEntry(rate = createRate("0.09")),
                    EarnProductId("rank_1_active") to createRateEntry(rate = createRate("0.03"), isActive = true),
                    EarnProductId("rank_2_rate_9") to createRateEntry(rate = createRate("0.09")),
                    EarnProductId("rank_3_rate_10") to createRateEntry(rate = createRate("0.10")),
                )
                val rateSources = entries.mapIndexed { rank, (id, entry) ->
                    TestEarnRateSource(id = id, rank = rank, ratesFlow = flowOf(mapOf(ETH_ID to entry)))
                }
                val producer = createSummaryProducer(rateSources = rateSources.toSet())

                // Act
                val frames = getEmittedValues(producer.produce())
                runCurrent()

                // Assert
                val actual = frames.last().rateSources.getValue(ETH_ID).map { it.productId }
                assertThat(actual).containsExactly(
                    EarnProductId("rank_1_active"),
                    EarnProductId("rank_3_rate_10"),
                    EarnProductId("rank_0_rate_9"),
                    EarnProductId("rank_2_rate_9"),
                ).inOrder()
            }

        private fun provideTestModels() = listOf(
            RateEntryModel(isActive = true, expectedPosition = EarnPositionSummary(cryptoAmount = null)),
            RateEntryModel(isActive = false, expectedPosition = null),
        )
    }

    private fun TestScope.createSummaryProducer(
        products: Set<EarnProduct> = emptySet(),
        rateSources: Set<EarnRateSource> = emptySet(),
        userWalletId: UserWalletId = USER_WALLET_ID,
    ): DefaultEarnProductSummaryProducer {
        val registry = EarnProductsRegistry(products = products, rateSources = rateSources)

        return DefaultEarnProductSummaryProducer(
            params = EarnProductSummaryProducer.Params(userWalletId),
            flowProducerTools = TestFlowProducerTools(backgroundScope, StandardTestDispatcher(testScheduler)),
            registry = registry,
            order = EarnSummaryOrder(registry),
            rateBadgeConverter = EarnRateBadgeConverter(),
            dispatchers = createTestingCoroutineDispatcherProvider(),
        )
    }

    private fun installLogWriter() {
        every { logWriter.isLoggable(any(), any()) } returns true
        TangemLogger.setLogWriters(listOf(logWriter))
    }

    private fun throwingSummaries(cause: Throwable): Flow<Map<CryptoCurrency.ID, EarnProductSummary>> =
        flow { throw cause }

    internal data class SourceModel(val first: StatusSource, val second: StatusSource, val expected: StatusSource) {
        override fun toString(): String = "$first + $second -> $expected"
    }

    internal data class RateEntryModel(val isActive: Boolean, val expectedPosition: EarnPositionSummary?) {
        override fun toString(): String = if (isActive) "active" else "inactive"
    }

    private companion object {

        val PENDING_ID = EarnProductId("pending")
        val ANSWERED_ID = EarnProductId("answered")
        val FAILING_ID = EarnProductId("failing")
        val HEALTHY_ID = EarnProductId("healthy")
        val RECOVERING_ID = EarnProductId("recovering")
        val STEADY_ID = EarnProductId("steady")
        val DEGRADING_ID = EarnProductId("degrading")
        val PRODUCT_ID = EarnProductId("product")
        val YIELD_ID = EarnProductId("yield")
        val OTHER_WALLET_ID = UserWalletId("fedcba9876543210")
    }
}