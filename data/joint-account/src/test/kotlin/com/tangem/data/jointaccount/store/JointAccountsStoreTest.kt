package com.tangem.data.jointaccount.store

import androidx.datastore.core.DataStore
import com.google.common.truth.Truth.assertThat
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.data.jointaccount.converter.JointAccountDMConverter
import com.tangem.data.jointaccount.createJointAccount
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.core.TestAppCoroutineScope
import com.tangem.test.core.datastore.MockStateDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class JointAccountsStoreTest {

    private val converter = JointAccountDMConverter()
    private val walletId = UserWalletId("011121314151617181910A0B0C0D0E0F011121314151617181910A0B0C0D0E0F")

    private fun createStore(
        persistence: MockStateDataStore<WalletIdWithJointAccountsDM> = MockStateDataStore(default = emptyMap()),
        scope: TestAppCoroutineScope,
    ): JointAccountsStore = JointAccountsStore(
        runtimeStore = RuntimeSharedStore(),
        persistenceDataStore = persistence,
        converter = converter,
        scope = scope,
    )

    @Test
    fun `GIVEN empty persistence WHEN created THEN get emits null for unknown wallet instead of hanging`() = runTest {
        // Arrange
        val store = createStore(scope = TestAppCoroutineScope(testScope = this))
        advanceUntilIdle()

        // Act
        val actual = store.get(walletId).first()

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN persisted accounts WHEN created THEN hydrated with CACHE source`() = runTest {
        // Arrange
        val account = createJointAccount(source = StatusSource.ACTUAL)
        val persistence = MockStateDataStore<WalletIdWithJointAccountsDM>(default = emptyMap())
        persistence.updateData { mapOf(walletId.stringValue to listOf(converter.convert(account))) }

        // Act
        val store = createStore(persistence = persistence, scope = TestAppCoroutineScope(testScope = this))
        advanceUntilIdle()

        // Assert
        assertThat(store.get(walletId).first())
            .containsExactly(account.copy(source = StatusSource.CACHE))
    }

    @Test
    fun `GIVEN store WHEN store accounts THEN emitted and persisted`() = runTest {
        // Arrange
        val persistence = MockStateDataStore<WalletIdWithJointAccountsDM>(default = emptyMap())
        val store = createStore(persistence = persistence, scope = TestAppCoroutineScope(testScope = this))
        advanceUntilIdle()
        val account = createJointAccount()

        // Act
        store.store(walletId, listOf(account))

        // Assert
        assertThat(store.get(walletId).first()).containsExactly(account)
        assertThat(persistence.data.first())
            .containsExactly(walletId.stringValue, listOf(converter.convert(account)))
    }

    @Test
    fun `GIVEN cached accounts WHEN updateStatusSource THEN runtime downgraded persistence untouched`() = runTest {
        // Arrange
        val persistence = MockStateDataStore<WalletIdWithJointAccountsDM>(default = emptyMap())
        val store = createStore(persistence = persistence, scope = TestAppCoroutineScope(testScope = this))
        advanceUntilIdle()
        val account = createJointAccount(source = StatusSource.ACTUAL)
        store.store(walletId, listOf(account))
        val persistedBefore = persistence.data.first()

        // Act
        store.updateStatusSource(walletId, StatusSource.ONLY_CACHE)

        // Assert
        assertThat(store.get(walletId).first()).containsExactly(account.copy(source = StatusSource.ONLY_CACHE))
        assertThat(persistence.data.first()).isEqualTo(persistedBefore)
    }

    @Test
    fun `GIVEN no data for wallet WHEN updateStatusSource THEN no-op`() = runTest {
        // Arrange
        val store = createStore(scope = TestAppCoroutineScope(testScope = this))
        advanceUntilIdle()

        // Act
        store.updateStatusSource(walletId, StatusSource.ONLY_CACHE)

        // Assert
        assertThat(store.get(walletId).first()).isNull()
        assertThat(store.contains(walletId)).isFalse()
    }

    @Test
    fun `GIVEN fetch lands before hydration WHEN hydration completes THEN fresh data is not overwritten`() = runTest {
        // Arrange
        val staleAccount = createJointAccount(status = JointAccount.Status.PENDING, source = StatusSource.ACTUAL)
        val persistence = MockStateDataStore<WalletIdWithJointAccountsDM>(default = emptyMap())
        persistence.updateData { mapOf(walletId.stringValue to listOf(converter.convert(staleAccount))) }
        val store = createStore(persistence = persistence, scope = TestAppCoroutineScope(testScope = this))

        // Act: the fetch writes before the hydration coroutine gets to run
        val freshAccount = createJointAccount(status = JointAccount.Status.ACTIVE, source = StatusSource.ACTUAL)
        store.store(walletId, listOf(freshAccount))
        advanceUntilIdle()

        // Assert
        assertThat(store.get(walletId).first()).containsExactly(freshAccount)
    }

    @Test
    fun `GIVEN persistence write fails WHEN store THEN runtime updated and no exception`() = runTest {
        // Arrange
        val failingPersistence = object : DataStore<WalletIdWithJointAccountsDM> {
            override val data = flowOf(emptyMap<String, List<JointAccountDM>>())
            override suspend fun updateData(
                transform: suspend (WalletIdWithJointAccountsDM) -> WalletIdWithJointAccountsDM,
            ): WalletIdWithJointAccountsDM = error("disk full")
        }
        val store = JointAccountsStore(
            runtimeStore = RuntimeSharedStore(),
            persistenceDataStore = failingPersistence,
            converter = converter,
            scope = TestAppCoroutineScope(testScope = this),
        )
        advanceUntilIdle()
        val account = createJointAccount()

        // Act
        store.store(walletId, listOf(account))

        // Assert
        assertThat(store.get(walletId).first()).containsExactly(account)
    }

    @Test
    fun `GIVEN two wallets WHEN store both THEN kept independently`() = runTest {
        // Arrange
        val store = createStore(scope = TestAppCoroutineScope(testScope = this))
        advanceUntilIdle()
        val otherWalletId = UserWalletId("FF1121314151617181910A0B0C0D0E0F011121314151617181910A0B0C0D0EFF")
        val first = createJointAccount(status = JointAccount.Status.PENDING)
        val second = createJointAccount(status = JointAccount.Status.ACTIVE, address = "0x1")

        // Act
        store.store(walletId, listOf(first))
        store.store(otherWalletId, listOf(second))

        // Assert
        assertThat(store.get(walletId).first()).containsExactly(first)
        assertThat(store.get(otherWalletId).first()).containsExactly(second)
    }
}