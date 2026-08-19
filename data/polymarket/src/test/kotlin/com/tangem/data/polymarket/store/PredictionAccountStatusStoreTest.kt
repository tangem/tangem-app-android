package com.tangem.data.polymarket.store

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.data.polymarket.entity.PredictionAccountStatusValueDTO
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
    fun `GIVEN a persisted value WHEN the store is created THEN it comes back marked as cached`() = runTest {
        // Arrange
        val persisted = MockStateDataStore<WalletIdWithPredictionStatusDTO>(
            default = mapOf(WALLET_A.stringValue to ACTIVE_DTO),
        )
        val store = createStore(testScope = this, persistenceDataStore = persisted)

        // Act
        advanceUntilIdle()

        // Assert — a restored value has not been refreshed in this session, whatever it claimed when written
        assertThat(store.getSyncOrNull(WALLET_A)).isEqualTo(ACTIVE.copy(source = StatusSource.CACHE))
    }

    @Test
    fun `GIVEN a transient state WHEN store THEN it does not reach the disk`() = runTest {
        // Arrange
        val persisted = MockStateDataStore<WalletIdWithPredictionStatusDTO>(default = emptyMap())
        val store = createStore(testScope = this, persistenceDataStore = persisted)
        store.store(userWalletId = WALLET_A, value = ACTIVE)

        // Act
        store.store(userWalletId = WALLET_A, value = PredictionAccountStatusValue.Error.Unavailable)

        // Assert — a momentary failure must not be read back next launch as the account's real state
        assertThat(store.getSyncOrNull(WALLET_A)).isEqualTo(PredictionAccountStatusValue.Error.Unavailable)
        assertThat(persisted.data.first()[WALLET_A.stringValue]).isEqualTo(ACTIVE_DTO)
    }

    @Test
    fun `GIVEN a stored value WHEN clear THEN it is dropped from both stores`() = runTest {
        // Arrange
        val persisted = MockStateDataStore<WalletIdWithPredictionStatusDTO>(default = emptyMap())
        val store = createStore(testScope = this, persistenceDataStore = persisted)
        store.store(userWalletId = WALLET_A, value = ACTIVE)
        store.store(userWalletId = WALLET_B, value = ACTIVE)

        // Act
        store.clear(WALLET_A)

        // Assert
        assertThat(store.getSyncOrNull(WALLET_A)).isNull()
        assertThat(store.getSyncOrNull(WALLET_B)).isEqualTo(ACTIVE)
        assertThat(persisted.data.first()).containsExactly(WALLET_B.stringValue, ACTIVE_DTO)
    }

    @Test
    fun `GIVEN a stored value WHEN markUnrefreshed THEN only the source changes`() = runTest {
        // Arrange
        val persisted = MockStateDataStore<WalletIdWithPredictionStatusDTO>(default = emptyMap())
        val store = createStore(testScope = this, persistenceDataStore = persisted)
        store.store(userWalletId = WALLET_A, value = ACTIVE)

        // Act
        store.markUnrefreshed(userWalletId = WALLET_A)

        // Assert — "could not be refreshed" is about this session, so it must not survive to the next launch
        assertThat(store.getSyncOrNull(WALLET_A)).isEqualTo(ACTIVE.copy(source = StatusSource.ONLY_CACHE))
        assertThat(persisted.data.first()[WALLET_A.stringValue]).isEqualTo(ACTIVE_DTO)
    }

    @Test
    fun `GIVEN a refresh issued before the cache loads WHEN it lands THEN it survives the preload`() = runTest {
        // Arrange — the persisted snapshot is older than what the refresh is about to write
        val persisted = MockStateDataStore<WalletIdWithPredictionStatusDTO>(
            default = mapOf(WALLET_A.stringValue to ACTIVE_DTO, WALLET_B.stringValue to ACTIVE_DTO),
        )
        val store = createStore(testScope = this, persistenceDataStore = persisted)
        val refreshed = ACTIVE.copy(balance = BigDecimal("99"))

        // Act — issued before the init coroutine has run: the write waits for the preload instead of racing it
        store.store(userWalletId = WALLET_A, value = refreshed)
        advanceUntilIdle()

        // Assert
        assertThat(store.getSyncOrNull(WALLET_A)).isEqualTo(refreshed)
        assertThat(store.getSyncOrNull(WALLET_B)).isEqualTo(ACTIVE.copy(source = StatusSource.CACHE))
    }

    @Test
    fun `GIVEN a wallet cleared before the cache loads WHEN it lands THEN the entry stays gone`() = runTest {
        // Arrange — the deleted wallet is still in the snapshot the init coroutine is about to read
        val persisted = MockStateDataStore<WalletIdWithPredictionStatusDTO>(
            default = mapOf(WALLET_A.stringValue to ACTIVE_DTO, WALLET_B.stringValue to ACTIVE_DTO),
        )
        val store = createStore(testScope = this, persistenceDataStore = persisted)

        // Act
        store.clear(WALLET_A)
        advanceUntilIdle()

        // Assert — a snapshot taken before the deletion must not hand the balance to a re-added wallet
        assertThat(store.getSyncOrNull(WALLET_A)).isNull()
        assertThat(persisted.data.first()).containsExactly(WALLET_B.stringValue, ACTIVE_DTO)
        assertThat(store.getSyncOrNull(WALLET_B)).isEqualTo(ACTIVE.copy(source = StatusSource.CACHE))
    }

    /**
     * With nothing cached there is no balance to keep, and an absent entry reads as [PredictionAccountStatusValue
     * .Loading] downstream — which would shimmer forever for a balance that is not coming.
     */
    @Test
    fun `GIVEN nothing stored WHEN markUnrefreshed THEN the failure is recorded`() = runTest {
        // Arrange
        val persisted = MockStateDataStore<WalletIdWithPredictionStatusDTO>(default = emptyMap())
        val store = createStore(testScope = this, persistenceDataStore = persisted)

        // Act
        store.markUnrefreshed(userWalletId = WALLET_A)

        // Assert — recorded for this session only, so the disk copy stays untouched
        assertThat(store.getSyncOrNull(WALLET_A)).isEqualTo(PredictionAccountStatusValue.Error.Unavailable)
        assertThat(persisted.data.first()).isEmpty()
    }

    private fun createStore(
        testScope: TestScope,
        persistenceDataStore: MockStateDataStore<WalletIdWithPredictionStatusDTO> = MockStateDataStore(emptyMap()),
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

        val ACTIVE_DTO = PredictionAccountStatusValueDTO.Active(
            balance = BigDecimal("12.5"),
            isTradingAllowed = true,
        )
    }
}