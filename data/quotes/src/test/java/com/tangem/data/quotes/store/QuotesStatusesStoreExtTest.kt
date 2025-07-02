package com.tangem.data.quotes.store

import com.google.common.truth.Truth
import com.tangem.common.test.data.quote.MockQuoteResponseFactory
import com.tangem.common.test.data.quote.toDomain
import com.tangem.common.test.datastore.MockStateDataStore
import com.tangem.common.test.utils.ProvideTestModels
import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import kotlin.properties.Delegates

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class QuotesStatusesStoreExtTest {

    private var runtimeStore: RuntimeSharedStore<Set<QuoteStatus>> by Delegates.notNull()
    private var persistenceStore: MockStateDataStore<CurrencyIdWithQuote> by Delegates.notNull()
    private var store: DefaultQuotesStatusesStore by Delegates.notNull()

    private val btcQuoteDM = "BTC" to MockQuoteResponseFactory.createSinglePrice(BigDecimal.ZERO)
    private val btcQuote = btcQuoteDM.toDomain()

    @BeforeEach
    fun resetMocks() {
        runtimeStore = RuntimeSharedStore()
        persistenceStore = MockStateDataStore(default = emptyMap())

        store = DefaultQuotesStatusesStore(
            runtimeStore = runtimeStore,
            persistenceDataStore = persistenceStore,
            dispatchers = TestingCoroutineDispatcherProvider(),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SetSourceAsCache {

        @ParameterizedTest
        @ProvideTestModels
        fun setSourceAsCache(model: SetSourceTestModel) = runTest {
            // Arrange
            if (model.initialRuntime != null) {
                runtimeStore.store(value = model.initialRuntime)
            }

            // Act
            store.setSourceAsCache(model.currenciesIds)
            val actual = store.getAllSyncOrNull()

            // Assert
            val expected = model.expected

            Truth.assertThat(actual).isEqualTo(expected)
        }

        private fun provideTestModels() = listOf(
            SetSourceTestModel(
                initialRuntime = null,
                currenciesIds = setOf(btcQuote.rawCurrencyId),
                expected = emptySet(),
            ),
            SetSourceTestModel(
                initialRuntime = setOf(btcQuote),
                currenciesIds = setOf(btcQuote.rawCurrencyId),
                expected = setOf(btcQuoteDM.toDomain(source = StatusSource.CACHE)),
            ),
            SetSourceTestModel(
                initialRuntime = setOf(btcQuoteDM.toDomain(source = StatusSource.CACHE)),
                currenciesIds = setOf(btcQuote.rawCurrencyId),
                expected = setOf(btcQuoteDM.toDomain(source = StatusSource.CACHE)),
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SetSourceAsOnlyCache {

        @ParameterizedTest
        @ProvideTestModels
        fun setSourceAsOnlyCache(model: SetSourceTestModel) = runTest {
            // Arrange
            if (model.initialRuntime != null) {
                runtimeStore.store(value = model.initialRuntime)
            }

            // Act
            store.setSourceAsOnlyCache(model.currenciesIds)
            val actual = store.getAllSyncOrNull()

            // Assert
            val expected = model.expected

            Truth.assertThat(actual).isEqualTo(expected)
        }

        private fun provideTestModels() = listOf(
            SetSourceTestModel(
                initialRuntime = null,
                currenciesIds = setOf(btcQuote.rawCurrencyId),
                expected = setOf(QuoteStatus(btcQuote.rawCurrencyId)),
            ),
            SetSourceTestModel(
                initialRuntime = setOf(btcQuote),
                currenciesIds = setOf(btcQuote.rawCurrencyId),
                expected = setOf(btcQuoteDM.toDomain(source = StatusSource.ONLY_CACHE)),
            ),
            SetSourceTestModel(
                initialRuntime = setOf(btcQuoteDM.toDomain(source = StatusSource.ONLY_CACHE)),
                currenciesIds = setOf(btcQuote.rawCurrencyId),
                expected = setOf(btcQuoteDM.toDomain(source = StatusSource.ONLY_CACHE)),
            ),
        )
    }

    data class SetSourceTestModel(
        val initialRuntime: Set<QuoteStatus>?,
        val currenciesIds: Set<CryptoCurrency.RawID>,
        val expected: Set<QuoteStatus>?,
    )
}