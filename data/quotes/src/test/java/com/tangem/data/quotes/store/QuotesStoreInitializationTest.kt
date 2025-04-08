package com.tangem.data.quotes.store

import androidx.datastore.core.DataStore
import com.google.common.truth.Truth
import com.tangem.common.test.data.quote.MockQuoteResponseFactory
import com.tangem.common.test.data.quote.toDomain
import com.tangem.common.test.datastore.MockStateDataStore
import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.domain.models.StatusSource
import com.tangem.domain.tokens.model.Quote
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
internal class QuotesStoreInitializationTest {

    @Test
    fun `test initialization if cache store is empty`() = runTest {
        val runtimeStore = RuntimeSharedStore<Set<Quote>>()
        val persistenceStore: DataStore<CurrencyIdWithQuote> = mockk()

        every { persistenceStore.data } returns emptyFlow()

        DefaultQuotesStoreV2(
            runtimeStore = runtimeStore,
            persistenceDataStore = persistenceStore,
            dispatchers = TestingCoroutineDispatcherProvider(),
        )

        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(null)
    }

    @Test
    fun `test initialization if cache store contains empty map`() = runTest {
        val runtimeStore = RuntimeSharedStore<Set<Quote>>()
        val persistenceStore = MockStateDataStore<CurrencyIdWithQuote>(default = emptyMap())

        DefaultQuotesStoreV2(
            runtimeStore = runtimeStore,
            persistenceDataStore = persistenceStore,
            dispatchers = TestingCoroutineDispatcherProvider(),
        )

        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(null)
    }

    @Test
    fun `test initialization if cache store is not empty`() = runTest {
        val runtimeStore = RuntimeSharedStore<Set<Quote>>()
        val persistenceStore = MockStateDataStore<CurrencyIdWithQuote>(default = emptyMap())

        val btcQuote = "BTC" to MockQuoteResponseFactory.createSinglePrice(BigDecimal.ZERO)
        val ethQuote = "ETH" to MockQuoteResponseFactory.createSinglePrice(BigDecimal.ONE)

        persistenceStore.updateData {
            it.toMutableMap().apply {
                this += btcQuote
                this += ethQuote
            }
        }

        DefaultQuotesStoreV2(
            runtimeStore = runtimeStore,
            persistenceDataStore = persistenceStore,
            dispatchers = TestingCoroutineDispatcherProvider(),
        )

        val expected = setOf(
            btcQuote.toDomain(source = StatusSource.CACHE),
            ethQuote.toDomain(source = StatusSource.CACHE),
        )

        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(expected)
    }
}