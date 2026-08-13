package com.tangem.data.polymarket.flow

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.data.polymarket.store.PredictionAccountStatusStore
import com.tangem.data.polymarket.store.WalletIdWithPredictionStatus
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.PredictionAccountStatusValue
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.flow.PredictionAccountStatusFetcher
import com.tangem.domain.polymarket.interactor.GetPolymarketBalanceInteractor
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketAuthError
import com.tangem.domain.polymarket.model.PolymarketBalanceAllowance
import com.tangem.domain.polymarket.model.PolymarketOnboardingError
import com.tangem.domain.polymarket.model.PolymarketWalletState
import com.tangem.domain.polymarket.model.PolymarketWalletStatus
import com.tangem.domain.polymarket.usecase.CheckPolymarketGeoblockUseCase
import com.tangem.domain.polymarket.usecase.DerivePolymarketAddressesUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketWalletStatusUseCase
import com.tangem.domain.quotes.single.SingleQuoteStatusFetcher
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.TestAppCoroutineScope
import com.tangem.test.core.datastore.MockStateDataStore
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultPredictionAccountStatusFetcherTest {

    private val deriveAddresses: DerivePolymarketAddressesUseCase = mockk()
    private val getWalletStatus: GetPolymarketWalletStatusUseCase = mockk()
    private val getBalance: GetPolymarketBalanceInteractor = mockk()
    private val checkGeoblock: CheckPolymarketGeoblockUseCase = mockk()
    private val quoteFetcher: SingleQuoteStatusFetcher = mockk(relaxed = true)

    @BeforeEach
    fun resetMocks() {
        clearMocks(deriveAddresses, getWalletStatus, getBalance, checkGeoblock, quoteFetcher)
        coEvery { quoteFetcher.invoke(any()) } returns Unit.right()
    }

    /**
     * The refresh runs for every wallet, including ones that never opened the feature. Deriving the addresses for
     * real opens a card session or unlocks the wallet, so it may only read what is already stored.
     */
    @Test
    fun `GIVEN no stored addresses WHEN invoke THEN it reports not onboarded without deriving`() = runTest {
        // Arrange
        coEvery { deriveAddresses.stored(WALLET) } returns null
        val store = createStore(testScope = this)

        // Act
        createFetcher(store).invoke(PredictionAccountStatusFetcher.Params(WALLET))

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
        createFetcher(store).invoke(PredictionAccountStatusFetcher.Params(WALLET))

        // Assert
        assertThat(store.getSyncOrNull(WALLET)).isEqualTo(model.expected)
    }

    @Test
    fun `GIVEN a ready wallet WHEN invoke THEN the balance is stored and the collateral quote is refreshed`() =
        runTest {
            // Arrange
            coEvery { deriveAddresses.stored(WALLET) } returns ADDRESSES
            coEvery { getWalletStatus.invoke(ADDRESSES) } returns
                walletState(PolymarketWalletStatus.READY_TO_TRADE).right()
            coEvery { getBalance.invoke(ADDRESSES) } returns
                PolymarketBalanceAllowance(balance = BigDecimal("40"), allowance = null).right()
            coEvery { checkGeoblock.invoke() } returns false.right()
            val store = createStore(testScope = this)

            // Act
            createFetcher(store).invoke(PredictionAccountStatusFetcher.Params(WALLET))

            // Assert — the rate itself is not stored, it belongs to the currency the user may switch at any time
            assertThat(store.getSyncOrNull(WALLET)).isEqualTo(
                PredictionAccountStatusValue.Active(
                    source = StatusSource.ACTUAL,
                    balance = BigDecimal("40"),
                    fiatRate = null,
                    isTradingAllowed = true,
                ),
            )
            coVerify(exactly = 1) { quoteFetcher.invoke(any()) }
        }

    @Test
    fun `GIVEN a blocked region WHEN invoke THEN the balance is kept and trading is not allowed`() = runTest {
        // Arrange
        coEvery { deriveAddresses.stored(WALLET) } returns ADDRESSES
        coEvery { getWalletStatus.invoke(ADDRESSES) } returns walletState(PolymarketWalletStatus.READY_TO_TRADE).right()
        coEvery { getBalance.invoke(ADDRESSES) } returns
            PolymarketBalanceAllowance(balance = BigDecimal("40"), allowance = null).right()
        coEvery { checkGeoblock.invoke() } returns true.right()
        val store = createStore(testScope = this)

        // Act
        createFetcher(store).invoke(PredictionAccountStatusFetcher.Params(WALLET))

        // Assert
        val actual = store.getSyncOrNull(WALLET) as PredictionAccountStatusValue.Active
        assertThat(actual.isTradingAllowed).isFalse()
        assertThat(actual.balance).isEqualTo(BigDecimal("40"))
    }

    @Test
    fun `GIVEN no credentials on this device WHEN invoke THEN the setup is reported as unfinished`() = runTest {
        // Arrange
        coEvery { deriveAddresses.stored(WALLET) } returns ADDRESSES
        coEvery { getWalletStatus.invoke(ADDRESSES) } returns walletState(PolymarketWalletStatus.READY_TO_TRADE).right()
        coEvery { getBalance.invoke(ADDRESSES) } returns PolymarketAuthError.KeyNotFound.left()
        val store = createStore(testScope = this)

        // Act
        createFetcher(store).invoke(PredictionAccountStatusFetcher.Params(WALLET))

        // Assert
        assertThat(store.getSyncOrNull(WALLET)).isEqualTo(
            PredictionAccountStatusValue.Onboarding(
                source = StatusSource.ACTUAL,
                stage = PredictionAccountStatusValue.Onboarding.Stage.DEPLOYED,
            ),
        )
    }

    @Test
    fun `GIVEN the refresh throws WHEN invoke THEN the cached status is kept and marked un-refreshed`() = runTest {
        // Arrange
        val cached = PredictionAccountStatusValue.Active(
            source = StatusSource.ACTUAL,
            balance = BigDecimal("40"),
            fiatRate = null,
            isTradingAllowed = true,
        )
        val store = createStore(testScope = this)
        store.store(userWalletId = WALLET, value = cached)
        coEvery { deriveAddresses.stored(WALLET) } throws IllegalStateException("no connection")

        // Act
        val actual = createFetcher(store).invoke(PredictionAccountStatusFetcher.Params(WALLET))

        // Assert
        assertThat(actual.isLeft()).isTrue()
        assertThat(store.getSyncOrNull(WALLET)).isEqualTo(cached.copy(source = StatusSource.ONLY_CACHE))
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
        StatusTestModel(
            status = PolymarketWalletStatus.UNKNOWN,
            expected = PredictionAccountStatusValue.Error.Unavailable,
        ),
    )

    @Test
    fun `GIVEN the wallet status fails WHEN invoke THEN the account is unavailable`() = runTest {
        // Arrange
        coEvery { deriveAddresses.stored(WALLET) } returns ADDRESSES
        coEvery { getWalletStatus.invoke(ADDRESSES) } returns PolymarketOnboardingError.Unknown.left()
        val store = createStore(testScope = this)

        // Act
        createFetcher(store).invoke(PredictionAccountStatusFetcher.Params(WALLET))

        // Assert
        assertThat(store.getSyncOrNull(WALLET)).isEqualTo(PredictionAccountStatusValue.Error.Unavailable)
    }

    private fun createStore(testScope: TestScope) = PredictionAccountStatusStore(
        runtimeStore = RuntimeSharedStore(),
        persistenceDataStore = MockStateDataStore<WalletIdWithPredictionStatus>(default = emptyMap()),
        scope = TestAppCoroutineScope(testScope),
    )

    private fun createFetcher(store: PredictionAccountStatusStore) = DefaultPredictionAccountStatusFetcher(
        statusStore = store,
        derivePolymarketAddressesUseCase = deriveAddresses,
        getPolymarketWalletStatusUseCase = getWalletStatus,
        getPolymarketBalanceInteractor = getBalance,
        checkPolymarketGeoblockUseCase = checkGeoblock,
        singleQuoteStatusFetcher = quoteFetcher,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    private companion object {
        val WALLET = UserWalletId("011")

        // The constructor is internal to the domain module, and the fetcher only passes the pair around
        val ADDRESSES: PolymarketAddresses = mockk(relaxed = true)

        fun walletState(status: PolymarketWalletStatus) = PolymarketWalletState(
            status = status,
            depositWalletAddress = "0xdeposit",
        )

        fun onboarding(stage: PredictionAccountStatusValue.Onboarding.Stage) =
            PredictionAccountStatusValue.Onboarding(source = StatusSource.ACTUAL, stage = stage)
    }
}