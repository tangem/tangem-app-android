package com.tangem.data.quotes.single

import com.google.common.truth.Truth
import com.tangem.common.test.utils.getEmittedValues
import com.tangem.data.quotes.store.QuotesStoreV2
import com.tangem.domain.models.StatusSource
import com.tangem.domain.quotes.single.SingleQuoteProducer
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Quote
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
internal class DefaultSingleQuoteProducerTest {

    private val params = SingleQuoteProducer.Params(
        rawCurrencyId = CryptoCurrency.RawID(value = "BTC"),
    )

    private val quotesStore = mockk<QuotesStoreV2>()

    private val producer = DefaultSingleQuoteProducer(
        params = params,
        quotesStore = quotesStore,
    )

    @Test
    fun `test that flow is mapped for network from params`() = runTest {
        val status = Quote.Empty(rawCurrencyId = params.rawCurrencyId)
        val storeQuote = flowOf(
            setOf(
                status,
                Quote.Empty(rawCurrencyId = CryptoCurrency.RawID(value = "ETH")),
            ),
        )

        every { quotesStore.get() } returns storeQuote

        val actual = producer.produce()

        verify { quotesStore.get() }

        val values = backgroundScope.getEmittedValues(testScheduler = testScheduler, actual = actual)

        Truth.assertThat(values.size).isEqualTo(1)
        Truth.assertThat(values).isEqualTo(listOf(status))
    }

    @Test
    fun `test that flow is updated if quote is updated`() = runTest {
        val storeQuote = MutableSharedFlow<Set<Quote>>(replay = 2, extraBufferCapacity = 1)

        every { quotesStore.get() } returns storeQuote

        val actual = producer.produceWithFallback()

        verify { quotesStore.get() }

        // first emit
        val status = Quote.Empty(rawCurrencyId = params.rawCurrencyId)
        storeQuote.emit(value = setOf(status))

        val values1 = backgroundScope.getEmittedValues(testScheduler = testScheduler, actual = actual)

        Truth.assertThat(values1.size).isEqualTo(1)
        Truth.assertThat(values1).isEqualTo(listOf(status))

        // second emit
        val updatedStatus = Quote.Value(
            rawCurrencyId = params.rawCurrencyId,
            fiatRate = BigDecimal.ONE,
            priceChange = BigDecimal.ZERO,
            source = StatusSource.ACTUAL,
        )
        storeQuote.emit(value = setOf(updatedStatus))

        val values2 = backgroundScope.getEmittedValues(testScheduler = testScheduler, actual = actual)

        Truth.assertThat(values2.size).isEqualTo(2)
        Truth.assertThat(values2).isEqualTo(listOf(status, updatedStatus))
    }

    @Test
    fun `test that flow is filtered the same status`() = runTest {
        val storeQuote = MutableSharedFlow<Set<Quote>>(replay = 2, extraBufferCapacity = 1)

        every { quotesStore.get() } returns storeQuote

        val actual = producer.produceWithFallback()

        verify { quotesStore.get() }

        // first emit
        val status = Quote.Empty(rawCurrencyId = params.rawCurrencyId)
        storeQuote.emit(value = setOf(status))

        val values1 = backgroundScope.getEmittedValues(testScheduler = testScheduler, actual = actual)

        Truth.assertThat(values1.size).isEqualTo(1)
        Truth.assertThat(values1).isEqualTo(listOf(status))

        // second emit
        storeQuote.emit(value = setOf(status))

        val values2 = backgroundScope.getEmittedValues(testScheduler = testScheduler, actual = actual)

        Truth.assertThat(values2.size).isEqualTo(1)
        Truth.assertThat(values2).isEqualTo(listOf(status))
    }

    @Test
    fun `test if flow throws exception`() = runTest {
        val exception = IllegalStateException()
        val status = Quote.Value(
            rawCurrencyId = params.rawCurrencyId,
            fiatRate = BigDecimal.ONE,
            priceChange = BigDecimal.ZERO,
            source = StatusSource.ACTUAL,
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

        val actual = producer.produceWithFallback()

        verify { quotesStore.get() }

        val values1 = backgroundScope.getEmittedValues(testScheduler = testScheduler, actual = actual)

        Truth.assertThat(values1.size).isEqualTo(1)
        val fallbackStatus = Quote.Empty(rawCurrencyId = params.rawCurrencyId)
        Truth.assertThat(values1).isEqualTo(listOf(fallbackStatus))

        innerFlow.emit(value = true)

        val values2 = backgroundScope.getEmittedValues(testScheduler = testScheduler, actual = actual)
        Truth.assertThat(values2.size).isEqualTo(1)
        Truth.assertThat(values2).isEqualTo(listOf(status))
    }

    @Test
    fun `test if flow doesn't contain network from params`() = runTest {
        val storeFlow = flowOf(
            setOf(
                Quote.Empty(rawCurrencyId = CryptoCurrency.RawID(value = "ETH")),
            ),
        )

        every { quotesStore.get() } returns storeFlow

        val actual = producer.produceWithFallback()

        verify { quotesStore.get() }

        val values = backgroundScope.getEmittedValues(testScheduler = testScheduler, actual = actual)

        Truth.assertThat(values.size).isEqualTo(0)
    }
}