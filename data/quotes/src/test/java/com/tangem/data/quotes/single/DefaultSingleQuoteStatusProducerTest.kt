package com.tangem.data.quotes.single

import app.cash.turbine.test
import com.google.common.truth.Truth
import com.tangem.data.quotes.store.QuotesStatusesStore
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.quotes.single.SingleQuoteStatusProducer
import com.tangem.test.core.TestFlowProducerTools
import com.tangem.test.core.getEmittedValues
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class DefaultSingleQuoteStatusProducerTest {

    private val params = SingleQuoteStatusProducer.Params(
        rawCurrencyId = CryptoCurrency.RawID(value = "BTC"),
    )

    private val quotesStore = mockk<QuotesStatusesStore>()

    private fun TestScope.createProducer(): DefaultSingleQuoteStatusProducer {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        return DefaultSingleQuoteStatusProducer(
            params = params,
            quotesStatusesStore = quotesStore,
            flowProducerTools = TestFlowProducerTools(scope = backgroundScope, dispatcher = testDispatcher),
            dispatchers = TestingCoroutineDispatcherProvider(
                main = testDispatcher,
                mainImmediate = testDispatcher,
                io = testDispatcher,
                default = testDispatcher,
                single = testDispatcher,
            ),
        )
    }

    @Test
    fun `test that flow is mapped for network from params`() = runTest {
        val status = QuoteStatus(rawCurrencyId = params.rawCurrencyId)
        val storeQuote = flowOf(
            setOf(
                status,
                QuoteStatus(rawCurrencyId = CryptoCurrency.RawID(value = "ETH")),
            ),
        )

        every { quotesStore.get() } returns storeQuote

        val actual = createProducer().produce()

        verify { quotesStore.get() }

        val values = getEmittedValues(flow = actual)

        Truth.assertThat(values.size).isEqualTo(1)
        Truth.assertThat(values).isEqualTo(listOf(status))
    }

    @Test
    fun `test that flow is updated if quote is updated`() = runTest {
        val storeQuote = MutableSharedFlow<Set<QuoteStatus>>(replay = 2, extraBufferCapacity = 1)

        every { quotesStore.get() } returns storeQuote

        val actual = createProducer().produceWithFallback()

        verify { quotesStore.get() }

        actual.test {
            val status = QuoteStatus(rawCurrencyId = params.rawCurrencyId)
            storeQuote.emit(value = setOf(status))
            Truth.assertThat(awaitItem()).isEqualTo(status)

            val updatedStatus = QuoteStatus(
                rawCurrencyId = params.rawCurrencyId,
                value = QuoteStatus.Data(
                    fiatRate = BigDecimal.ONE,
                    priceChange = BigDecimal.ZERO,
                    fiatRateUSD = BigDecimal.ZERO,
                    source = StatusSource.ACTUAL,
                ),
            )
            storeQuote.emit(value = setOf(updatedStatus))
            Truth.assertThat(awaitItem()).isEqualTo(updatedStatus)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that flow is filtered the same status`() = runTest {
        val storeQuote = MutableSharedFlow<Set<QuoteStatus>>(replay = 2, extraBufferCapacity = 1)

        every { quotesStore.get() } returns storeQuote

        val actual = createProducer().produceWithFallback()

        verify { quotesStore.get() }

        actual.test {
            val status = QuoteStatus(rawCurrencyId = params.rawCurrencyId)
            storeQuote.emit(value = setOf(status))
            Truth.assertThat(awaitItem()).isEqualTo(status)

            // same status again -> filtered out by distinctUntilChanged
            storeQuote.emit(value = setOf(status))
            expectNoEvents()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test if flow throws exception`() = runTest {
        val exception = IllegalStateException()
        val status = QuoteStatus(
            rawCurrencyId = params.rawCurrencyId,
            value = QuoteStatus.Data(
                fiatRate = BigDecimal.ONE,
                fiatRateUSD = BigDecimal.ZERO,
                priceChange = BigDecimal.ZERO,
                source = StatusSource.ACTUAL,
            ),
        )

        val innerFlow = MutableStateFlow(value = false)
        val storeQuote = flow {
            if (innerFlow.value) {
                emit(setOf(status))
            } else {
                throw exception
            }
        }
            .buffer(capacity = 5)

        every { quotesStore.get() } returns storeQuote

        val actual = createProducer().produceWithFallback()

        verify { quotesStore.get() }

        actual.test {
            val fallbackStatus = QuoteStatus(rawCurrencyId = params.rawCurrencyId)
            Truth.assertThat(awaitItem()).isEqualTo(fallbackStatus)

            innerFlow.value = true
            advanceTimeBy(delayTimeMillis = 2001)
            runCurrent()

            Truth.assertThat(awaitItem()).isEqualTo(status)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test if flow doesn't contain network from params`() = runTest {
        val storeFlow = flowOf(
            setOf(
                QuoteStatus(rawCurrencyId = CryptoCurrency.RawID(value = "ETH")),
            ),
        )

        every { quotesStore.get() } returns storeFlow

        val actual = createProducer().produceWithFallback()

        verify { quotesStore.get() }

        actual.test {
            // params currency (BTC) is not in the store -> nothing is emitted
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }
}