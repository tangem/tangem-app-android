package com.tangem.data.quotes.store

import com.google.common.truth.Truth
import com.tangem.common.test.data.quote.MockQuoteResponseFactory
import com.tangem.common.test.data.quote.toDomain
import com.tangem.common.test.datastore.MockStateDataStore
import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.domain.models.StatusSource
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Quote
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
internal class QuotesStoreUpdateMethodsTest {

    private val runtimeStore = RuntimeSharedStore<Set<Quote>>()
    private val persistenceStore = MockStateDataStore<CurrencyIdWithQuote>(default = emptyMap())

    private val store = DefaultQuotesStoreV2(
        runtimeStore = runtimeStore,
        persistenceDataStore = persistenceStore,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @Test
    fun `refresh if runtime store is empty`() = runTest {
        val currenciesIds = setOf(
            CryptoCurrency.RawID(value = "BTC"),
            CryptoCurrency.RawID(value = "ETH"),
        )

        store.refresh(currenciesIds = currenciesIds)

        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(emptySet<Quote>())
        Truth.assertThat(persistenceStore.data.firstOrNull()).isEqualTo(emptyMap<String, Set<Quote>>())
    }

    @Test
    fun `refresh if runtime store contains quote with this id`() = runTest {
        val quote = MockQuoteResponseFactory.createSinglePrice(value = BigDecimal.ONE)
            .toDomain(rawCurrencyId = "BTC", source = StatusSource.ACTUAL)

        runtimeStore.store(value = setOf(quote))

        store.refresh(currenciesIds = setOf(quote.rawCurrencyId))

        val runtimeExpected = setOf(quote.copySealed(source = StatusSource.CACHE))

        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(runtimeExpected)
        Truth.assertThat(persistenceStore.data.firstOrNull()).isEqualTo(emptyMap<String, Set<Quote>>())
    }

    @Test
    fun `store actual if runtime and cache stores contain quotes with this id`() = runTest {
        val prevStatus = MockQuoteResponseFactory.createSinglePrice(value = BigDecimal.ONE)

        runtimeStore.store(
            value = setOf(
                prevStatus.toDomain(rawCurrencyId = "BTC", source = StatusSource.ONLY_CACHE),
            ),
        )

        persistenceStore.updateData {
            it.toMutableMap().apply {
                put("BTC", prevStatus)
            }
        }

        val newStatus = MockQuoteResponseFactory.createSinglePrice(value = BigDecimal.TEN)

        store.storeActual(values = mapOf("BTC" to newStatus))

        val runtimeExpected = setOf(
            newStatus.toDomain(rawCurrencyId = "BTC", source = StatusSource.ACTUAL),
        )
        val persistenceExpected = mapOf("BTC" to newStatus)

        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(runtimeExpected)
        Truth.assertThat(persistenceStore.data.firstOrNull()).isEqualTo(persistenceExpected)
    }

    @Test
    fun `store error if runtime store is empty`() = runTest {
        val currenciesIds = setOf(
            CryptoCurrency.RawID(value = "BTC"),
            CryptoCurrency.RawID(value = "ETH"),
        )

        store.storeError(currenciesIds = currenciesIds)

        val runtimeExpected = currenciesIds.map(Quote::Empty).toSet()

        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(runtimeExpected)
        Truth.assertThat(persistenceStore.data.firstOrNull()).isEqualTo(emptyMap<String, Set<Quote>>())
    }

    @Test
    fun `store error if runtime store contains status with this network`() = runTest {
        val status = MockQuoteResponseFactory.createSinglePrice(value = BigDecimal.ONE)

        runtimeStore.store(
            value = setOf(
                status.toDomain(rawCurrencyId = "BTC", source = StatusSource.CACHE),
                Quote.Empty(rawCurrencyId = CryptoCurrency.RawID(value = "ETH")),
            ),
        )

        store.storeError(
            currenciesIds = setOf(
                CryptoCurrency.RawID(value = "BTC"),
                CryptoCurrency.RawID(value = "ETH"),
            ),
        )

        val runtimeExpected = setOf(
            status.toDomain(rawCurrencyId = "BTC", source = StatusSource.ONLY_CACHE),
            Quote.Empty(rawCurrencyId = CryptoCurrency.RawID(value = "ETH")),
        )

        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(runtimeExpected)
        Truth.assertThat(persistenceStore.data.firstOrNull()).isEqualTo(emptyMap<String, Set<Quote>>())
    }
}