package com.tangem.data.yield.supply.status.store

import androidx.datastore.core.DataStore
import com.google.common.truth.Truth.assertThat
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.data.yield.supply.status.USDT_ADDRESS
import com.tangem.data.yield.supply.status.createStatus
import com.tangem.data.yield.supply.status.createToken
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.yield.supply.models.status.YieldModuleStatus
import com.tangem.test.core.TestAppCoroutineScope
import com.tangem.test.core.datastore.MockStateDataStore
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class DefaultYieldModuleStatusStoreTest {

    private val userWalletId = UserWalletId("011")
    private val otherWalletId = UserWalletId("022")

    private val usdc = createToken()
    private val usdt = createToken(contractAddress = USDT_ADDRESS, symbol = "USDT")

    private val activeUsdc = createStatus(
        token = usdc,
        isActive = true,
        isInitialized = true,
        isAllowedToSpend = true,
        effectiveProtocolBalance = BigDecimal("12.5"),
    )
    private val inactiveUsdt = createStatus(token = usdt)

    @Nested
    inner class Initialization {

        @Test
        fun `GIVEN persistence emits nothing WHEN store created THEN runtime is initialized empty`() = runTest {
            // Arrange
            val runtimeStore = RuntimeSharedStore<WalletIdWithStatuses>()
            val persistenceStore: DataStore<WalletIdWithStatusDMs> = mockk()
            every { persistenceStore.data } returns emptyFlow()

            // Act
            val store = DefaultYieldModuleStatusStore(runtimeStore, persistenceStore, TestAppCoroutineScope())

            // Assert
            assertThat(runtimeStore.getSyncOrNull()).isEqualTo(emptyMap<UserWalletId, Set<YieldModuleStatus>>())
            assertThat(store.get(userWalletId).first()).isEmpty()
        }

        @Test
        fun `GIVEN reading persistence throws WHEN store created THEN readers emit empty and writes still work`() =
            runTest {
                // Arrange
                val runtimeStore = RuntimeSharedStore<WalletIdWithStatuses>()
                val persistenceStore: DataStore<WalletIdWithStatusDMs> = mockk()
                every { persistenceStore.data } returns flow { throw IllegalStateException("corrupted") }
                coEvery { persistenceStore.updateData(any()) } returns emptyMap()
                val store = DefaultYieldModuleStatusStore(runtimeStore, persistenceStore, TestAppCoroutineScope())

                // Act
                val beforeStore = store.get(userWalletId).first()
                store.storeActual(userWalletId, setOf(activeUsdc))

                // Assert
                assertThat(beforeStore).isEmpty()
                assertThat(store.get(userWalletId).first()).containsExactly(activeUsdc)
            }

        @Test
        fun `GIVEN persisted statuses WHEN store created THEN runtime holds them as CACHE`() = runTest {
            // Arrange
            val runtimeStore = RuntimeSharedStore<WalletIdWithStatuses>()
            val persistenceStore = MockStateDataStore<WalletIdWithStatusDMs>(
                default = mapOf(userWalletId.stringValue to setOf(activeUsdc.toDataModel())),
            )

            // Act
            DefaultYieldModuleStatusStore(runtimeStore, persistenceStore, TestAppCoroutineScope())

            // Assert
            assertThat(runtimeStore.getSyncOrNull())
                .isEqualTo(mapOf(userWalletId to setOf(activeUsdc.copy(source = StatusSource.CACHE))))
        }

        @Test
        fun `GIVEN status stored before hydration WHEN store created THEN fresh status wins per key`() = runTest {
            // Arrange
            val runtimeStore = RuntimeSharedStore<WalletIdWithStatuses>()
            val exitedUsdc = createStatus(token = usdc, isInitialized = true)
            runtimeStore.store(mapOf(userWalletId to setOf(exitedUsdc)))
            val persistenceStore = MockStateDataStore<WalletIdWithStatusDMs>(
                default = mapOf(
                    userWalletId.stringValue to setOf(activeUsdc.toDataModel(), inactiveUsdt.toDataModel()),
                    otherWalletId.stringValue to setOf(inactiveUsdt.toDataModel()),
                ),
            )

            // Act
            DefaultYieldModuleStatusStore(runtimeStore, persistenceStore, TestAppCoroutineScope())

            // Assert
            val cachedUsdt = inactiveUsdt.copy(source = StatusSource.CACHE)
            assertThat(runtimeStore.getSyncOrNull()).isEqualTo(
                mapOf(
                    userWalletId to setOf(exitedUsdc, cachedUsdt),
                    otherWalletId to setOf(cachedUsdt),
                ),
            )
        }
    }

    @Nested
    inner class GetSync {

        @Test
        fun `GIVEN runtime holds the wallet WHEN getSync THEN runtime statuses are returned`() = runTest {
            // Arrange
            val runtimeStore = RuntimeSharedStore<WalletIdWithStatuses>()
            val persistenceStore = MockStateDataStore<WalletIdWithStatusDMs>(default = emptyMap())
            val store = DefaultYieldModuleStatusStore(runtimeStore, persistenceStore, TestAppCoroutineScope())
            runtimeStore.store(mapOf(userWalletId to setOf(activeUsdc)))

            // Act
            val actual = store.getSync(userWalletId)

            // Assert
            assertThat(actual).containsExactly(activeUsdc)
        }

        @Test
        fun `GIVEN another wallet stored during hydration WHEN getSync THEN the cached status is still returned`() =
            runTest {
                // Arrange
                val gate = CompletableDeferred<Unit>()
                val persistenceStore = GatedDataStore(
                    snapshot = mapOf(userWalletId.stringValue to setOf(activeUsdc.toDataModel())),
                    gate = gate,
                )
                val runtimeStore = RuntimeSharedStore<WalletIdWithStatuses>()
                val store = DefaultYieldModuleStatusStore(runtimeStore, persistenceStore, TestAppCoroutineScope())
                val actual = async { store.getSync(userWalletId) }
                runCurrent()

                // Act
                gate.complete(Unit)

                // Assert
                assertThat(actual.await()).containsExactly(activeUsdc.copy(source = StatusSource.CACHE))
            }

        @Test
        fun `GIVEN wallet stored nowhere WHEN getSync THEN returns empty`() = runTest {
            // Arrange
            val persistenceStore = MockStateDataStore<WalletIdWithStatusDMs>(default = emptyMap())
            val store = DefaultYieldModuleStatusStore(
                RuntimeSharedStore(),
                persistenceStore,
                TestAppCoroutineScope(),
            )

            // Act
            val actual = store.getSync(userWalletId)

            // Assert
            assertThat(actual).isEmpty()
        }
    }

    @Nested
    inner class Updates {

        private val runtimeStore = RuntimeSharedStore<WalletIdWithStatuses>()
        private val persistenceStore = MockStateDataStore<WalletIdWithStatusDMs>(default = emptyMap())
        private val store = DefaultYieldModuleStatusStore(runtimeStore, persistenceStore, TestAppCoroutineScope())

        @Test
        fun `GIVEN statuses WHEN storeActual THEN both stores hold them as ACTUAL`() = runTest {
            // Arrange
            val cachedUsdc = activeUsdc.copy(source = StatusSource.CACHE)

            // Act
            store.storeActual(userWalletId, setOf(cachedUsdc, inactiveUsdt))

            // Assert
            assertThat(store.get(userWalletId).first()).containsExactly(activeUsdc, inactiveUsdt)
            assertThat(persistenceStore.data.first())
                .isEqualTo(mapOf(userWalletId.stringValue to setOf(activeUsdc.toDataModel(), inactiveUsdt.toDataModel())))
        }

        @Test
        fun `GIVEN stored status WHEN storeActual with the same key THEN status is replaced`() = runTest {
            // Arrange
            store.storeActual(userWalletId, setOf(activeUsdc, inactiveUsdt))
            val exitedUsdc = createStatus(token = usdc, isInitialized = true)

            // Act
            store.storeActual(userWalletId, setOf(exitedUsdc))

            // Assert
            assertThat(store.get(userWalletId).first()).containsExactly(exitedUsdc, inactiveUsdt)
            assertThat(persistenceStore.data.first().getValue(userWalletId.stringValue))
                .containsExactly(exitedUsdc.toDataModel(), inactiveUsdt.toDataModel())
        }

        @Test
        fun `GIVEN stored statuses WHEN refresh THEN only refreshed keys become CACHE`() = runTest {
            // Arrange
            store.storeActual(userWalletId, setOf(activeUsdc, inactiveUsdt))

            // Act
            store.refresh(userWalletId, setOf(activeUsdc.key))

            // Assert
            assertThat(store.get(userWalletId).first())
                .containsExactly(activeUsdc.copy(source = StatusSource.CACHE), inactiveUsdt)
        }

        @Test
        fun `GIVEN stored status WHEN storeError THEN it becomes ONLY_CACHE and missing key stays absent`() = runTest {
            // Arrange
            store.storeActual(userWalletId, setOf(activeUsdc))

            // Act
            store.storeError(userWalletId, setOf(activeUsdc.key, inactiveUsdt.key))

            // Assert
            assertThat(store.get(userWalletId).first())
                .containsExactly(activeUsdc.copy(source = StatusSource.ONLY_CACHE))
        }

        @Test
        fun `GIVEN stored statuses WHEN clear THEN only cleared keys are removed from both stores`() = runTest {
            // Arrange
            store.storeActual(userWalletId, setOf(activeUsdc, inactiveUsdt))

            // Act
            store.clear(userWalletId, setOf(activeUsdc.key))

            // Assert
            assertThat(store.get(userWalletId).first()).containsExactly(inactiveUsdt)
            assertThat(persistenceStore.data.first().getValue(userWalletId.stringValue))
                .containsExactly(inactiveUsdt.toDataModel())
        }

        @Test
        fun `GIVEN two wallets WHEN remove one THEN the other wallet is kept`() = runTest {
            // Arrange
            store.storeActual(userWalletId, setOf(activeUsdc))
            store.storeActual(otherWalletId, setOf(inactiveUsdt))

            // Act
            store.remove(listOf(userWalletId))

            // Assert
            assertThat(store.get(userWalletId).first()).isEmpty()
            assertThat(store.get(otherWalletId).first()).containsExactly(inactiveUsdt)
            assertThat(persistenceStore.data.first().keys).containsExactly(otherWalletId.stringValue)
        }
    }

    @Nested
    inner class DuringHydration {

        private val gate = CompletableDeferred<Unit>()
        private val runtimeStore = RuntimeSharedStore<WalletIdWithStatuses>()
        private val persistenceStore = GatedDataStore(
            snapshot = mapOf(userWalletId.stringValue to setOf(activeUsdc.toDataModel())),
            gate = gate,
        )
        private val store = DefaultYieldModuleStatusStore(runtimeStore, persistenceStore, TestAppCoroutineScope())

        @Test
        fun `GIVEN the disk snapshot is read WHEN the wallet is removed before it is applied THEN it stays removed`() =
            runTest {
                // Arrange
                val removal = launch { store.remove(listOf(userWalletId)) }
                runCurrent()

                // Act
                gate.complete(Unit)
                removal.join()

                // Assert
                assertThat(runtimeStore.getSyncOrNull()?.get(userWalletId)).isNull()
                assertThat(persistenceStore.stored().keys).doesNotContain(userWalletId.stringValue)
            }

        @Test
        fun `GIVEN the disk snapshot is read WHEN a refresh fails before it is applied THEN ONLY_CACHE is kept`() =
            runTest {
                // Arrange
                val failure = launch { store.storeError(userWalletId, setOf(activeUsdc.key)) }
                runCurrent()

                // Act
                gate.complete(Unit)
                failure.join()

                // Assert
                assertThat(store.get(userWalletId).first())
                    .containsExactly(activeUsdc.copy(source = StatusSource.ONLY_CACHE))
            }
    }

    @Test
    fun `GIVEN the first disk write is slow WHEN a second storeActual overlaps THEN runtime and disk end with the second`() =
        runTest {
            // Arrange
            val runtimeStore = RuntimeSharedStore<WalletIdWithStatuses>()
            val persistenceStore = SlowFirstWriteDataStore()
            val store = DefaultYieldModuleStatusStore(runtimeStore, persistenceStore, TestAppCoroutineScope())
            val exitedUsdc = createStatus(token = usdc, isInitialized = true)
            val first = launch { store.storeActual(userWalletId, setOf(activeUsdc)) }
            runCurrent()
            val second = launch { store.storeActual(userWalletId, setOf(exitedUsdc)) }
            runCurrent()

            // Act
            persistenceStore.firstWriteGate.complete(Unit)
            first.join()
            second.join()

            // Assert
            assertThat(store.get(userWalletId).first()).containsExactly(exitedUsdc)
            assertThat(persistenceStore.data.first().getValue(userWalletId.stringValue))
                .containsExactly(exitedUsdc.toDataModel())
        }

    @Test
    fun `GIVEN persistence fails WHEN remove THEN runtime is still removed`() = runTest {
        // Arrange
        val runtimeStore = RuntimeSharedStore<WalletIdWithStatuses>()
        val persistenceStore: DataStore<WalletIdWithStatusDMs> = mockk()
        every { persistenceStore.data } returns emptyFlow()
        coEvery { persistenceStore.updateData(any()) } throws IllegalStateException("disk")
        runtimeStore.store(mapOf(userWalletId to setOf(activeUsdc)))
        val store = DefaultYieldModuleStatusStore(runtimeStore, persistenceStore, TestAppCoroutineScope())

        // Act
        store.remove(listOf(userWalletId))

        // Assert
        assertThat(runtimeStore.getSyncOrNull()).isEqualTo(emptyMap<UserWalletId, Any>())
    }
}

/**
 * Serves a disk snapshot only once [gate] opens, while writes go straight through — the window in which the snapshot
 * is already taken but not applied to the runtime store yet.
 */
private class GatedDataStore(
    private val snapshot: WalletIdWithStatusDMs,
    private val gate: CompletableDeferred<Unit>,
) : DataStore<WalletIdWithStatusDMs> {

    private val delegate = MockStateDataStore(default = snapshot)

    override val data: Flow<WalletIdWithStatusDMs> = flow {
        gate.await()
        emit(snapshot)
    }

    override suspend fun updateData(
        transform: suspend (t: WalletIdWithStatusDMs) -> WalletIdWithStatusDMs,
    ): WalletIdWithStatusDMs = delegate.updateData(transform)

    suspend fun stored(): WalletIdWithStatusDMs = delegate.data.first()
}

/** Holds the first write until [firstWriteGate] opens; later writes go straight through */
private class SlowFirstWriteDataStore : DataStore<WalletIdWithStatusDMs> {

    val firstWriteGate = CompletableDeferred<Unit>()

    private val delegate = MockStateDataStore<WalletIdWithStatusDMs>(default = emptyMap())
    private var isFirstWrite = true

    override val data: Flow<WalletIdWithStatusDMs> = delegate.data

    override suspend fun updateData(
        transform: suspend (t: WalletIdWithStatusDMs) -> WalletIdWithStatusDMs,
    ): WalletIdWithStatusDMs {
        if (isFirstWrite) {
            isFirstWrite = false
            firstWriteGate.await()
        }
        return delegate.updateData(transform)
    }
}