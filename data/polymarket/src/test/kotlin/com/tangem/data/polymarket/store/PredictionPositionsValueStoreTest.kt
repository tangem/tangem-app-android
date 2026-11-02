package com.tangem.data.polymarket.store

import androidx.datastore.core.DataStore
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.data.polymarket.entity.PredictionPositionsValueDTO
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PredictionPositionsValue
import com.tangem.test.core.TestAppCoroutineScope
import com.tangem.test.core.datastore.MockStateDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class PredictionPositionsValueStoreTest {

    @Test
    fun `GIVEN nothing stored WHEN get THEN emits unknown without waiting`() = runTest {
        // Arrange
        val store = createStore(testScope = this)

        // Act
        store.get(WALLET_A).test {
            // Assert
            assertThat(awaitItem()).isEqualTo(PredictionPositionsValue.Unknown)
        }
    }

    @Test
    fun `GIVEN nothing stored WHEN getSyncOrNull THEN it is null`() = runTest {
        // Arrange
        val store = createStore(testScope = this)

        // Act
        val actual = store.getSyncOrNull(WALLET_A)

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN a value WHEN store THEN it is emitted and persisted`() = runTest {
        // Arrange
        val persisted = MockStateDataStore<WalletIdWithPositionsValueDTO>(default = emptyMap())
        val store = createStore(testScope = this, persistenceDataStore = persisted)

        // Act
        store.store(userWalletId = WALLET_A, value = VALUE)

        // Assert
        assertThat(store.get(WALLET_A).first()).isEqualTo(VALUE)
        assertThat(persisted.data.first())
            .isEqualTo(mapOf(WALLET_A.stringValue to PredictionPositionsValueDTO(amount = BigDecimal("7.5"))))
    }

    @Test
    fun `GIVEN a persisted value WHEN the store is created THEN it comes back as cached`() = runTest {
        // Arrange
        val persisted = MockStateDataStore<WalletIdWithPositionsValueDTO>(
            default = mapOf(WALLET_A.stringValue to PredictionPositionsValueDTO(amount = BigDecimal("7.5"))),
        )
        val store = createStore(testScope = this, persistenceDataStore = persisted)

        // Act
        advanceUntilIdle()

        // Assert
        assertThat(store.getSyncOrNull(WALLET_A)).isEqualTo(VALUE.copy(source = StatusSource.CACHE))
    }

    @Test
    fun `GIVEN a value stored before the cache loads WHEN it lands THEN it survives the preload`() = runTest {
        // Arrange
        val persisted = MockStateDataStore<WalletIdWithPositionsValueDTO>(
            default = mapOf(WALLET_A.stringValue to PERSISTED, WALLET_B.stringValue to PERSISTED),
        )
        val store = createStore(testScope = this, persistenceDataStore = persisted)

        // Act
        store.store(userWalletId = WALLET_A, value = VALUE)
        advanceUntilIdle()

        // Assert
        assertThat(store.getSyncOrNull(WALLET_A)).isEqualTo(VALUE)
        assertThat(store.getSyncOrNull(WALLET_B))
            .isEqualTo(PredictionPositionsValue.Known(amount = BigDecimal("1"), source = StatusSource.CACHE))
    }

    @Test
    fun `GIVEN a wallet cleared before the cache loads WHEN it lands THEN the entry stays gone`() = runTest {
        // Arrange
        val persisted = MockStateDataStore<WalletIdWithPositionsValueDTO>(
            default = mapOf(WALLET_A.stringValue to PERSISTED, WALLET_B.stringValue to PERSISTED),
        )
        val store = createStore(testScope = this, persistenceDataStore = persisted)

        // Act
        store.clear(WALLET_A)
        advanceUntilIdle()

        // Assert
        assertThat(store.getSyncOrNull(WALLET_A)).isNull()
        assertThat(persisted.data.first()).containsExactly(WALLET_B.stringValue, PERSISTED)
    }

    @Test
    fun `GIVEN a stored value WHEN markUnrefreshed THEN only the source changes`() = runTest {
        // Arrange
        val store = createStore(testScope = this)
        store.store(userWalletId = WALLET_A, value = VALUE)

        // Act
        store.markUnrefreshed(userWalletId = WALLET_A)

        // Assert
        assertThat(store.get(WALLET_A).first()).isEqualTo(VALUE.copy(source = StatusSource.ONLY_CACHE))
    }

    @Test
    fun `GIVEN nothing stored WHEN markUnrefreshed THEN the value is unavailable and not persisted`() = runTest {
        // Arrange
        val persisted = MockStateDataStore<WalletIdWithPositionsValueDTO>(default = emptyMap())
        val store = createStore(testScope = this, persistenceDataStore = persisted)

        // Act
        store.markUnrefreshed(userWalletId = WALLET_A)

        // Assert
        assertThat(store.get(WALLET_A).first()).isEqualTo(PredictionPositionsValue.Unavailable)
        assertThat(persisted.data.first()).isEmpty()
    }

    @Test
    fun `GIVEN an unavailable value WHEN store THEN the value replaces it`() = runTest {
        // Arrange
        val store = createStore(testScope = this)
        store.markUnrefreshed(userWalletId = WALLET_A)

        // Act
        store.store(userWalletId = WALLET_A, value = VALUE)

        // Assert
        assertThat(store.get(WALLET_A).first()).isEqualTo(VALUE)
    }

    @Test
    fun `GIVEN two wallets stored WHEN one is cleared THEN the other stays on both layers`() = runTest {
        // Arrange
        val persisted = MockStateDataStore<WalletIdWithPositionsValueDTO>(default = emptyMap())
        val store = createStore(testScope = this, persistenceDataStore = persisted)
        store.store(userWalletId = WALLET_A, value = VALUE)
        store.store(userWalletId = WALLET_B, value = VALUE)

        // Act
        store.clear(WALLET_A)

        // Assert
        assertThat(store.getSyncOrNull(WALLET_A)).isNull()
        assertThat(store.get(WALLET_B).first()).isEqualTo(VALUE)
        assertThat(persisted.data.first().keys).containsExactly(WALLET_B.stringValue)
    }

    private fun createStore(
        testScope: TestScope,
        persistenceDataStore: DataStore<WalletIdWithPositionsValueDTO> = MockStateDataStore(emptyMap()),
    ) = PredictionPositionsValueStore(
        runtimeStore = RuntimeSharedStore(),
        persistenceDataStore = persistenceDataStore,
        scope = TestAppCoroutineScope(testScope),
    )

    private companion object {
        val WALLET_A = UserWalletId("011")
        val WALLET_B = UserWalletId("022")

        val VALUE = PredictionPositionsValue.Known(amount = BigDecimal("7.5"), source = StatusSource.ACTUAL)
        val PERSISTED = PredictionPositionsValueDTO(amount = BigDecimal("1"))
    }
}