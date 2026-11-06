package com.tangem.data.collectibles.account.flow

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.data.collectibles.account.DefaultCollectiblesAccountRepository
import com.tangem.data.collectibles.account.converter.CollectiblesAccountStatusValueDTOConverter
import com.tangem.data.collectibles.account.store.CollectiblesAccountStore
import com.tangem.data.collectibles.account.store.WalletIdWithCollectiblesAccountDTO
import com.tangem.domain.collectibles.CollectiblesAccountError
import com.tangem.domain.collectibles.account.CollectiblesVaultCurrencyFactory
import com.tangem.domain.collectibles.account.derivation.CollectiblesOwnerDeriver
import com.tangem.domain.collectibles.account.flow.CollectiblesAccountStatusFetcher
import com.tangem.domain.collectibles.delegate.CollectiblesHotDelegate
import com.tangem.domain.collectibles.delegate.HotDelegatePublicKey
import com.tangem.domain.collectibles.vault.GachaVaultError
import com.tangem.domain.collectibles.vault.repository.GachaVaultRepository
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.CollectiblesAccountStatusValue
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.core.TestAppCoroutineScope
import com.tangem.test.mock.collectibles.MockCollectibles
import com.tangem.test.core.datastore.MockStateDataStore
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.time.TestTimeSource
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal class DefaultCollectiblesAccountStatusFetcherTest {

    private val ownerDeriver: CollectiblesOwnerDeriver = mockk()
    private val gachaVaultRepository: GachaVaultRepository = mockk()
    private val hotDelegate: CollectiblesHotDelegate = mockk()

    @BeforeEach
    fun linkToThisPhone() {
        coEvery { gachaVaultRepository.getVaultHotDelegate(USER_WALLET) } returns DELEGATE.right()
        coEvery { hotDelegate.getPublicKeyOrNull(any()) } returns HotDelegatePublicKey(DELEGATE)
    }

    @Test
    fun `GIVEN the vault answers WHEN invoke THEN the amount is recorded as actual`() = runTest {
        // Arrange
        coEvery { ownerDeriver.storedOwner(WALLET) } returns OWNER
        coEvery { gachaVaultRepository.getVaultBalance(USER_WALLET, CACHED) } returns READ.right()
        val store = createStore(testScope = this, recorded = ONBOARDED.copy(vaultBalance = CACHED))

        // Act
        val actual = createFetcher(store).invoke(PARAMS)

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        assertThat(store.getSyncOrNull(WALLET)).isEqualTo(
            CollectiblesAccountStatusValue.Onboarded(source = StatusSource.ACTUAL, vaultBalance = READ),
        )
        coVerify(exactly = 1) { gachaVaultRepository.getVaultBalance(USER_WALLET, CACHED) }
    }

    @Test
    fun `GIVEN the vault read fails WHEN invoke THEN the cached amount is kept and marked stale`() =
        runTest {
            // Arrange
            coEvery { ownerDeriver.storedOwner(WALLET) } returns OWNER
            coEvery { gachaVaultRepository.getVaultBalance(USER_WALLET, CACHED) } returns UNREADABLE.left()
            val store = createStore(testScope = this, recorded = ONBOARDED.copy(vaultBalance = CACHED))

            // Act
            val actual = createFetcher(store).invoke(PARAMS)

            // Assert
            assertThat(actual).isEqualTo(Unit.right())
            assertThat(store.getSyncOrNull(WALLET)).isEqualTo(
                CollectiblesAccountStatusValue.Onboarded(source = StatusSource.ONLY_CACHE, vaultBalance = CACHED),
            )
        }

    /** A wallet restored on a new phone: the vault exists, its owner key has not been derived here yet. */
    @Test
    fun `GIVEN a record and no derived owner key WHEN invoke THEN the derivation is reported as missing`() = runTest {
        // Arrange
        coEvery { ownerDeriver.storedOwner(WALLET) } returns null
        val store = createStore(testScope = this, recorded = ONBOARDED.copy(vaultBalance = CACHED))

        // Act
        createFetcher(store).invoke(PARAMS)

        // Assert — the cached amount goes with it: it was read through a key this device no longer has
        assertThat(store.getSyncOrNull(WALLET)).isEqualTo(CollectiblesAccountStatusValue.MissedDerivation)
    }

    @Test
    fun `GIVEN nothing recorded WHEN invoke THEN nothing is stored and the owner key is never asked for`() = runTest {
        // Arrange
        val store = createStore(testScope = this, recorded = null)

        // Act
        createFetcher(store).invoke(PARAMS)

        // Assert
        assertThat(store.getSyncOrNull(WALLET)).isNull()
    }

    @Test
    fun `GIVEN the vault answers to another phone WHEN invoke THEN the account awaits the delegate update`() =
        runTest {
            // Arrange
            coEvery { ownerDeriver.storedOwner(WALLET) } returns OWNER
            coEvery { gachaVaultRepository.getVaultHotDelegate(USER_WALLET) } returns "another".right()
            coEvery { gachaVaultRepository.getVaultBalance(USER_WALLET, CACHED) } returns READ.right()
            val store = createStore(testScope = this, recorded = ONBOARDED.copy(vaultBalance = CACHED))

            // Act
            createFetcher(store).invoke(PARAMS)

            // Assert
            assertThat(store.getSyncOrNull(WALLET)).isEqualTo(
                CollectiblesAccountStatusValue.UpdateRequired(source = StatusSource.ACTUAL, vaultBalance = READ),
            )
        }

    @Test
    fun `GIVEN a relinked vault WHEN invoke THEN the account is onboarded again`() = runTest {
        // Arrange
        coEvery { ownerDeriver.storedOwner(WALLET) } returns OWNER
        coEvery { gachaVaultRepository.getVaultBalance(USER_WALLET, CACHED) } returns READ.right()
        val recorded = CollectiblesAccountStatusValue.UpdateRequired(source = StatusSource.CACHE, vaultBalance = CACHED)
        val store = createStore(testScope = this, recorded = recorded)

        // Act
        createFetcher(store).invoke(PARAMS)

        // Assert
        assertThat(store.getSyncOrNull(WALLET)).isEqualTo(
            CollectiblesAccountStatusValue.Onboarded(source = StatusSource.ACTUAL, vaultBalance = READ),
        )
    }

    @Test
    fun `GIVEN a relink still landing WHEN the vault answers to the old delegate THEN the account stays onboarded`() =
        runTest {
            // Arrange
            coEvery { ownerDeriver.storedOwner(WALLET) } returns OWNER
            coEvery { gachaVaultRepository.getVaultHotDelegate(USER_WALLET) } returns "another".right()
            coEvery { gachaVaultRepository.getVaultBalance(USER_WALLET, CACHED) } returns READ.right()
            val store = createStore(testScope = this, recorded = ONBOARDED.copy(vaultBalance = CACHED))
            val accountRepository = createAccountRepository(store)
            accountRepository.markRelinkSent(WALLET)

            // Act
            createFetcher(store, accountRepository = accountRepository).invoke(PARAMS)

            // Assert
            assertThat(store.getSyncOrNull(WALLET)).isEqualTo(
                CollectiblesAccountStatusValue.Onboarded(source = StatusSource.ACTUAL, vaultBalance = READ),
            )
        }

    @Test
    fun `GIVEN a relink sent WHEN the vault answers to this device THEN the relink is no longer pending`() =
        runTest {
            // Arrange
            coEvery { ownerDeriver.storedOwner(WALLET) } returns OWNER
            coEvery { gachaVaultRepository.getVaultBalance(USER_WALLET, CACHED) } returns READ.right()
            val store = createStore(testScope = this, recorded = ONBOARDED.copy(vaultBalance = CACHED))
            val accountRepository = createAccountRepository(store)
            accountRepository.markRelinkSent(WALLET)

            // Act
            createFetcher(store, accountRepository = accountRepository).invoke(PARAMS)

            // Assert
            assertThat(accountRepository.isRelinkPending(WALLET)).isFalse()
        }

    @Test
    fun `GIVEN a relink finishes during the vault read WHEN invoke THEN its result is not overwritten`() = runTest {
        // Arrange
        coEvery { ownerDeriver.storedOwner(WALLET) } returns OWNER
        coEvery { gachaVaultRepository.getVaultHotDelegate(USER_WALLET) } returns "another".right()
        val recorded = CollectiblesAccountStatusValue.UpdateRequired(source = StatusSource.CACHE, vaultBalance = CACHED)
        val store = createStore(testScope = this, recorded = recorded)
        val accountRepository = createAccountRepository(store)
        coEvery { gachaVaultRepository.getVaultBalance(USER_WALLET, CACHED) } coAnswers {
            accountRepository.markRelinkSent(WALLET)
            store.store(WALLET, CollectiblesAccountStatusValue.Onboarded(source = StatusSource.ACTUAL))
            READ.right()
        }

        // Act
        createFetcher(store, accountRepository = accountRepository).invoke(PARAMS)

        // Assert
        assertThat(store.getSyncOrNull(WALLET)).isEqualTo(
            CollectiblesAccountStatusValue.Onboarded(source = StatusSource.ACTUAL, vaultBalance = READ),
        )
    }

    @Test
    fun `GIVEN the account is cleared during the vault read WHEN invoke THEN it is not brought back`() = runTest {
        // Arrange
        coEvery { ownerDeriver.storedOwner(WALLET) } returns OWNER
        val store = createStore(testScope = this, recorded = ONBOARDED.copy(vaultBalance = CACHED))
        coEvery { gachaVaultRepository.getVaultBalance(USER_WALLET, CACHED) } coAnswers {
            store.clear(WALLET)
            READ.right()
        }

        // Act
        createFetcher(store).invoke(PARAMS)

        // Assert
        assertThat(store.getSyncOrNull(WALLET)).isNull()
    }

    /** Neither a read that did not answer nor a vault without a delegate says the vault moved to another phone. */
    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `GIVEN an onboarded account and no delegate answer WHEN invoke THEN it stays onboarded`(isReadFailed: Boolean) =
        runTest {
            // Arrange
            coEvery { ownerDeriver.storedOwner(WALLET) } returns OWNER
            coEvery { gachaVaultRepository.getVaultHotDelegate(USER_WALLET) } returns
                if (isReadFailed) UNREADABLE.left() else null.right()
            coEvery { gachaVaultRepository.getVaultBalance(USER_WALLET, CACHED) } returns READ.right()
            val store = createStore(testScope = this, recorded = ONBOARDED.copy(vaultBalance = CACHED))

            // Act
            createFetcher(store).invoke(PARAMS)

            // Assert
            assertThat(store.getSyncOrNull(WALLET)).isEqualTo(
                CollectiblesAccountStatusValue.Onboarded(source = StatusSource.ACTUAL, vaultBalance = READ),
            )
        }

    @Test
    fun `GIVEN a locked wallet WHEN invoke THEN the cached amount is kept and marked stale`() = runTest {
        // Arrange
        val store = createStore(testScope = this, recorded = ONBOARDED.copy(vaultBalance = CACHED))

        // Act
        createFetcher(store, userWallet = LOCKED_WALLET).invoke(PARAMS)

        // Assert
        assertThat(store.getSyncOrNull(WALLET)).isEqualTo(
            CollectiblesAccountStatusValue.Onboarded(source = StatusSource.ONLY_CACHE, vaultBalance = CACHED),
        )
    }

    @Test
    fun `GIVEN the owner key read throws WHEN invoke THEN the failure is reported and the cache is kept`() = runTest {
        // Arrange
        coEvery { ownerDeriver.storedOwner(WALLET) } throws IllegalStateException("keystore is locked")
        val store = createStore(testScope = this, recorded = ONBOARDED.copy(vaultBalance = CACHED))

        // Act
        val actual = createFetcher(store).invoke(PARAMS)

        // Assert
        assertThat(actual.isLeft()).isTrue()
        assertThat(store.getSyncOrNull(WALLET)).isEqualTo(
            CollectiblesAccountStatusValue.Onboarded(source = StatusSource.ONLY_CACHE, vaultBalance = CACHED),
        )
    }

    private suspend fun createStore(testScope: TestScope, recorded: CollectiblesAccountStatusValue?) =
        CollectiblesAccountStore(
            runtimeStore = RuntimeSharedStore(),
            persistenceDataStore = MockStateDataStore<WalletIdWithCollectiblesAccountDTO>(default = emptyMap()),
            converter = CONVERTER,
            scope = TestAppCoroutineScope(testScope),
        ).apply {
            recorded?.let { store(userWalletId = WALLET, value = it) }
        }

    private fun createFetcher(
        store: CollectiblesAccountStore,
        userWallet: UserWallet = USER_WALLET,
        accountRepository: DefaultCollectiblesAccountRepository = createAccountRepository(store),
    ) = DefaultCollectiblesAccountStatusFetcher(
        collectiblesAccountStore = store,
        collectiblesAccountRepository = accountRepository,
        userWalletsListRepository = mockk {
            every { userWallets } returns MutableStateFlow<List<UserWallet>?>(listOf(userWallet))
        },
        collectiblesOwnerDeriver = ownerDeriver,
        gachaVaultRepository = gachaVaultRepository,
        hotDelegate = hotDelegate,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    private fun createAccountRepository(store: CollectiblesAccountStore) = DefaultCollectiblesAccountRepository(
        collectiblesAccountStore = store,
        walletAccountsFetcher = mockk(),
        walletAccountsSaver = mockk(),
        timeSource = TestTimeSource(),
    )

    private companion object {
        const val OWNER = "8kXHqmvJ3sYVsBLnr5aXqjy6r2ZxHhCTvJH1DPDcnUXG"
        const val DELEGATE = "delegate"

        val WALLET = UserWalletId("011")
        val PARAMS = CollectiblesAccountStatusFetcher.Params(userWalletId = WALLET)

        val USER_WALLET: UserWallet = mockk<UserWallet.Hot>(relaxed = true) {
            every { walletId } returns WALLET
            every { isLocked } returns false
        }

        val LOCKED_WALLET: UserWallet = mockk<UserWallet.Hot>(relaxed = true) {
            every { walletId } returns WALLET
            every { isLocked } returns true
        }

        val CONVERTER = CollectiblesAccountStatusValueDTOConverter(
            currencyFactory = CollectiblesVaultCurrencyFactory(MockCollectibles::usdc),
        )

        val ONBOARDED = CollectiblesAccountStatusValue.Onboarded(source = StatusSource.CACHE)
        val CACHED = MockCollectibles.vaultBalance(amount = BigDecimal("9"))
        val READ = MockCollectibles.vaultBalance(amount = BigDecimal("12"))
        val UNREADABLE = CollectiblesAccountError.Vault(error = GachaVaultError.WalletUnavailable)
    }
}