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
        val store = createStore(testScope = this)
        store.store(userWalletId = WALLET_A, value = ACTIVE)

        // Act
        store.updateStatusSource(userWalletId = WALLET_A, source = StatusSource.ONLY_CACHE)

        // Assert
        assertThat(store.getSyncOrNull(WALLET_A)).isEqualTo(ACTIVE.copy(source = StatusSource.ONLY_CACHE))
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