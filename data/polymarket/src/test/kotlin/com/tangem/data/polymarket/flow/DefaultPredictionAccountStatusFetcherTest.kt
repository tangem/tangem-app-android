package com.tangem.data.polymarket.flow

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.data.polymarket.converter.PredictionAccountStatusValueDTOConverter
import com.tangem.data.polymarket.store.PredictionAccountStatusStore
import com.tangem.data.polymarket.store.WalletIdWithPredictionStatusDTO
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.PredictionAccountStatusValue
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.flow.PredictionAccountStatusFetcher
import com.tangem.domain.polymarket.flow.PredictionAccountStatusFetcher.Mode
import com.tangem.domain.polymarket.interactor.GetPolymarketBalanceInteractor
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketAuthError
import com.tangem.domain.polymarket.model.PolymarketBalanceAllowance
import com.tangem.domain.polymarket.model.PolymarketOnboardingError
import com.tangem.domain.polymarket.model.PolymarketWalletState
import com.tangem.domain.polymarket.model.PolymarketWalletError
import com.tangem.domain.polymarket.model.PolymarketPositionsError
import com.tangem.domain.polymarket.model.PolymarketWalletStatus
import com.tangem.domain.polymarket.usecase.DerivePolymarketAddressesUseCase
import com.tangem.domain.polymarket.usecase.RecordPolymarketConfirmationUseCase
import com.tangem.domain.polymarket.PolymarketPositionsRepository
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.usecase.GetPolymarketWalletStatusUseCase
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.TestAppCoroutineScope
import com.tangem.test.core.datastore.MockStateDataStore
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultPredictionAccountStatusFetcherTest {

    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val userWallet: UserWallet.Cold = mockk()
    private val deriveAddresses: DerivePolymarketAddressesUseCase = mockk()
    private val getWalletStatus: GetPolymarketWalletStatusUseCase = mockk()
    private val getBalance: GetPolymarketBalanceInteractor = mockk()
    private val recordConfirmation: RecordPolymarketConfirmationUseCase = mockk(relaxUnitFun = true)
    private val polymarketRepository: PolymarketRepository = mockk()
    private val positionsRepository: PolymarketPositionsRepository = mockk()

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            userWalletsListRepository,
            userWallet,
            deriveAddresses,
            getWalletStatus,
            getBalance,
            recordConfirmation,
            polymarketRepository,
            positionsRepository,
        )
        every { userWallet.isLocked } returns false
        every { userWalletsListRepository.userWallets } returns MutableStateFlow<List<UserWallet>?>(listOf(userWallet))
        every { userWallet.walletId } returns WALLET
    }

    /**
     * The refresh runs for every wallet, including ones that never opened the feature. Deriving the addresses for
     * real opens a card session or unlocks the wallet, so it may only read what is already stored.
     */
    @Test
    fun `GIVEN no stored addresses WHEN invoke THEN it reports not onboarded without deriving`() = runTest {
        // Arrange
        coEvery { deriveAddresses.stored(WALLET) } returns null
        coEvery { polymarketRepository.getWalletStatusByWalletId(WALLET.stringValue) } returns
            walletState(PolymarketWalletStatus.NOT_CREATED).right()
        val store = createStore(testScope = this)

        // Act
        createFetcher(store).invoke(PredictionAccountStatusFetcher.Params(WALLET, mode = Mode.AVAILABLE_ONLY))

        // Assert
        assertThat(store.getSyncOrNull(WALLET)).isEqualTo(PredictionAccountStatusValue.NotOnboarded)
        coVerify(exactly = 0) { deriveAddresses.invoke(any()) }
        coVerify(exactly = 0) { getWalletStatus.invoke(any()) }
    }

    @ParameterizedTest
    @ProvideTestModels
    fun invoke(model: StatusTestModel) = runTest {
        // Arrange
        coEvery { deriveAddresses.stored(WALLET) } returns ADDRESSES
        coEvery { getWalletStatus.invoke(ADDRESSES) } returns walletState(model.status).right()
        val store = createStore(testScope = this)

        // Act
        createFetcher(store).invoke(PredictionAccountStatusFetcher.Params(WALLET, mode = Mode.AVAILABLE_ONLY))

        // Assert
        assertThat(store.getSyncOrNull(WALLET)).isEqualTo(model.expected)
    }

    @Test
    fun `GIVEN no address stored WHEN a balance refresh runs THEN no deposit address is invented`() = runTest {
        // Arrange
        coEvery { deriveAddresses.stored(WALLET) } returns ADDRESSES
        coEvery { getWalletStatus.invoke(ADDRESSES) } returns walletState(PolymarketWalletStatus.READY_TO_TRADE).right()
        coEvery { getBalance.invoke(ADDRESSES) } returns
            PolymarketBalanceAllowance(balance = BigDecimal("40"), allowances = null).right()
        val store = createStore(testScope = this)

        // Act
        createFetcher(store).invoke(PredictionAccountStatusFetcher.Params(WALLET, mode = Mode.AVAILABLE_ONLY))

        // Assert
        assertThat((store.getSyncOrNull(WALLET) as PredictionAccountStatusValue.Active).depositAddress).isNull()
    }

    @Test
    fun `GIVEN an address a funding flow confirmed WHEN invoke THEN the refreshed balance keeps it`() = runTest {
        // Arrange
        coEvery { deriveAddresses.stored(WALLET) } returns ADDRESSES
        coEvery { getWalletStatus.invoke(ADDRESSES) } returns walletState(PolymarketWalletStatus.READY_TO_TRADE).right()
        coEvery { getBalance.invoke(ADDRESSES) } returns
            PolymarketBalanceAllowance(balance = BigDecimal("40"), allowances = null).right()
        val store = createStore(testScope = this)
        store.store(userWalletId = WALLET, value = ACTIVE.copy(depositAddress = BRIDGE_ADDRESS))

        // Act
        createFetcher(store).invoke(PredictionAccountStatusFetcher.Params(WALLET, mode = Mode.AVAILABLE_ONLY))

        // Assert
        val stored = store.getSyncOrNull(WALLET) as PredictionAccountStatusValue.Active
        assertThat(stored.balance).isEqualTo(BigDecimal("40"))
        assertThat(stored.depositAddress).isEqualTo(BRIDGE_ADDRESS)
    }

    @Test
    fun `GIVEN a ready wallet WHEN invoke THEN the balance is stored`() = runTest {
        // Arrange
        coEvery { deriveAddresses.stored(WALLET) } returns ADDRESSES
        coEvery { getWalletStatus.invoke(ADDRESSES) } returns walletState(PolymarketWalletStatus.READY_TO_TRADE).right()
        coEvery { getBalance.invoke(ADDRESSES) } returns
            PolymarketBalanceAllowance(balance = BigDecimal("40"), allowances = null).right()
        val store = createStore(testScope = this)

        // Act
        createFetcher(store).invoke(PredictionAccountStatusFetcher.Params(WALLET, mode = Mode.AVAILABLE_ONLY))

        // Assert — the rate itself is not stored, it belongs to the currency the user may switch at any time
        assertThat(store.getSyncOrNull(WALLET)).isEqualTo(
            PredictionAccountStatusValue.Active(
                source = StatusSource.ACTUAL,
                balance = BigDecimal("40"),
                positionsValue = null,
                fiatRate = null,
                depositCurrency = COLLATERAL,
                depositAddress = null,
            ),
        )
    }

    @Test
    fun `GIVEN a full refresh WHEN invoke THEN the positions value is stored next to the balance`() = runTest {
        // Arrange
        stubReadyWallet(balance = BigDecimal("40"))
        coEvery { positionsRepository.getPositionsValue(ADDRESSES) } returns BigDecimal("7").right()
        val store = createStore(testScope = this)

        // Act
        createFetcher(store).invoke(PredictionAccountStatusFetcher.Params(WALLET, mode = Mode.FULL))

        // Assert
        val stored = store.getSyncOrNull(WALLET) as PredictionAccountStatusValue.Active
        assertThat(stored.balance).isEqualTo(BigDecimal("40"))
        assertThat(stored.positionsValue).isEqualTo(BigDecimal("7"))
    }

    @Test
    fun `GIVEN the positions value cannot be read WHEN a full refresh runs THEN the stored one is kept`() =
        runTest {
            // Arrange
            stubReadyWallet(balance = BigDecimal("40"))
            coEvery { positionsRepository.getPositionsValue(ADDRESSES) } returns
                PolymarketPositionsError.Network.left()
            val store = createStore(testScope = this)
            store.store(userWalletId = WALLET, value = ACTIVE.copy(positionsValue = BigDecimal("7")))

            // Act
            createFetcher(store).invoke(PredictionAccountStatusFetcher.Params(WALLET, mode = Mode.FULL))

            // Assert
            val stored = store.getSyncOrNull(WALLET) as PredictionAccountStatusValue.Active
            assertThat(stored.balance).isEqualTo(BigDecimal("40"))
            assertThat(stored.positionsValue).isEqualTo(BigDecimal("7"))
            coVerify(exactly = 1) { positionsRepository.getPositionsValue(ADDRESSES) }
        }

    @Test
    fun `GIVEN the balance cannot be read WHEN a full refresh runs THEN the positions read is dropped and the value flagged`() =
        runTest {
            // Arrange
            stubReadyWallet(balance = BigDecimal("40"))
            coEvery { getBalance.invoke(ADDRESSES) } returns PolymarketAuthError.RateLimited.left()
            coEvery { positionsRepository.getPositionsValue(ADDRESSES) } coAnswers { awaitCancellation() }
            val store = createStore(testScope = this)
            val cached = ACTIVE.copy(positionsValue = BigDecimal("7"))
            store.store(userWalletId = WALLET, value = cached)

            // Act
            createFetcher(store).invoke(PredictionAccountStatusFetcher.Params(WALLET, mode = Mode.FULL))

            // Assert
            assertThat(store.getSyncOrNull(WALLET)).isEqualTo(cached.copy(source = StatusSource.ONLY_CACHE))
        }

    @Test
    fun `GIVEN a stored positions value WHEN an available-only refresh runs THEN it is carried over unread`() =
        runTest {
            // Arrange
            stubReadyWallet(balance = BigDecimal("40"))
            val store = createStore(testScope = this)
            store.store(userWalletId = WALLET, value = ACTIVE.copy(positionsValue = BigDecimal("7")))

            // Act
            createFetcher(store).invoke(PredictionAccountStatusFetcher.Params(WALLET, mode = Mode.AVAILABLE_ONLY))

            // Assert
            val stored = store.getSyncOrNull(WALLET) as PredictionAccountStatusValue.Active
            assertThat(stored.positionsValue).isEqualTo(BigDecimal("7"))
            coVerify(exactly = 0) { positionsRepository.getPositionsValue(any()) }
        }

    @Test
    fun `GIVEN a full refresh lands mid-fetch WHEN an available-only refresh completes THEN the newer value is carried`() =
        runTest {
            // Arrange
            val store = createStore(testScope = this)
            store.store(userWalletId = WALLET, value = ACTIVE.copy(positionsValue = BigDecimal("7")))
            coEvery { deriveAddresses.stored(WALLET) } returns ADDRESSES
            coEvery { getWalletStatus.invoke(ADDRESSES) } returns
                walletState(PolymarketWalletStatus.READY_TO_TRADE).right()
            coEvery { getBalance.invoke(ADDRESSES) } coAnswers {
                store.store(userWalletId = WALLET, value = ACTIVE.copy(positionsValue = BigDecimal("9")))
                PolymarketBalanceAllowance(balance = BigDecimal("40"), allowances = null).right()
            }

            // Act
            createFetcher(store).invoke(PredictionAccountStatusFetcher.Params(WALLET, mode = Mode.AVAILABLE_ONLY))

            // Assert
            val stored = store.getSyncOrNull(WALLET) as PredictionAccountStatusValue.Active
            assertThat(stored.positionsValue).isEqualTo(BigDecimal("9"))
        }

    @Test
    fun `GIVEN a refresh in flight WHEN another one starts for the same wallet THEN neither waits for the other`() =
        runTest {
            // Arrange
            val store = createStore(testScope = this)
            stubReadyWallet(balance = BigDecimal("40"))
            coEvery { positionsRepository.getPositionsValue(ADDRESSES) } returns BigDecimal("9").right()
            val inFlight = CompletableDeferred<Unit>()
            coEvery { getBalance.invoke(ADDRESSES) } coAnswers {
                inFlight.await()
                PolymarketBalanceAllowance(balance = BigDecimal("40"), allowances = null).right()
            } andThenAnswer {
                PolymarketBalanceAllowance(balance = BigDecimal("41"), allowances = null).right()
            }
            val fetcher = createFetcher(store)

            // Act
            val first = async { fetcher.invoke(PredictionAccountStatusFetcher.Params(WALLET, mode = Mode.FULL)) }
            runCurrent()
            val second = async {
                fetcher.invoke(PredictionAccountStatusFetcher.Params(WALLET, mode = Mode.AVAILABLE_ONLY))
            }
            runCurrent()

            // Assert
            assertThat(second.isCompleted).isTrue()
            coVerify(exactly = 2) { getBalance.invoke(ADDRESSES) }
            assertThat((store.getSyncOrNull(WALLET) as PredictionAccountStatusValue.Active).balance)
                .isEqualTo(BigDecimal("41"))

            inFlight.complete(Unit)
            first.await()
        }

    @Test
    fun `GIVEN an address confirmed while the positions value loads WHEN it lands THEN the address survives`() =
        runTest {
            // Arrange
            val store = createStore(testScope = this)
            store.store(userWalletId = WALLET, value = ACTIVE.copy(depositAddress = null))
            stubReadyWallet(balance = BigDecimal("40"))
            val loading = CompletableDeferred<Unit>()
            coEvery { positionsRepository.getPositionsValue(ADDRESSES) } coAnswers {
                loading.await()
                BigDecimal("9").right()
            }
            val fetcher = createFetcher(store)

            // Act
            val refresh = async { fetcher.invoke(PredictionAccountStatusFetcher.Params(WALLET, mode = Mode.FULL)) }
            runCurrent()
            store.store(userWalletId = WALLET, value = ACTIVE.copy(depositAddress = BRIDGE_ADDRESS))
            loading.complete(Unit)
            refresh.await()

            // Assert
            val stored = store.getSyncOrNull(WALLET) as PredictionAccountStatusValue.Active
            assertThat(stored.depositAddress).isEqualTo(BRIDGE_ADDRESS)
            assertThat(stored.positionsValue).isEqualTo(BigDecimal("9"))
        }

    @Test
    fun `GIVEN a refresh fails after a concurrent one succeeded WHEN it lands THEN only the source is flagged`() =
        runTest {
            // Arrange
            val store = createStore(testScope = this)
            coEvery { getWalletStatus.invoke(ADDRESSES) } returns
                walletState(PolymarketWalletStatus.READY_TO_TRADE).right()
            coEvery { getBalance.invoke(ADDRESSES) } returns
                PolymarketBalanceAllowance(balance = BigDecimal("40"), allowances = null).right()
            coEvery { positionsRepository.getPositionsValue(ADDRESSES) } returns BigDecimal("9").right()
            val failing = CompletableDeferred<Unit>()
            coEvery { deriveAddresses.stored(WALLET) } coAnswers {
                failing.await()
                error("boom")
            } andThenAnswer { ADDRESSES }
            val fetcher = createFetcher(store)

            // Act
            val first = async { fetcher.invoke(PredictionAccountStatusFetcher.Params(WALLET, mode = Mode.FULL)) }
            runCurrent()
            val second = async { fetcher.invoke(PredictionAccountStatusFetcher.Params(WALLET, mode = Mode.FULL)) }
            runCurrent()
            failing.complete(Unit)
            first.await()
            second.await()

            // Assert
            val stored = store.getSyncOrNull(WALLET) as PredictionAccountStatusValue.Active
            assertThat(stored.source).isEqualTo(StatusSource.ONLY_CACHE)
            assertThat(stored.balance).isEqualTo(BigDecimal("40"))
            assertThat(stored.positionsValue).isEqualTo(BigDecimal("9"))
        }

    @Test
    fun `GIVEN a refresh in flight WHEN another wallet refreshes THEN it does not wait`() = runTest {
        // Arrange
        val store = createStore(testScope = this)
        val otherWallet: UserWallet.Cold = mockk {
            every { isLocked } returns false
            every { walletId } returns OTHER_WALLET
        }
        every { userWalletsListRepository.userWallets } returns
            MutableStateFlow<List<UserWallet>?>(listOf(userWallet, otherWallet))
        coEvery { deriveAddresses.stored(WALLET) } returns ADDRESSES
        coEvery { deriveAddresses.stored(OTHER_WALLET) } returns OTHER_ADDRESSES
        coEvery { getWalletStatus.invoke(any()) } returns walletState(PolymarketWalletStatus.READY_TO_TRADE).right()
        coEvery { positionsRepository.getPositionsValue(any()) } returns BigDecimal("9").right()
        val inFlight = CompletableDeferred<Unit>()
        coEvery { getBalance.invoke(ADDRESSES) } coAnswers {
            inFlight.await()
            PolymarketBalanceAllowance(balance = BigDecimal("40"), allowances = null).right()
        }
        coEvery { getBalance.invoke(OTHER_ADDRESSES) } returns
            PolymarketBalanceAllowance(balance = BigDecimal("5"), allowances = null).right()
        val fetcher = createFetcher(store)

        // Act
        val blocked = async { fetcher.invoke(PredictionAccountStatusFetcher.Params(WALLET, mode = Mode.FULL)) }
        runCurrent()
        fetcher.invoke(PredictionAccountStatusFetcher.Params(OTHER_WALLET, mode = Mode.FULL))

        // Assert
        assertThat(store.getSyncOrNull(OTHER_WALLET)).isInstanceOf(PredictionAccountStatusValue.Active::class.java)
        assertThat(blocked.isCompleted).isFalse()

        inFlight.complete(Unit)
        blocked.await()
    }

    @Test
    fun `GIVEN the collateral cannot be read WHEN invoke THEN the account is reported as onboarded`() = runTest {
        // Arrange
        coEvery { deriveAddresses.stored(WALLET) } returns ADDRESSES
        coEvery { getWalletStatus.invoke(ADDRESSES) } returns walletState(PolymarketWalletStatus.READY_TO_TRADE).right()
        coEvery { getBalance.invoke(ADDRESSES) } returns PolymarketAuthError.KeyNotFound.left()
        val store = createStore(testScope = this)

        // Act
        createFetcher(store).invoke(PredictionAccountStatusFetcher.Params(WALLET, mode = Mode.AVAILABLE_ONLY))

        // Assert
        assertThat(store.getSyncOrNull(WALLET))
            .isEqualTo(PredictionAccountStatusValue.Onboarded(source = StatusSource.ACTUAL))
    }

    @Test
    fun `GIVEN an unreadable collateral WHEN invoke THEN the wallet total is not blanked`() = runTest {
        // Arrange — the resolver answers KeyNotFound for any 404, so this is not only the missing-key case
        coEvery { deriveAddresses.stored(WALLET) } returns ADDRESSES
        coEvery { getWalletStatus.invoke(ADDRESSES) } returns walletState(PolymarketWalletStatus.READY_TO_TRADE).right()
        coEvery { getBalance.invoke(ADDRESSES) } returns PolymarketAuthError.KeyNotFound.left()
        val store = createStore(testScope = this)

        // Act
        createFetcher(store).invoke(PredictionAccountStatusFetcher.Params(WALLET, mode = Mode.AVAILABLE_ONLY))

        // Assert
        assertThat(store.getSyncOrNull(WALLET)?.totalFiatBalance)
            .isEqualTo(TotalFiatBalance.Loaded(amount = BigDecimal.ZERO, source = StatusSource.ACTUAL))
    }

    @Test
    fun `GIVEN the refresh throws WHEN invoke THEN the cached status is kept and marked un-refreshed`() = runTest {
        // Arrange
        val store = createStore(testScope = this)
        store.store(userWalletId = WALLET, value = ACTIVE)
        coEvery { deriveAddresses.stored(WALLET) } throws IllegalStateException("no connection")

        // Act
        val actual = createFetcher(store).invoke(PredictionAccountStatusFetcher.Params(WALLET, mode = Mode.AVAILABLE_ONLY))

        // Assert
        assertThat(actual.isLeft()).isTrue()
        assertThat(store.getSyncOrNull(WALLET)).isEqualTo(ACTIVE.copy(source = StatusSource.ONLY_CACHE))
    }

    @Test
    fun `GIVEN nothing cached WHEN the wallet status fails THEN the failure is recorded`() = runTest {
        // Arrange
        val store = createStore(testScope = this)
        coEvery { deriveAddresses.stored(WALLET) } returns ADDRESSES
        coEvery { getWalletStatus.invoke(ADDRESSES) } returns PolymarketOnboardingError.Unknown.left()

        // Act
        createFetcher(store).invoke(PredictionAccountStatusFetcher.Params(WALLET, mode = Mode.AVAILABLE_ONLY))

        // Assert
        assertThat(store.getSyncOrNull(WALLET)).isEqualTo(PredictionAccountStatusValue.Error.Unavailable)
    }

    internal data class StatusTestModel(
        val status: PolymarketWalletStatus,
        val expected: PredictionAccountStatusValue,
    )

    private fun provideTestModels() = listOf(
        StatusTestModel(
            status = PolymarketWalletStatus.NOT_CREATED,
            expected = PredictionAccountStatusValue.NotOnboarded,
        ),
        StatusTestModel(
            status = PolymarketWalletStatus.DEPLOYMENT_IN_PROGRESS,
            expected = onboarding(PredictionAccountStatusValue.Onboarding.Stage.DEPLOYING),
        ),
        StatusTestModel(
            status = PolymarketWalletStatus.DEPLOYED,
            expected = onboarding(PredictionAccountStatusValue.Onboarding.Stage.DEPLOYED),
        ),
        StatusTestModel(
            status = PolymarketWalletStatus.APPROVALS_IN_PROGRESS,
            expected = onboarding(PredictionAccountStatusValue.Onboarding.Stage.APPROVING),
        ),
        StatusTestModel(
            status = PolymarketWalletStatus.DEPLOYMENT_FAILED,
            expected = PredictionAccountStatusValue.Error.OnboardingFailed,
        ),
        StatusTestModel(
            status = PolymarketWalletStatus.APPROVALS_FAILED,
            expected = PredictionAccountStatusValue.Error.OnboardingFailed,
        ),
    )

    /**
     * The repository answers with `Either.Left` instead of throwing, so a left that is treated as an answer would
     * drop a real balance out of the wallet total and out of the on-disk cache — for every user at once, whenever
     * the backend has a bad minute.
     */
    @Test
    fun `GIVEN a cached balance WHEN the wallet status fails THEN the balance is kept and marked un-refreshed`() =
        runTest {
            // Arrange
            val store = createStore(testScope = this)
            store.store(userWalletId = WALLET, value = ACTIVE)
            coEvery { deriveAddresses.stored(WALLET) } returns ADDRESSES
            coEvery { getWalletStatus.invoke(ADDRESSES) } returns PolymarketOnboardingError.Unknown.left()

            // Act
            createFetcher(store).invoke(PredictionAccountStatusFetcher.Params(WALLET, mode = Mode.AVAILABLE_ONLY))

            // Assert
            assertThat(store.getSyncOrNull(WALLET)).isEqualTo(ACTIVE.copy(source = StatusSource.ONLY_CACHE))
        }

    @Test
    fun `GIVEN a cached balance WHEN the balance read fails on the network THEN the balance is kept`() = runTest {
        // Arrange
        val store = createStore(testScope = this)
        store.store(userWalletId = WALLET, value = ACTIVE)
        coEvery { deriveAddresses.stored(WALLET) } returns ADDRESSES
        coEvery { getWalletStatus.invoke(ADDRESSES) } returns walletState(PolymarketWalletStatus.READY_TO_TRADE).right()
        coEvery { getBalance.invoke(ADDRESSES) } returns PolymarketAuthError.Network.left()

        // Act
        createFetcher(store).invoke(PredictionAccountStatusFetcher.Params(WALLET, mode = Mode.AVAILABLE_ONLY))

        // Assert — throttled or offline means the balance is unknown, not zero
        assertThat(store.getSyncOrNull(WALLET)).isEqualTo(ACTIVE.copy(source = StatusSource.ONLY_CACHE))
    }

    /**
     * A locked hot wallet holds no keys to read the addresses from. Reporting that as "never onboarded" would show
     * an onboarding invitation to a user who has money in the account, and erase the cached balance while at it.
     */
    @Test
    fun `GIVEN a locked wallet WHEN invoke THEN nothing is overwritten`() = runTest {
        // Arrange
        val store = createStore(testScope = this)
        store.store(userWalletId = WALLET, value = ACTIVE)
        every { userWallet.isLocked } returns true

        // Act
        createFetcher(store).invoke(PredictionAccountStatusFetcher.Params(WALLET, mode = Mode.AVAILABLE_ONLY))

        // Assert
        assertThat(store.getSyncOrNull(WALLET)).isEqualTo(ACTIVE.copy(source = StatusSource.ONLY_CACHE))
        coVerify(exactly = 0) { deriveAddresses.stored(any()) }
    }

    @Test
    fun `GIVEN a status this build does not know WHEN invoke THEN nothing is overwritten`() = runTest {
        // Arrange
        val store = createStore(testScope = this)
        store.store(userWalletId = WALLET, value = ACTIVE)
        coEvery { deriveAddresses.stored(WALLET) } returns ADDRESSES
        coEvery { getWalletStatus.invoke(ADDRESSES) } returns walletState(PolymarketWalletStatus.UNKNOWN).right()

        // Act
        createFetcher(store).invoke(PredictionAccountStatusFetcher.Params(WALLET, mode = Mode.AVAILABLE_ONLY))

        // Assert — the BFF contract calls UNKNOWN "not ready, keep polling", not an error
        assertThat(store.getSyncOrNull(WALLET)).isEqualTo(ACTIVE.copy(source = StatusSource.ONLY_CACHE))
    }

    private fun createStore(testScope: TestScope) = PredictionAccountStatusStore(
        dtoConverter = PredictionAccountStatusValueDTOConverter(
            collateralCurrencyFactory = mockk { every { createDepositTarget() } returns COLLATERAL },
        ),
        runtimeStore = RuntimeSharedStore(),
        persistenceDataStore = MockStateDataStore<WalletIdWithPredictionStatusDTO>(default = emptyMap()),
        scope = TestAppCoroutineScope(testScope),
    )


    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class OnboardingRecord {

        /**
         * The entry gate stops asking the backend once a wallet is recorded, so this refresh is the only thing
         * that can notice the backend dropping the wallet. What each status does to the record is the use case's
         * rule; the fetcher's obligation is only to hand it every status it reads.
         */
        @Test
        fun `GIVEN the backend answers WHEN invoke THEN the status is handed to the record`() = runTest {
            // Arrange
            coEvery { deriveAddresses.stored(WALLET) } returns ADDRESSES
            coEvery { getWalletStatus.invoke(ADDRESSES) } returns
                walletState(PolymarketWalletStatus.APPROVALS_FAILED).right()

            // Act
            createFetcher(createStore(testScope = this)).invoke(PredictionAccountStatusFetcher.Params(WALLET, mode = Mode.AVAILABLE_ONLY))

            // Assert
            coVerify(exactly = 1) {
                recordConfirmation.invoke(userWalletId = WALLET, status = PolymarketWalletStatus.APPROVALS_FAILED)
            }
        }

        @Test
        fun `GIVEN the backend reports ready WHEN invoke THEN the ready status is handed to the record`() = runTest {
            // Arrange
            coEvery { deriveAddresses.stored(WALLET) } returns ADDRESSES
            coEvery { getWalletStatus.invoke(ADDRESSES) } returns
                walletState(PolymarketWalletStatus.READY_TO_TRADE).right()
            coEvery { getBalance.invoke(ADDRESSES) } returns
                PolymarketBalanceAllowance(balance = BigDecimal("40"), allowances = null).right()

            // Act
            createFetcher(createStore(testScope = this)).invoke(PredictionAccountStatusFetcher.Params(WALLET, mode = Mode.AVAILABLE_ONLY))

            // Assert
            coVerify(exactly = 1) {
                recordConfirmation.invoke(userWalletId = WALLET, status = PolymarketWalletStatus.READY_TO_TRADE)
            }
        }

        /** A status that could not be read is no answer at all, so the record must not even be asked about it. */
        @Test
        fun `GIVEN the status cannot be read WHEN invoke THEN the record is left alone`() = runTest {
            // Arrange
            coEvery { deriveAddresses.stored(WALLET) } returns ADDRESSES
            coEvery { getWalletStatus.invoke(ADDRESSES) } returns PolymarketOnboardingError.Unknown.left()

            // Act
            createFetcher(createStore(testScope = this)).invoke(PredictionAccountStatusFetcher.Params(WALLET, mode = Mode.AVAILABLE_ONLY))

            // Assert
            coVerify(exactly = 0) { recordConfirmation.invoke(any(), any()) }
        }
    }


    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class WithoutStoredAddresses {

        /**
         * A wallet set up on another device: this one never derived its owner address, so the backend can only
         * be asked by wallet id. Answering "not onboarded" without asking would deny the user their account.
         */
        @Test
        fun `GIVEN no stored address WHEN the backend knows the wallet by id THEN it is onboarded`() = runTest {
            // Arrange
            coEvery { deriveAddresses.stored(WALLET) } returns null
            coEvery { polymarketRepository.getWalletStatusByWalletId(WALLET.stringValue) } returns
                walletState(PolymarketWalletStatus.READY_TO_TRADE).right()
            val store = createStore(testScope = this)

            // Act
            createFetcher(store).invoke(PredictionAccountStatusFetcher.Params(WALLET, mode = Mode.AVAILABLE_ONLY))

            // Assert — no local credentials, so there is no amount to show, only the account itself
            assertThat(store.getSyncOrNull(WALLET)).isEqualTo(PredictionAccountStatusValue.Onboarded(source = StatusSource.ACTUAL))
            coVerify(exactly = 1) {
                recordConfirmation.invoke(userWalletId = WALLET, status = PolymarketWalletStatus.READY_TO_TRADE)
            }
            coVerify(exactly = 0) { getWalletStatus.invoke(any()) }
        }

        @Test
        fun `GIVEN no stored address AND the backend has no wallet WHEN invoke THEN it is not onboarded`() = runTest {
            // Arrange
            coEvery { deriveAddresses.stored(WALLET) } returns null
            coEvery { polymarketRepository.getWalletStatusByWalletId(WALLET.stringValue) } returns
                walletState(PolymarketWalletStatus.NOT_CREATED).right()
            val store = createStore(testScope = this)

            // Act
            createFetcher(store).invoke(PredictionAccountStatusFetcher.Params(WALLET, mode = Mode.AVAILABLE_ONLY))

            // Assert
            assertThat(store.getSyncOrNull(WALLET)).isEqualTo(PredictionAccountStatusValue.NotOnboarded)
            coVerify(exactly = 1) {
                recordConfirmation.invoke(userWalletId = WALLET, status = PolymarketWalletStatus.NOT_CREATED)
            }
        }

        /** A failed question is not an answer: neither the cached value nor the record may be dropped for it. */
        @Test
        fun `GIVEN no stored address WHEN the lookup fails THEN the cached value and the record survive`() = runTest {
            // Arrange
            coEvery { deriveAddresses.stored(WALLET) } returns null
            coEvery { polymarketRepository.getWalletStatusByWalletId(WALLET.stringValue) } returns
                PolymarketWalletError.Network.left()
            val store = createStore(testScope = this)
            store.store(userWalletId = WALLET, value = ACTIVE)

            // Act
            createFetcher(store).invoke(PredictionAccountStatusFetcher.Params(WALLET, mode = Mode.AVAILABLE_ONLY))

            // Assert
            assertThat(store.getSyncOrNull(WALLET)).isEqualTo(ACTIVE.copy(source = StatusSource.ONLY_CACHE))
            coVerify(exactly = 1) { polymarketRepository.getWalletStatusByWalletId(WALLET.stringValue) }
            coVerify(exactly = 0) { recordConfirmation.invoke(any(), any()) }
        }

        /**
         * The status was a definite answer even though the balance behind it was not: recording it saves the
         * entry gate a request it would otherwise have to make.
         */
        @Test
        fun `GIVEN the backend reports ready WHEN the balance cannot be read THEN the record is still written`() =
            runTest {
                // Arrange
                coEvery { deriveAddresses.stored(WALLET) } returns ADDRESSES
                coEvery { getWalletStatus.invoke(ADDRESSES) } returns
                    walletState(PolymarketWalletStatus.READY_TO_TRADE).right()
                coEvery { getBalance.invoke(ADDRESSES) } returns PolymarketAuthError.RateLimited.left()

                // Act
                createFetcher(createStore(testScope = this)).invoke(PredictionAccountStatusFetcher.Params(WALLET, mode = Mode.AVAILABLE_ONLY))

                // Assert
                coVerify(exactly = 1) {
                    recordConfirmation.invoke(userWalletId = WALLET, status = PolymarketWalletStatus.READY_TO_TRADE)
                }
            }

        /**
         * An unrecognised status is "not ready, keep polling" by the BFF contract. Overwriting a cached balance
         * with "no account" on the strength of one would lose a real amount.
         */
        @Test
        fun `GIVEN no stored address WHEN the status is unrecognised THEN the cached value survives`() = runTest {
            // Arrange
            coEvery { deriveAddresses.stored(WALLET) } returns null
            coEvery { polymarketRepository.getWalletStatusByWalletId(WALLET.stringValue) } returns
                walletState(PolymarketWalletStatus.UNKNOWN).right()
            val store = createStore(testScope = this)
            store.store(userWalletId = WALLET, value = ACTIVE)

            // Act
            createFetcher(store).invoke(PredictionAccountStatusFetcher.Params(WALLET, mode = Mode.AVAILABLE_ONLY))

            // Assert
            assertThat(store.getSyncOrNull(WALLET)).isEqualTo(ACTIVE.copy(source = StatusSource.ONLY_CACHE))
            coVerify(exactly = 1) {
                recordConfirmation.invoke(userWalletId = WALLET, status = PolymarketWalletStatus.UNKNOWN)
            }
        }

        /** Mid-setup elsewhere is not "no account": the stage must survive the trip through the id lookup. */
        @Test
        fun `GIVEN no stored address WHEN the wallet is mid-setup THEN the stage is reported`() = runTest {
            // Arrange
            coEvery { deriveAddresses.stored(WALLET) } returns null
            coEvery { polymarketRepository.getWalletStatusByWalletId(WALLET.stringValue) } returns
                walletState(PolymarketWalletStatus.DEPLOYED).right()
            val store = createStore(testScope = this)

            // Act
            createFetcher(store).invoke(PredictionAccountStatusFetcher.Params(WALLET, mode = Mode.AVAILABLE_ONLY))

            // Assert
            assertThat(store.getSyncOrNull(WALLET)).isEqualTo(
                PredictionAccountStatusValue.Onboarding(
                    source = StatusSource.ACTUAL,
                    stage = PredictionAccountStatusValue.Onboarding.Stage.DEPLOYED,
                ),
            )
        }
    }

    private fun stubReadyWallet(balance: BigDecimal) {
        coEvery { deriveAddresses.stored(WALLET) } returns ADDRESSES
        coEvery { getWalletStatus.invoke(ADDRESSES) } returns walletState(PolymarketWalletStatus.READY_TO_TRADE).right()
        coEvery { getBalance.invoke(ADDRESSES) } returns
            PolymarketBalanceAllowance(balance = balance, allowances = null).right()
    }

    private fun createFetcher(store: PredictionAccountStatusStore) = DefaultPredictionAccountStatusFetcher(
        statusStore = store,
        userWalletsListRepository = userWalletsListRepository,
        derivePolymarketAddressesUseCase = deriveAddresses,
        getPolymarketWalletStatusUseCase = getWalletStatus,
        getPolymarketBalanceInteractor = getBalance,
        recordPolymarketConfirmation = recordConfirmation,
        polymarketRepository = polymarketRepository,
        positionsRepository = positionsRepository,
        collateralCurrencyFactory = mockk { every { createDepositTarget() } returns COLLATERAL },
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    private companion object {
        val COLLATERAL: CryptoCurrency.Token = mockk(relaxed = true)

        const val BRIDGE_ADDRESS = "0xbridge0000000000000000000000000000000001"

        val WALLET = UserWalletId("011")

        val ACTIVE = PredictionAccountStatusValue.Active(
            source = StatusSource.ACTUAL,
            balance = BigDecimal("40"),
            positionsValue = null,
            fiatRate = null,
            depositCurrency = COLLATERAL,
            depositAddress = BRIDGE_ADDRESS,
        )

        const val DEPOSIT_WALLET = "0xdeposit000000000000000000000000000000002"

        val OTHER_WALLET = UserWalletId("022")

        val ADDRESSES: PolymarketAddresses = mockk(relaxed = true) {
            every { depositWalletAddress } returns DEPOSIT_WALLET
        }

        val OTHER_ADDRESSES: PolymarketAddresses = mockk(relaxed = true) {
            every { depositWalletAddress } returns "0xdeposit000000000000000000000000000000003"
        }

        fun walletState(status: PolymarketWalletStatus) = PolymarketWalletState(
            status = status,
            depositWalletAddress = "0xdeposit",
        )

        fun onboarding(stage: PredictionAccountStatusValue.Onboarding.Stage) =
            PredictionAccountStatusValue.Onboarding(source = StatusSource.ACTUAL, stage = stage)
    }
}