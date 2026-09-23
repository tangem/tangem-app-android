package com.tangem.data.collectibles.account.store

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.data.collectibles.account.converter.CollectiblesAccountStatusValueDTOConverter
import com.tangem.data.collectibles.account.entity.CollectiblesAccountDTO
import com.tangem.data.collectibles.account.entity.CollectiblesAccountStatusValueDTO
import com.tangem.data.collectibles.account.entity.CollectiblesVaultBalanceDTO
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.CollectiblesAccountStatusValue
import com.tangem.domain.collectibles.account.CollectiblesVaultCurrencyFactory
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.core.TestAppCoroutineScope
import com.tangem.test.mock.collectibles.MockCollectibles
import com.tangem.test.core.datastore.MockStateDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class CollectiblesAccountStoreTest {

    @Test
    fun `GIVEN nothing stored WHEN get THEN emits null without waiting`() = runTest {
        // Arrange
        val store = createStore(testScope = this)

        // Act & Assert — no advanceUntilIdle: the cache load has not run yet, and the flow must still emit
        store.get(WALLET_A).test {
            assertThat(awaitItem()).isNull()
        }
    }

    @Test
    fun `GIVEN a record WHEN store THEN it is kept for this wallet only and persisted`() = runTest {
        // Arrange
        val persisted = MockStateDataStore<WalletIdWithCollectiblesAccountDTO>(default = emptyMap())
        val store = createStore(testScope = this, persistenceDataStore = persisted)

        // Act
        store.store(userWalletId = WALLET_A, value = ONBOARDED)

        // Assert
        assertThat(store.get(WALLET_A).first()).isEqualTo(ONBOARDED)
        assertThat(store.get(WALLET_B).first()).isNull()
        assertThat(persisted.data.first()).containsExactly(
            WALLET_A.stringValue,
            ACCOUNT_DTO,
        )
    }

    @Test
    fun `GIVEN a state that is not an account WHEN store THEN it is refused`() = runTest {
        // Arrange
        val store = createStore(testScope = this)

        // Act
        store.store(userWalletId = WALLET_A, value = CollectiblesAccountStatusValue.Error.Unavailable)

        // Assert
        assertThat(store.getSyncOrNull(WALLET_A)).isNull()
    }

    @Test
    fun `GIVEN a cached balance WHEN the record is stored again THEN the balance survives`() = runTest {
        // Arrange
        val store = createStore(testScope = this)
        store.store(userWalletId = WALLET_A, value = ONBOARDED)
        store.storeBalance(userWalletId = WALLET_A, balance = BALANCE)

        // Act
        store.store(userWalletId = WALLET_A, value = CollectiblesAccountStatusValue.UpdateRequired(StatusSource.ACTUAL))

        // Assert
        assertThat(store.getSyncOrNull(WALLET_A)).isEqualTo(
            CollectiblesAccountStatusValue.UpdateRequired(source = StatusSource.ACTUAL, vaultBalance = BALANCE),
        )
    }

    /** A balance without a record has nothing to belong to: the wallet has no collectibles account. */
    @Test
    fun `GIVEN nothing recorded WHEN storeBalance THEN nothing is stored`() = runTest {
        // Arrange
        val store = createStore(testScope = this)

        // Act
        store.storeBalance(userWalletId = WALLET_A, balance = BALANCE)

        // Assert
        assertThat(store.getSyncOrNull(WALLET_A)).isNull()
    }

    @Test
    fun `GIVEN a persisted status WHEN the store is created THEN it comes back marked as cached`() = runTest {
        // Arrange
        val persisted = MockStateDataStore<WalletIdWithCollectiblesAccountDTO>(
            default = mapOf(
                WALLET_A.stringValue to CollectiblesAccountDTO(status = ONBOARDED_WITH_BALANCE_DTO),
            ),
        )

        // Act
        val store = createStore(testScope = this, persistenceDataStore = persisted)
        advanceUntilIdle()

        // Assert
        assertThat(store.getSyncOrNull(WALLET_A)).isEqualTo(
            CollectiblesAccountStatusValue.Onboarded(source = StatusSource.CACHE, vaultBalance = BALANCE),
        )
    }

    @Test
    fun `GIVEN a persisted status WHEN getSyncOrNull runs before the preload THEN it waits for the disk`() = runTest {
        // Arrange — no advanceUntilIdle: the cache load has not run yet
        val persisted = MockStateDataStore<WalletIdWithCollectiblesAccountDTO>(
            default = mapOf(WALLET_A.stringValue to ACCOUNT_DTO),
        )
        val store = createStore(testScope = this, persistenceDataStore = persisted)

        // Act
        val actual = store.getSyncOrNull(WALLET_A)

        // Assert
        assertThat(actual).isEqualTo(CollectiblesAccountStatusValue.Onboarded(source = StatusSource.CACHE))
    }

    @Test
    fun `GIVEN a stored status WHEN markUnrefreshed THEN the disk copy stays untouched`() = runTest {
        // Arrange
        val persisted = MockStateDataStore<WalletIdWithCollectiblesAccountDTO>(default = emptyMap())
        val store = createStore(testScope = this, persistenceDataStore = persisted)
        store.store(userWalletId = WALLET_A, value = ONBOARDED)
        store.storeBalance(userWalletId = WALLET_A, balance = BALANCE)

        // Act
        store.markUnrefreshed(userWalletId = WALLET_A)

        // Assert
        assertThat(store.getSyncOrNull(WALLET_A)).isEqualTo(
            CollectiblesAccountStatusValue.Onboarded(source = StatusSource.ONLY_CACHE, vaultBalance = BALANCE),
        )
        assertThat(persisted.data.first()).containsExactly(
            WALLET_A.stringValue,
            CollectiblesAccountDTO(status = ONBOARDED_WITH_BALANCE_DTO),
        )
    }

    @Test
    fun `GIVEN nothing recorded WHEN markUnrefreshed THEN nothing is stored`() = runTest {
        // Arrange
        val store = createStore(testScope = this)

        // Act
        store.markUnrefreshed(userWalletId = WALLET_A)

        // Assert
        assertThat(store.getSyncOrNull(WALLET_A)).isNull()
    }

    @Test
    fun `GIVEN a stored status WHEN clear THEN it is gone from runtime and disk`() = runTest {
        // Arrange
        val persisted = MockStateDataStore<WalletIdWithCollectiblesAccountDTO>(default = emptyMap())
        val store = createStore(testScope = this, persistenceDataStore = persisted)
        store.store(userWalletId = WALLET_A, value = ONBOARDED)
        store.store(userWalletId = WALLET_B, value = ONBOARDED)

        // Act
        store.clear(WALLET_A)

        // Assert
        assertThat(store.getSyncOrNull(WALLET_A)).isNull()
        assertThat(store.getSyncOrNull(WALLET_B)).isEqualTo(ONBOARDED)
        assertThat(persisted.data.first().keys).containsExactly(WALLET_B.stringValue)
    }

    /** The key is created before `init_vault` runs, so it has to attach to an entry that has no status yet. */
    @Test
    fun `GIVEN no account WHEN storeDelegate THEN it is kept and a later status does not drop it`() = runTest {
        // Arrange
        val persisted = MockStateDataStore<WalletIdWithCollectiblesAccountDTO>(default = emptyMap())
        val store = createStore(testScope = this, persistenceDataStore = persisted)

        // Act
        store.storeDelegate(userWalletId = WALLET_A, hotWalletId = DELEGATE)
        store.store(userWalletId = WALLET_A, value = ONBOARDED)

        // Assert
        assertThat(store.getDelegate(WALLET_A)).isEqualTo(DELEGATE)
        assertThat(persisted.data.first()).containsExactly(
            WALLET_A.stringValue,
            CollectiblesAccountDTO(status = CollectiblesAccountStatusValueDTO.Onboarded(), delegateHotWalletId = DELEGATE),
        )
    }

    @Test
    fun `GIVEN a delegate WHEN removeDelegate THEN the status of the account survives`() = runTest {
        // Arrange
        val store = createStore(testScope = this)
        store.store(userWalletId = WALLET_A, value = ONBOARDED)
        store.storeDelegate(userWalletId = WALLET_A, hotWalletId = DELEGATE)

        // Act
        store.removeDelegate(WALLET_A)

        // Assert
        assertThat(store.getDelegate(WALLET_A)).isNull()
        assertThat(store.getSyncOrNull(WALLET_A)).isEqualTo(ONBOARDED)
    }

    @Test
    fun `GIVEN a delegate of another wallet WHEN getDelegate THEN this wallet has none`() = runTest {
        // Arrange
        val store = createStore(testScope = this)
        store.storeDelegate(userWalletId = WALLET_B, hotWalletId = DELEGATE)

        // Act & Assert
        assertThat(store.getDelegate(WALLET_A)).isNull()
        assertThat(store.getDelegate(WALLET_B)).isEqualTo(DELEGATE)
    }

    @Test
    fun `GIVEN a stored delegate WHEN clear THEN it goes with the account`() = runTest {
        // Arrange
        val store = createStore(testScope = this)
        store.store(userWalletId = WALLET_A, value = ONBOARDED)
        store.storeDelegate(userWalletId = WALLET_A, hotWalletId = DELEGATE)

        // Act
        store.clear(WALLET_A)

        // Assert
        assertThat(store.getDelegate(WALLET_A)).isNull()
    }

    private fun createStore(
        testScope: TestScope,
        persistenceDataStore: MockStateDataStore<WalletIdWithCollectiblesAccountDTO> = MockStateDataStore(emptyMap()),
    ) = CollectiblesAccountStore(
        runtimeStore = RuntimeSharedStore(),
        persistenceDataStore = persistenceDataStore,
        converter = CollectiblesAccountStatusValueDTOConverter(
            currencyFactory = CollectiblesVaultCurrencyFactory(MockCollectibles::usdc),
        ),
        scope = TestAppCoroutineScope(testScope),
    )

    private companion object {
        val WALLET_A = UserWalletId("011")
        val WALLET_B = UserWalletId("022")

        val ONBOARDED = CollectiblesAccountStatusValue.Onboarded(source = StatusSource.CACHE)

        val BALANCE = MockCollectibles.vaultBalance(amount = BigDecimal("20"))
        val BALANCE_DTO = CollectiblesVaultBalanceDTO(
            usdcAmount = BigDecimal("20"),
            usdcMint = MockCollectibles.USDC_MINT,
        )

        val ACCOUNT_DTO = CollectiblesAccountDTO(
            status = CollectiblesAccountStatusValueDTO.Onboarded(balance = null),
        )
        val ONBOARDED_WITH_BALANCE_DTO = CollectiblesAccountStatusValueDTO.Onboarded(balance = BALANCE_DTO)

        const val DELEGATE = "hot-1"
    }
}