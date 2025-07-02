package com.tangem.data.quotes.store

import androidx.datastore.core.DataStore
import com.google.common.truth.Truth
import com.tangem.common.test.data.quote.MockQuoteResponseFactory
import com.tangem.common.test.data.quote.toDomain
import com.tangem.common.test.datastore.MockStateDataStore
import com.tangem.common.test.utils.ProvideTestModels
import com.tangem.common.test.utils.getEmittedValues
import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import kotlin.properties.Delegates

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class QuotesStatusesStoreTest {

    private var runtimeStore: RuntimeSharedStore<Set<QuoteStatus>> by Delegates.notNull()
    private var persistenceStore: MockStateDataStore<CurrencyIdWithQuote> by Delegates.notNull()
    private var store: DefaultQuotesStatusesStore by Delegates.notNull()

    // region Data models
    private val btcQuoteDM = "BTC" to MockQuoteResponseFactory.createSinglePrice(BigDecimal.ZERO)
    private val ethQuoteDM = "ETH" to MockQuoteResponseFactory.createSinglePrice(BigDecimal.ONE)
    // endregion

    // region Domain models
    private val btcQuote = btcQuoteDM.toDomain()
    private val ethQuote = ethQuoteDM.toDomain()
    private val adaEmptyQuote = QuoteStatus(rawCurrencyId = CryptoCurrency.RawID(value = "ADA"))
    // endregion

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
    inner class Initialization {

        @Test
        fun `initialization if cache store is empty`() = runTest {
            // Arrange
            val runtimeStore = RuntimeSharedStore<Set<QuoteStatus>>()
            val persistenceStore: DataStore<CurrencyIdWithQuote> = mockk()

            every { persistenceStore.data } returns emptyFlow()

            // Act
            DefaultQuotesStatusesStore(
                runtimeStore = runtimeStore,
                persistenceDataStore = persistenceStore,
                dispatchers = TestingCoroutineDispatcherProvider(),
            )

            val actual = runtimeStore.getSyncOrNull()

            // Assert
            val expected = null
            Truth.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `initialization if cache store contains empty map`() = runTest {
            // Arrange
            val runtimeStore = RuntimeSharedStore<Set<QuoteStatus>>()
            val persistenceStore = MockStateDataStore<CurrencyIdWithQuote>(default = emptyMap())

            // Act
            DefaultQuotesStatusesStore(
                runtimeStore = runtimeStore,
                persistenceDataStore = persistenceStore,
                dispatchers = TestingCoroutineDispatcherProvider(),
            )

            val actual = runtimeStore.getSyncOrNull()

            // Assert
            val expected = null
            Truth.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `initialization if cache store is not empty`() = runTest {
            // Arrange
            val runtimeStore = RuntimeSharedStore<Set<QuoteStatus>>()
            val persistenceStore = MockStateDataStore<CurrencyIdWithQuote>(default = emptyMap())

            persistenceStore.updateData {
                it.toMutableMap().apply {
                    this += btcQuoteDM
                    this += ethQuoteDM
                }
            }

            // Act
            DefaultQuotesStatusesStore(
                runtimeStore = runtimeStore,
                persistenceDataStore = persistenceStore,
                dispatchers = TestingCoroutineDispatcherProvider(),
            )

            val actual = runtimeStore.getSyncOrNull()

            // Assert
            val expected = setOf(
                btcQuoteDM.toDomain(source = StatusSource.CACHE),
                ethQuoteDM.toDomain(source = StatusSource.CACHE),
            )

            Truth.assertThat(actual).isEqualTo(expected)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Get {

        @ParameterizedTest
        @ProvideTestModels
        fun get(model: GetTestModel) = runTest {
            // Arrange
            if (model.initialRuntime != null) {
                runtimeStore.store(value = model.initialRuntime)
            }

            // Act
            val actual = getEmittedValues(flow = store.get())

            // Assert
            val expected = listOfNotNull(model.expected)

            Truth.assertThat(actual).isEqualTo(expected)
        }

        private fun provideTestModels() = listOf(
            GetTestModel(initialRuntime = null, expected = null),
            GetTestModel(initialRuntime = emptySet(), expected = emptySet()),
            GetTestModel(initialRuntime = setOf(btcQuote, ethQuote), expected = setOf(btcQuote, ethQuote)),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetAllSyncOrNull {

        @ParameterizedTest
        @ProvideTestModels
        fun getAllSyncOrNull(model: GetTestModel) = runTest {
            // Arrange
            if (model.initialRuntime != null) {
                runtimeStore.store(value = model.initialRuntime)
            }

            // Act
            val actual = store.getAllSyncOrNull()

            // Assert
            val expected = model.expected

            Truth.assertThat(actual).isEqualTo(expected)
        }

        private fun provideTestModels() = listOf(
            GetTestModel(initialRuntime = null, expected = null),
            GetTestModel(initialRuntime = emptySet(), expected = emptySet()),
            GetTestModel(initialRuntime = setOf(btcQuote, ethQuote), expected = setOf(btcQuote, ethQuote)),
        )
    }

    data class GetTestModel(val initialRuntime: Set<QuoteStatus>?, val expected: Set<QuoteStatus>?)

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SingleUpdateStatusSource {

        @ParameterizedTest
        @ProvideTestModels
        fun updateStatusSource(model: UpdateStatusSourceModel.Single) = runTest {
            // Arrange
            if (model.initialRuntime != null) {
                runtimeStore.store(value = model.initialRuntime)
            }

            // Act
            store.updateStatusSource(
                currencyId = model.currencyId,
                source = model.source,
                ifNotFound = model.ifNotFound,
            )

            val actual = store.getAllSyncOrNull()

            // Assert
            val expected = model.expected

            Truth.assertThat(actual).isEqualTo(expected)
        }

        private fun provideTestModels() = listOf(
            // region runtime store is null
            UpdateStatusSourceModel.Single(
                initialRuntime = null,
                currencyId = btcQuote.rawCurrencyId,
                source = StatusSource.CACHE, // never-mind
                expected = emptySet(),
            ),
            UpdateStatusSourceModel.Single(
                initialRuntime = null,
                currencyId = btcQuote.rawCurrencyId,
                source = StatusSource.ONLY_CACHE, // never-mind
                ifNotFound = ::QuoteStatus,
                expected = setOf(
                    QuoteStatus(rawCurrencyId = btcQuote.rawCurrencyId),
                ),
            ),
            // endregion

            // region runtime store is empty
            UpdateStatusSourceModel.Single(
                initialRuntime = emptySet(),
                currencyId = btcQuote.rawCurrencyId,
                source = StatusSource.CACHE, // never-mind
                expected = emptySet(),
            ),
            UpdateStatusSourceModel.Single(
                initialRuntime = emptySet(),
                currencyId = btcQuote.rawCurrencyId,
                source = StatusSource.ONLY_CACHE, // never-mind
                ifNotFound = ::QuoteStatus,
                expected = setOf(
                    QuoteStatus(rawCurrencyId = btcQuote.rawCurrencyId),
                ),
            ),
            // endregion

            // region runtime store contains statuses
            UpdateStatusSourceModel.Single(
                initialRuntime = setOf(btcQuote, ethQuote, adaEmptyQuote),
                currencyId = btcQuote.rawCurrencyId,
                source = StatusSource.CACHE,
                expected = setOf(
                    btcQuote.copy(value = btcQuote.value.copySealed(source = StatusSource.CACHE)),
                    ethQuote,
                    adaEmptyQuote,
                ),
            ),
            UpdateStatusSourceModel.Single(
                initialRuntime = setOf(btcQuote, ethQuote),
                currencyId = btcQuote.rawCurrencyId,
                source = StatusSource.ONLY_CACHE,
                expected = setOf(
                    btcQuote.copy(value = btcQuote.value.copySealed(source = StatusSource.ONLY_CACHE)),
                    ethQuote,
                ),
            ),
            UpdateStatusSourceModel.Single(
                initialRuntime = setOf(btcQuote, ethQuote),
                currencyId = btcQuote.rawCurrencyId,
                source = StatusSource.ACTUAL,
                expected = setOf(btcQuote, ethQuote),
            ),
            UpdateStatusSourceModel.Single(
                initialRuntime = setOf(btcQuote),
                currencyId = adaEmptyQuote.rawCurrencyId,
                source = StatusSource.ACTUAL,
                ifNotFound = ::QuoteStatus,
                expected = setOf(btcQuote, adaEmptyQuote),
            ),
            // endregion
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class MultiUpdateStatusSource {

        @ParameterizedTest
        @ProvideTestModels
        fun updateStatusSource(model: UpdateStatusSourceModel.Multi) = runTest {
            // Arrange
            if (model.initialRuntime != null) {
                runtimeStore.store(value = model.initialRuntime)
            }

            // Act
            store.updateStatusSource(
                currenciesIds = model.currenciesIds,
                source = model.source,
                ifNotFound = model.ifNotFound,
            )

            val actual = store.getAllSyncOrNull()

            // Assert
            val expected = model.expected

            Truth.assertThat(actual).isEqualTo(expected)
        }

        private fun provideTestModels() = listOf(
            // region runtime store is null
            UpdateStatusSourceModel.Multi(
                initialRuntime = null,
                currenciesIds = setOf(),
                source = StatusSource.CACHE, // never-mind
                expected = null,
            ),
            UpdateStatusSourceModel.Multi(
                initialRuntime = null,
                currenciesIds = setOf(btcQuote.rawCurrencyId),
                source = StatusSource.ONLY_CACHE, // never-mind
                expected = emptySet(),
            ),
            UpdateStatusSourceModel.Multi(
                initialRuntime = null,
                currenciesIds = setOf(
                    btcQuote.rawCurrencyId,
                    CryptoCurrency.RawID(value = "ETH"),
                ),
                source = StatusSource.ACTUAL, // never-mind
                expected = emptySet(),
            ),
            UpdateStatusSourceModel.Multi(
                initialRuntime = null,
                currenciesIds = setOf(btcQuote.rawCurrencyId),
                source = StatusSource.ONLY_CACHE, // never-mind
                ifNotFound = ::QuoteStatus,
                expected = setOf(
                    QuoteStatus(rawCurrencyId = btcQuote.rawCurrencyId),
                ),
            ),
            // endregion

            // region runtime store is empty
            UpdateStatusSourceModel.Multi(
                initialRuntime = emptySet(),
                currenciesIds = setOf(),
                source = StatusSource.CACHE, // never-mind
                expected = emptySet(),
            ),
            UpdateStatusSourceModel.Multi(
                initialRuntime = emptySet(),
                currenciesIds = setOf(btcQuote.rawCurrencyId),
                source = StatusSource.ONLY_CACHE, // never-mind
                expected = emptySet(),
            ),
            UpdateStatusSourceModel.Multi(
                initialRuntime = emptySet(),
                currenciesIds = setOf(
                    btcQuote.rawCurrencyId,
                    CryptoCurrency.RawID(value = "ETH"),
                ),
                source = StatusSource.ACTUAL, // never-mind
                expected = emptySet(),
            ),
            UpdateStatusSourceModel.Multi(
                initialRuntime = emptySet(),
                currenciesIds = setOf(btcQuote.rawCurrencyId),
                source = StatusSource.ONLY_CACHE, // never-mind
                ifNotFound = ::QuoteStatus,
                expected = setOf(
                    QuoteStatus(rawCurrencyId = btcQuote.rawCurrencyId),
                ),
            ),
            // endregion

            // region runtime store contains statuses
            UpdateStatusSourceModel.Multi(
                initialRuntime = setOf(btcQuote, adaEmptyQuote),
                currenciesIds = setOf(btcQuote.rawCurrencyId, adaEmptyQuote.rawCurrencyId),
                source = StatusSource.CACHE,
                expected = setOf(
                    btcQuote.copy(value = btcQuote.value.copySealed(source = StatusSource.CACHE)),
                    adaEmptyQuote,
                ),
            ),
            UpdateStatusSourceModel.Multi(
                initialRuntime = setOf(btcQuote, adaEmptyQuote),
                currenciesIds = setOf(btcQuote.rawCurrencyId, adaEmptyQuote.rawCurrencyId),
                source = StatusSource.ONLY_CACHE,
                expected = setOf(
                    btcQuote.copy(value = btcQuote.value.copySealed(source = StatusSource.ONLY_CACHE)),
                    adaEmptyQuote,
                ),
            ),
            UpdateStatusSourceModel.Multi(
                initialRuntime = setOf(btcQuote, adaEmptyQuote),
                currenciesIds = setOf(btcQuote.rawCurrencyId, adaEmptyQuote.rawCurrencyId),
                source = StatusSource.ACTUAL,
                expected = setOf(btcQuote, adaEmptyQuote),
            ),
            UpdateStatusSourceModel.Multi(
                initialRuntime = setOf(btcQuote),
                currenciesIds = setOf(btcQuote.rawCurrencyId, adaEmptyQuote.rawCurrencyId),
                source = StatusSource.ACTUAL,
                expected = setOf(btcQuote),
            ),
            UpdateStatusSourceModel.Multi(
                initialRuntime = setOf(btcQuote),
                currenciesIds = setOf(btcQuote.rawCurrencyId, adaEmptyQuote.rawCurrencyId),
                source = StatusSource.ACTUAL,
                ifNotFound = ::QuoteStatus,
                expected = setOf(btcQuote, adaEmptyQuote),
            ),
            // endregion
        )
    }

    sealed interface UpdateStatusSourceModel {

        val initialRuntime: Set<QuoteStatus>?
        val source: StatusSource
        val ifNotFound: (CryptoCurrency.RawID) -> QuoteStatus?
        val expected: Set<QuoteStatus>?

        data class Single(
            override val initialRuntime: Set<QuoteStatus>?,
            val currencyId: CryptoCurrency.RawID,
            override val source: StatusSource,
            override val ifNotFound: (CryptoCurrency.RawID) -> QuoteStatus? = { null },
            override val expected: Set<QuoteStatus>?,
        ) : UpdateStatusSourceModel

        data class Multi(
            override val initialRuntime: Set<QuoteStatus>?,
            val currenciesIds: Set<CryptoCurrency.RawID>,
            override val source: StatusSource,
            override val ifNotFound: (CryptoCurrency.RawID) -> QuoteStatus? = { null },
            override val expected: Set<QuoteStatus>?,
        ) : UpdateStatusSourceModel
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Store {

        @ParameterizedTest
        @ProvideTestModels
        fun store(model: StoreTestModel) = runTest {
            // Arrange
            if (model.initialRuntime != null) {
                runtimeStore.store(value = model.initialRuntime)
            }

            if (model.initialPersistence != null) {
                persistenceStore.updateData { model.initialPersistence }
            }

            // Act
            store.store(values = model.values)

            val runtimeActual = runtimeStore.getSyncOrNull()
            val persistenceActual = getEmittedValues(persistenceStore.data)

            // Assert
            val runtimeExpected = model.runtimeExpected
            val persistenceExpected = listOf(model.persistenceExpected)

            Truth.assertThat(runtimeActual).isEqualTo(runtimeExpected)
            Truth.assertThat(persistenceActual).isEqualTo(persistenceExpected)
        }

        private fun provideTestModels() = listOf(
            // region stores are null
            StoreTestModel(
                initialRuntime = null,
                initialPersistence = null,
                values = emptyMap(),
                runtimeExpected = null,
                persistenceExpected = emptyMap(),
            ),
            StoreTestModel(
                initialRuntime = null,
                initialPersistence = null,
                values = mapOf(btcQuoteDM),
                runtimeExpected = setOf(btcQuote),
                persistenceExpected = mapOf(btcQuoteDM),
            ),
            // endregion

            // region persistence store is null
            StoreTestModel(
                initialRuntime = setOf(btcQuote),
                initialPersistence = null,
                values = emptyMap(),
                runtimeExpected = setOf(btcQuote),
                persistenceExpected = emptyMap(),
            ),
            StoreTestModel(
                initialRuntime = setOf(ethQuote),
                initialPersistence = null,
                values = mapOf(btcQuoteDM),
                runtimeExpected = setOf(ethQuote, btcQuote),
                persistenceExpected = mapOf(btcQuoteDM),
            ),
            // endregion

            // region runtime store is null
            StoreTestModel(
                initialRuntime = null,
                initialPersistence = mapOf(btcQuoteDM),
                values = emptyMap(),
                runtimeExpected = null,
                persistenceExpected = mapOf(btcQuoteDM),
            ),
            StoreTestModel(
                initialRuntime = null,
                initialPersistence = mapOf(ethQuoteDM),
                values = mapOf(btcQuoteDM),
                runtimeExpected = setOf(btcQuote),
                persistenceExpected = mapOf(ethQuoteDM, btcQuoteDM),
            ),
            // endregion

            // region stores contain data
            StoreTestModel(
                initialRuntime = setOf(btcQuote),
                initialPersistence = mapOf(btcQuoteDM),
                values = emptyMap(),
                runtimeExpected = setOf(btcQuote),
                persistenceExpected = mapOf(btcQuoteDM),
            ),
            StoreTestModel(
                initialRuntime = setOf(btcQuote),
                initialPersistence = mapOf(btcQuoteDM),
                values = mapOf(ethQuoteDM),
                runtimeExpected = setOf(ethQuote, btcQuote),
                persistenceExpected = mapOf(ethQuoteDM, btcQuoteDM),
            ),
            // endregion
        )
    }

    data class StoreTestModel(
        val initialRuntime: Set<QuoteStatus>?,
        val initialPersistence: CurrencyIdWithQuote?,
        val values: CurrencyIdWithQuote,
        val persistenceExpected: CurrencyIdWithQuote?,
        val runtimeExpected: Set<QuoteStatus>?,
    )
}