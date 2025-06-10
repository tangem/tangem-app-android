package com.tangem.data.quotes.single

import com.google.common.truth.Truth
import com.tangem.common.test.utils.getEmittedValues
import com.tangem.data.quotes.store.QuotesStatusesStore
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.quotes.single.SingleQuoteProducer
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
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
internal class DefaultSingleQuoteStatusProducerTest {

    private val params = SingleQuoteProducer.Params(
        rawCurrencyId = CryptoCurrency.RawID(value = "BTC"),
    )

    private val quotesStore = mockk<QuotesStatusesStore>()

    private val producer = DefaultSingleQuoteProducer(
        params = params,
        quotesStore = quotesStore,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

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

        val actual = producer.produce()

        verify { quotesStore.get() }

        val values = getEmittedValues(flow = actual)

        Truth.assertThat(values.size).isEqualTo(1)
        Truth.assertThat(values).isEqualTo(listOf(status))
    }

    @Test
    fun `test that flow is updated if quote is updated`() = runTest {
        val storeQuote = MutableSharedFlow<Set<QuoteStatus>>(replay = 2, extraBufferCapacity = 1)

        every { quotesStore.get() } returns storeQuote

        val actual = producer.produceWithFallback()

        verify { quotesStore.get() }

        // first emit
        val status = QuoteStatus(rawCurrencyId = params.rawCurrencyId)
        storeQuote.emit(value = setOf(status))

        val values1 = getEmittedValues(flow = actual)

        Truth.assertThat(values1.size).isEqualTo(1)
        Truth.assertThat(values1).isEqualTo(listOf(status))

        // second emit
        val updatedStatus = QuoteStatus(
            rawCurrencyId = params.rawCurrencyId,
            value = QuoteStatus.Data(
                fiatRate = BigDecimal.ONE,
                priceChange = BigDecimal.ZERO,
                source = StatusSource.ACTUAL,
            ),
        )
        storeQuote.emit(value = setOf(updatedStatus))

        val values2 = getEmittedValues(flow = actual)

        Truth.assertThat(values2.size).isEqualTo(2)
        Truth.assertThat(values2).isEqualTo(listOf(status, updatedStatus))
    }

    @Test
    fun `test that flow is filtered the same status`() = runTest {
        val storeQuote = MutableSharedFlow<Set<QuoteStatus>>(replay = 2, extraBufferCapacity = 1)

        every { quotesStore.get() } returns storeQuote

        val actual = producer.produceWithFallback()

        verify { quotesStore.get() }

        // first emit
        val status = QuoteStatus(rawCurrencyId = params.rawCurrencyId)
        storeQuote.emit(value = setOf(status))

        val values1 = getEmittedValues(flow = actual)

        Truth.assertThat(values1.size).isEqualTo(1)
        Truth.assertThat(values1).isEqualTo(listOf(status))

        // second emit
        storeQuote.emit(value = setOf(status))

        val values2 = getEmittedValues(flow = actual)

        Truth.assertThat(values2.size).isEqualTo(1)
        Truth.assertThat(values2).isEqualTo(listOf(status))
    }

    @Test
    fun `test if flow throws exception`() = runTest {
        val exception = IllegalStateException()
        val status = QuoteStatus(
            rawCurrencyId = params.rawCurrencyId,
            value = QuoteStatus.Data(
                fiatRate = BigDecimal.ONE,
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

        val actual = producer.produceWithFallback()

        verify { quotesStore.get() }

        val values1 = getEmittedValues(flow = actual)

        Truth.assertThat(values1.size).isEqualTo(1)
        val fallbackStatus = QuoteStatus(rawCurrencyId = params.rawCurrencyId)
        Truth.assertThat(values1).isEqualTo(listOf(fallbackStatus))

        innerFlow.emit(value = true)

        val values2 = getEmittedValues(flow = actual)
        Truth.assertThat(values2.size).isEqualTo(1)
        Truth.assertThat(values2).isEqualTo(listOf(status))
    }

    @Test
    fun `test if flow doesn't contain network from params`() = runTest {
        val storeFlow = flowOf(
            setOf(
                QuoteStatus(rawCurrencyId = CryptoCurrency.RawID(value = "ETH")),
            ),
        )

        every { quotesStore.get() } returns storeFlow

        val actual = producer.produceWithFallback()

        verify { quotesStore.get() }

        val values = getEmittedValues(flow = actual)

        Truth.assertThat(values.size).isEqualTo(0)
    }
}