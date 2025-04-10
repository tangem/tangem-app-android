package com.tangem.data.quotes.store

import com.google.common.truth.Truth
import com.tangem.common.test.data.quote.MockQuoteResponseFactory
import com.tangem.common.test.data.quote.toDomain
import com.tangem.common.test.datastore.MockStateDataStore
import com.tangem.common.test.utils.getEmittedValues
import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.domain.tokens.model.Quote
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
internal class QuotesStoreGetMethodTest {

    private val runtimeStore = RuntimeSharedStore<Set<Quote>>()
    private val persistenceStore = MockStateDataStore<CurrencyIdWithQuote>(default = emptyMap())

    private val store = DefaultQuotesStoreV2(
        runtimeStore = runtimeStore,
        persistenceDataStore = persistenceStore,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @Test
    fun `test get if runtime store is empty`() = runTest {
        val actual = store.get()

        val values = backgroundScope.getEmittedValues(testScheduler, actual)

        Truth.assertThat(values).isEqualTo(emptyList<Set<Quote>>())
    }

    @Test
    fun `test get if runtime store contains empty set`() = runTest {
        runtimeStore.store(value = emptySet())

        val actual = store.get()

        val values = backgroundScope.getEmittedValues(testScheduler, actual)

        Truth.assertThat(values).isEqualTo(listOf(emptySet<Quote>()))
    }

    @Test
    fun `test get if runtime store is not empty`() = runTest {
        val btcQuote = "BTC" to MockQuoteResponseFactory.createSinglePrice(BigDecimal.ZERO)
        val ethQuote = "ETH" to MockQuoteResponseFactory.createSinglePrice(BigDecimal.ONE)

        runtimeStore.store(value = setOf(btcQuote.toDomain(), ethQuote.toDomain()))

        val actual = store.get()

        val values = backgroundScope.getEmittedValues(testScheduler, actual)

        Truth.assertThat(values.size).isEqualTo(1)
        Truth.assertThat(values).isEqualTo(listOf(setOf(btcQuote.toDomain(), ethQuote.toDomain())))
    }
}