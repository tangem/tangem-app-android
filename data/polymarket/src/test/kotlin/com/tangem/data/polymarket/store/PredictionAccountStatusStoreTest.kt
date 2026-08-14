package com.tangem.data.polymarket.store

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.PredictionAccountStatusValue
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.core.TestAppCoroutineScope
import com.tangem.test.core.datastore.MockStateDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class PredictionAccountStatusStoreTest {

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
    fun `GIVEN a stored value WHEN get THEN emits it`() = runTest {
        // Arrange
        val store = createStore(testScope = this)

        // Act
        store.store(userWalletId = WALLET_A, value = ACTIVE)

        // Assert
        assertThat(store.get(WALLET_A).first()).isEqualTo(ACTIVE)
        assertThat(store.get(WALLET_B).first()).isNull()
    }

    @Test
    fun `GIVEN a persisted value WHEN the store is created THEN it is served from the cache`() = runTest {
        // Arrange
        val persisted = MockStateDataStore<WalletIdWithPredictionStatus>(
            default = mapOf(WALLET_A.stringValue to ACTIVE),
        )
        val store = createStore(testScope = this, persistenceDataStore = persisted)

        // Act
        advanceUntilIdle()

        // Assert
        assertThat(store.getSyncOrNull(WALLET_A)).isEqualTo(ACTIVE)
    }

    @Test
    fun `GIVEN a stored value WHEN clear THEN it is dropped from both stores`() = runTest {
        // Arrange
        val persisted = MockStateDataStore<WalletIdWithPredictionStatus>(default = emptyMap())
        val store = createStore(testScope = this, persistenceDataStore = persisted)
        store.store(userWalletId = WALLET_A, value = ACTIVE)
        store.store(userWalletId = WALLET_B, value = ACTIVE)

        // Act
        store.clear(WALLET_A)

        // Assert
        assertThat(store.getSyncOrNull(WALLET_A)).isNull()
        assertThat(store.getSyncOrNull(WALLET_B)).isEqualTo(ACTIVE)
        assertThat(persisted.data.first()).containsExactly(WALLET_B.stringValue, ACTIVE)
    }

    @Test
    fun `GIVEN a stored value WHEN updateStatusSource THEN only the source changes`() = runTest {
        // Arrange
        val persisted = MockStateDataStore<WalletIdWithPredictionStatus>(default = emptyMap())
        val store = createStore(testScope = this, persistenceDataStore = persisted)
        store.store(userWalletId = WALLET_A, value = ACTIVE)

        // Act
        store.updateStatusSource(userWalletId = WALLET_A, source = StatusSource.ONLY_CACHE)

        // Assert — "could not be refreshed" is about this session, so it must not survive to the next launch
        assertThat(store.getSyncOrNull(WALLET_A)).isEqualTo(ACTIVE.copy(source = StatusSource.ONLY_CACHE))
        assertThat(persisted.data.first()[WALLET_A.stringValue]).isEqualTo(ACTIVE)
    }

    @Test
    fun `GIVEN a refresh issued before the cache loads WHEN it lands THEN it survives the preload`() = runTest {
        // Arrange — the persisted snapshot is older than what the refresh is about to write
        val persisted = MockStateDataStore<WalletIdWithPredictionStatus>(
            default = mapOf(WALLET_A.stringValue to ACTIVE, WALLET_B.stringValue to ACTIVE),
        )
        val store = createStore(testScope = this, persistenceDataStore = persisted)
        val refreshed = ACTIVE.copy(balance = BigDecimal("99"))

        // Act — issued before the init coroutine has run: the write waits for the preload instead of racing it
        store.store(userWalletId = WALLET_A, value = refreshed)
        advanceUntilIdle()

        // Assert
        assertThat(store.getSyncOrNull(WALLET_A)).isEqualTo(refreshed)
        assertThat(store.getSyncOrNull(WALLET_B)).isEqualTo(ACTIVE)
    }

    @Test
    fun `GIVEN a wallet cleared before the cache loads WHEN it lands THEN the entry stays gone`() = runTest {
        // Arrange — the deleted wallet is still in the snapshot the init coroutine is about to read
        val persisted = MockStateDataStore<WalletIdWithPredictionStatus>(
            default = mapOf(WALLET_A.stringValue to ACTIVE, WALLET_B.stringValue to ACTIVE),
        )
        val store = createStore(testScope = this, persistenceDataStore = persisted)

        // Act
        store.clear(WALLET_A)
        advanceUntilIdle()

        // Assert — a snapshot taken before the deletion must not hand the balance to a re-added wallet
        assertThat(store.getSyncOrNull(WALLET_A)).isNull()
        assertThat(persisted.data.first()).containsExactly(WALLET_B.stringValue, ACTIVE)
        assertThat(store.getSyncOrNull(WALLET_B)).isEqualTo(ACTIVE)
    }

    @Test
    fun `GIVEN nothing stored WHEN updateStatusSource THEN nothing is stored`() = runTest {
        // Arrange
        val store = createStore(testScope = this)

        // Act
        store.updateStatusSource(userWalletId = WALLET_A, source = StatusSource.ONLY_CACHE)

        // Assert
        assertThat(store.getSyncOrNull(WALLET_A)).isNull()
    }

    private fun createStore(
        testScope: TestScope,
        persistenceDataStore: MockStateDataStore<WalletIdWithPredictionStatus> = MockStateDataStore(emptyMap()),
    ) = PredictionAccountStatusStore(
        runtimeStore = RuntimeSharedStore(),
        persistenceDataStore = persistenceDataStore,
        scope = TestAppCoroutineScope(testScope),
    )

    private companion object {
        val WALLET_A = UserWalletId("011")
        val WALLET_B = UserWalletId("022")

        val ACTIVE = PredictionAccountStatusValue.Active(
            source = StatusSource.ACTUAL,
            balance = BigDecimal("12.5"),
            fiatRate = null,
            isTradingAllowed = true,
        )
    }
}