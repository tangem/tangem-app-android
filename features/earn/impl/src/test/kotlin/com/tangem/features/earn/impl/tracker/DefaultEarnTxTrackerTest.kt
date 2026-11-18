package com.tangem.features.earn.impl.tracker

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.domain.models.network.TxInfo.TransactionStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.earn.impl.analytics.EarnTxResultAnalyticsEvent
import com.tangem.features.earn.impl.overview.EarnProductsRegistry
import com.tangem.features.earn.product.api.analytics.EarnProductAnalytics
import com.tangem.features.earn.product.api.analytics.EarnTxAnalytics
import com.tangem.features.earn.product.api.product.EarnProduct
import com.tangem.features.earn.product.api.session.EarnSession
import com.tangem.features.earn.product.api.session.EarnTxRef
import com.tangem.features.earn.product.api.session.EarnTxStatus
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.TestAppCoroutineScope
import com.tangem.test.core.datastore.DelayedReadDataStore
import com.tangem.test.core.datastore.MockStateDataStore
import com.tangem.test.core.datastore.createAppPreferencesStore
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import io.mockk.clearMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultEarnTxTrackerTest {

    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxUnitFun = true)
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val accountListSupplier: SingleAccountListSupplier = mockk()
    private val session: EarnSession = mockk()
    private val sessionFactory: EarnSession.Factory = mockk()
    private val clock: EarnTxClock = mockk()

    private val currency: CryptoCurrency = MockCryptoCurrencyFactory().ethereum
    private val otherCurrency: CryptoCurrency = MockCryptoCurrencyFactory().stellar
    private val lending: EarnProduct = createProduct(id = LENDING_ID, category = "ETH Lending")
    private val staking: EarnProduct = createProduct(id = STAKING_ID, category = "ETH Staking")
    private val ref = createRef(id = "0xaaa")

    private val expectedConfirmed = EarnTxResultAnalyticsEvent.TransactionConfirmed(
        category = "ETH Lending",
        params = mapOf("Transaction Type" to "Deposit", "With Upgrade" to "false", "Amount Bucket" to "250-1K"),
    )

    @BeforeEach
    fun setUp() {
        clearMocks(
            analyticsEventHandler,
            userWalletsListRepository,
            accountListSupplier,
            session,
            sessionFactory,
            clock,
        )
        mockkObject(TangemLogger)
        every { clock.now() } returns NOW
        every { sessionFactory.create(any()) } returns session
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(userWallet(USER_WALLET_ID)))
        every { accountListSupplier(USER_WALLET_ID) } returns flowOf(accountListWith(currency))
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(TangemLogger)
    }

    @Test
    fun `GIVEN onSent WHEN track gives Confirmed THEN one Confirmed with params and confirmedParams and the record is removed`() =
        runTest {
            // Arrange
            val store = createStore()
            val tracker = createTracker(store)
            every { session.track(ref) } returns statuses(TransactionStatus.Unconfirmed, TransactionStatus.Confirmed)

            // Act
            tracker.onSent(ref)
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { analyticsEventHandler.send(expectedConfirmed) }
            confirmVerified(analyticsEventHandler)
            verify(exactly = 1) {
                sessionFactory.create(EarnProduct.Params(userWalletId = USER_WALLET_ID, currency = currency))
            }
            assertThat(store.all()).isEmpty()
        }

    @Test
    fun `GIVEN onSent WHEN track gives Failed THEN one Failed with Reverted and no confirmed params`() = runTest {
        // Arrange
        val store = createStore()
        val tracker = createTracker(store)
        every { session.track(ref) } returns statuses(TransactionStatus.Unconfirmed, TransactionStatus.Failed)

        // Act
        tracker.onSent(ref)
        advanceUntilIdle()

        // Assert
        val expected = EarnTxResultAnalyticsEvent.TransactionFailed(
            category = "ETH Lending",
            params = mapOf("Transaction Type" to "Deposit", "With Upgrade" to "false"),
        )
        verify(exactly = 1) { analyticsEventHandler.send(expected) }
        confirmVerified(analyticsEventHandler)
        assertThat(store.all()).isEmpty()
    }

    @Test
    fun `GIVEN a stored record WHEN resume THEN track continues and reports the result`() = runTest {
        // Arrange
        val store = createStore()
        store.add(ref)
        val tracker = createTracker(store)
        every { session.track(ref) } returns statuses(TransactionStatus.Confirmed)

        // Act
        tracker.resume()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { session.track(ref) }
        verify(exactly = 1) { analyticsEventHandler.send(expectedConfirmed) }
        assertThat(store.all()).isEmpty()
    }

    @Test
    fun `GIVEN a resolved record WHEN resume again THEN no event`() = runTest {
        // Arrange
        val store = createStore()
        val tracker = createTracker(store)
        every { session.track(ref) } returns statuses(TransactionStatus.Confirmed)
        tracker.onSent(ref)
        advanceUntilIdle()

        // Act
        tracker.resume()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { session.track(ref) }
        verify(exactly = 1) { analyticsEventHandler.send(expectedConfirmed) }
        confirmVerified(analyticsEventHandler)
    }

    @Test
    fun `GIVEN Unconfirmed until the stream ends WHEN onSent THEN no event and the record stays`() = runTest {
        // Arrange
        val store = createStore()
        val tracker = createTracker(store)
        every { session.track(ref) } returns statuses(TransactionStatus.Unconfirmed, TransactionStatus.Unconfirmed)

        // Act
        tracker.onSent(ref)
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { analyticsEventHandler.send(any()) }
        assertThat(store.all()).containsExactly(ref)
    }

    @Test
    fun `GIVEN Unconfirmed until the stream ends WHEN resume later THEN the record is tracked again`() = runTest {
        // Arrange
        val store = createStore()
        val tracker = createTracker(store)
        every { session.track(ref) } returnsMany listOf(
            statuses(TransactionStatus.Unconfirmed),
            statuses(TransactionStatus.Confirmed),
        )
        tracker.onSent(ref)
        advanceUntilIdle()

        // Act
        tracker.resume()
        advanceUntilIdle()

        // Assert
        verify(exactly = 2) { session.track(ref) }
        verify(exactly = 1) { analyticsEventHandler.send(expectedConfirmed) }
        assertThat(store.all()).isEmpty()
    }

    @Test
    fun `GIVEN track throws WHEN onSent THEN no event, the record stays and the error is logged`() = runTest {
        // Arrange
        val store = createStore()
        val tracker = createTracker(store)
        val error = IllegalStateException("network")
        every { session.track(ref) } returns flow { throw error }

        // Act
        tracker.onSent(ref)
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { analyticsEventHandler.send(any()) }
        verify(exactly = 1) { TangemLogger.e(any(), error, any()) }
        assertThat(store.all()).containsExactly(ref)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a record that cannot be followed WHEN resume THEN it is removed without track and others are tracked`(
        model: DropModel,
    ) = runTest {
        // Arrange
        val store = createStore()
        val tracked = createRef(id = "0xbbb")
        store.add(model.dropped)
        store.add(tracked)
        val tracker = createTracker(store)
        every { session.track(tracked) } returns statuses(TransactionStatus.Confirmed)

        // Act
        tracker.resume()
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { session.track(model.dropped) }
        verify(exactly = 1) { session.track(tracked) }
        verify(exactly = 1) { analyticsEventHandler.send(expectedConfirmed) }
        verify(exactly = 1) { TangemLogger.e(any(), any(), any()) }
        assertThat(store.all()).isEmpty()
    }

    @Test
    fun `GIVEN a record created 8 days ago WHEN resume THEN it is removed without lookups, track and event`() =
        runTest {
            // Arrange
            val store = createStore()
            val expired = createRef(id = "0xaaa", createdAt = NOW - 8.days)
            store.add(expired)
            val tracker = createTracker(store)

            // Act
            tracker.resume()
            advanceUntilIdle()

            // Assert
            verify(exactly = 0) { userWalletsListRepository.userWallets }
            verify(exactly = 0) { accountListSupplier(any<UserWalletId>()) }
            verify(exactly = 0) { session.track(any()) }
            verify(exactly = 0) { analyticsEventHandler.send(any()) }
            verify(exactly = 1) { TangemLogger.e(any(), any(), any()) }
            assertThat(store.all()).isEmpty()
        }

    @Test
    fun `GIVEN a record created 6 days ago WHEN resume THEN it is tracked and its result is sent`() = runTest {
        // Arrange
        val store = createStore()
        val recent = createRef(id = "0xaaa", createdAt = NOW - 6.days)
        store.add(recent)
        val tracker = createTracker(store)
        every { session.track(recent) } returns statuses(TransactionStatus.Confirmed)

        // Act
        tracker.resume()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { session.track(recent) }
        verify(exactly = 1) { analyticsEventHandler.send(expectedConfirmed) }
        assertThat(store.all()).isEmpty()
    }

    @Test
    fun `GIVEN the wallets never load WHEN resume THEN no event and the record stays`() = runTest {
        // Arrange
        val store = createStore()
        store.add(ref)
        val tracker = createTracker(store)
        every { userWalletsListRepository.userWallets } returns MutableStateFlow<List<UserWallet>?>(null)

        // Act
        tracker.resume()
        runCurrent()
        advanceTimeBy(31.seconds)
        runCurrent()

        // Assert
        verify(exactly = 0) { accountListSupplier(any<UserWalletId>()) }
        verify(exactly = 0) { session.track(any()) }
        verify(exactly = 0) { analyticsEventHandler.send(any()) }
        assertThat(store.all()).containsExactly(ref)
    }

    @Test
    fun `GIVEN the wallet is present but its account list never comes WHEN resume THEN no event and the record stays`() =
        runTest {
            // Arrange
            val store = createStore()
            store.add(ref)
            val tracker = createTracker(store)
            every { accountListSupplier(USER_WALLET_ID) } returns flow { awaitCancellation() }

            // Act
            tracker.resume()
            runCurrent()
            advanceTimeBy(31.seconds)
            runCurrent()

            // Assert
            verify(exactly = 0) { session.track(any()) }
            verify(exactly = 0) { analyticsEventHandler.send(any()) }
            assertThat(store.all()).containsExactly(ref)
        }

    @Test
    fun `GIVEN the account list timed out WHEN resume again with the wallet unlocked THEN the record is tracked`() =
        runTest {
            // Arrange
            val store = createStore()
            store.add(ref)
            val tracker = createTracker(store)
            every { accountListSupplier(USER_WALLET_ID) } returnsMany listOf(
                flow { awaitCancellation() },
                flowOf(accountListWith(currency)),
            )
            every { session.track(ref) } returns statuses(TransactionStatus.Confirmed)
            tracker.resume()
            runCurrent()
            advanceTimeBy(31.seconds)
            runCurrent()

            // Act
            tracker.resume()
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { analyticsEventHandler.send(expectedConfirmed) }
            assertThat(store.all()).isEmpty()
        }

    @Test
    fun `GIVEN the same ref WHEN onSent and resume both start tracking THEN track is called once`() = runTest {
        // Arrange
        val store = createStore()
        val tracker = createTracker(store)
        val statuses = MutableSharedFlow<EarnTxStatus>()
        every { session.track(ref) } returns statuses

        // Act
        tracker.onSent(ref)
        runCurrent()
        tracker.resume()
        runCurrent()
        statuses.emit(EarnTxStatus(TransactionStatus.Confirmed))
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { session.track(ref) }
        verify(exactly = 1) { analyticsEventHandler.send(expectedConfirmed) }
        confirmVerified(analyticsEventHandler)
    }

    @Test
    fun `GIVEN resume read a record that resolves meanwhile WHEN resume tracks it THEN the result is sent once`() =
        runTest {
            // Arrange
            val store = createStore(
                preferences = DelayedReadDataStore(
                    delegate = MockStateDataStore(emptyPreferences()),
                    readDelayMillis = READ_DELAY_MILLIS,
                ),
            )
            val tracker = createTracker(store)
            val firstStatuses = MutableSharedFlow<EarnTxStatus>()
            every { session.track(ref) } returnsMany listOf(firstStatuses, statuses(TransactionStatus.Confirmed))
            tracker.onSent(ref)
            advanceTimeBy((READ_DELAY_MILLIS * 3 / 2).milliseconds)
            runCurrent()

            // Act
            tracker.resume()
            advanceTimeBy((READ_DELAY_MILLIS / 2).milliseconds)
            firstStatuses.emit(EarnTxStatus(TransactionStatus.Confirmed))
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { analyticsEventHandler.send(expectedConfirmed) }
            confirmVerified(analyticsEventHandler)
            assertThat(store.all()).isEmpty()
        }

    private fun provideTestModels() = listOf(
        DropModel(name = "unknown product", dropped = createRef(id = "0xccc", productId = EarnProductId("vault"))),
        DropModel(name = "no session factory", dropped = createRef(id = "0xccc", productId = STAKING_ID)),
        DropModel(name = "no currency in the wallet", dropped = createRef(id = "0xccc", currencyId = otherCurrency.id)),
        DropModel(name = "removed wallet", dropped = createRef(id = "0xccc", userWalletId = REMOVED_WALLET_ID)),
    )

    internal data class DropModel(val name: String, val dropped: EarnTxRef) {
        override fun toString(): String = name
    }

    private fun TestScope.createTracker(store: EarnTxStore) = DefaultEarnTxTracker(
        store = store,
        registry = { EarnProductsRegistry(products = setOf(lending, staking), rateSources = emptySet()) },
        sessionFactories = { mapOf(LENDING_ID.value to sessionFactory) },
        userWalletsListRepository = userWalletsListRepository,
        accountListSupplier = accountListSupplier,
        analyticsEventHandler = analyticsEventHandler,
        appScope = TestAppCoroutineScope(this),
        clock = clock,
    )

    private fun createStore(
        preferences: DataStore<Preferences> = MockStateDataStore(emptyPreferences()),
    ) = EarnTxStore(
        appPreferencesStore = createAppPreferencesStore(
            dispatchers = TestingCoroutineDispatcherProvider(),
            preferencesDataStore = preferences,
        ),
    )

    private fun statuses(vararg values: TransactionStatus): Flow<EarnTxStatus> =
        flowOf(*values.map(::EarnTxStatus).toTypedArray())

    private fun userWallet(id: UserWalletId): UserWallet = mockk { every { walletId } returns id }

    private fun accountListWith(currency: CryptoCurrency): AccountList =
        AccountList.empty(userWalletId = USER_WALLET_ID, cryptoCurrencies = listOf(currency))

    private fun createProduct(id: EarnProductId, category: String): EarnProduct = mockk {
        every { this@mockk.id } returns id
        every { rank } returns 0
        every { analytics } returns EarnProductAnalytics(category = category, product = id.value)
    }

    private fun createRef(
        id: String,
        productId: EarnProductId = LENDING_ID,
        currencyId: CryptoCurrency.ID = currency.id,
        userWalletId: UserWalletId = USER_WALLET_ID,
        createdAt: Instant = Instant.parse("2030-01-01T00:00:00Z"),
    ) = EarnTxRef(
        version = 1,
        productId = productId,
        id = id,
        userWalletId = userWalletId,
        currencyId = currencyId,
        createdAt = createdAt,
        analytics = EarnTxAnalytics(
            params = mapOf("Transaction Type" to "Deposit", "With Upgrade" to "false"),
            confirmedParams = mapOf("Amount Bucket" to "250-1K"),
        ),
    )

    private companion object {
        val USER_WALLET_ID = UserWalletId("0123456789abcdef")
        val REMOVED_WALLET_ID = UserWalletId("fedcba9876543210")
        val LENDING_ID = EarnProductId("lending")
        val STAKING_ID = EarnProductId("staking")
        val NOW: Instant = Instant.parse("2030-01-02T00:00:00Z")
        const val READ_DELAY_MILLIS = 100L
    }
}