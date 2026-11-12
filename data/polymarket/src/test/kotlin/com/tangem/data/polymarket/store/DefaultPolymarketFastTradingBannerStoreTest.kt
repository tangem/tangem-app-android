package com.tangem.data.polymarket.store

import androidx.datastore.core.DataStore
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.sessionkey.PolymarketFastTradingBannerKind
import com.tangem.test.core.datastore.MockStateDataStore
import app.cash.turbine.test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.io.IOException
import java.time.Instant

internal class DefaultPolymarketFastTradingBannerStoreTest {

    @Test
    fun `GIVEN nothing dismissed WHEN dismissedAt THEN emits null`() = runTest {
        // Arrange
        val store = createStore()

        // Act
        val actual = store.dismissedAt(WALLET_A, PolymarketFastTradingBannerKind.ACTIVATE).first()

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN a dismissal stored WHEN dismissedAt THEN emits its instant`() = runTest {
        // Arrange
        val store = createStore()

        // Act
        store.dismiss(WALLET_A, PolymarketFastTradingBannerKind.ACTIVATE, at = DISMISSED_AT)

        // Assert
        assertThat(store.dismissedAt(WALLET_A, PolymarketFastTradingBannerKind.ACTIVATE).first())
            .isEqualTo(DISMISSED_AT)
    }

    @Test
    fun `GIVEN dismissals persisted under the iOS kind keys WHEN dismissedAt THEN emits them`() = runTest {
        // Arrange
        val store = DefaultPolymarketFastTradingBannerStore(
            dataStore = MockStateDataStore(
                default = mapOf(
                    WALLET_A.stringValue to mapOf(
                        "activate_banner" to DISMISSED_AT.toEpochMilli(),
                        "extend_banner" to EXTEND_DISMISSED_AT.toEpochMilli(),
                    ),
                ),
            ),
        )

        // Act
        val activate = store.dismissedAt(WALLET_A, PolymarketFastTradingBannerKind.ACTIVATE).first()
        val extend = store.dismissedAt(WALLET_A, PolymarketFastTradingBannerKind.EXTEND).first()

        // Assert
        assertThat(listOf(activate, extend)).containsExactly(DISMISSED_AT, EXTEND_DISMISSED_AT).inOrder()
    }

    @Test
    fun `GIVEN one kind dismissed WHEN dismissedAt of the other kind THEN emits null`() = runTest {
        // Arrange
        val store = createStore()

        // Act
        store.dismiss(WALLET_A, PolymarketFastTradingBannerKind.ACTIVATE, at = DISMISSED_AT)

        // Assert
        assertThat(store.dismissedAt(WALLET_A, PolymarketFastTradingBannerKind.EXTEND).first()).isNull()
    }

    @Test
    fun `GIVEN one wallet dismissed WHEN dismissedAt of another wallet THEN emits null`() = runTest {
        // Arrange
        val store = createStore()

        // Act
        store.dismiss(WALLET_A, PolymarketFastTradingBannerKind.ACTIVATE, at = DISMISSED_AT)

        // Assert
        assertThat(store.dismissedAt(WALLET_B, PolymarketFastTradingBannerKind.ACTIVATE).first()).isNull()
    }

    @Test
    fun `GIVEN the data store fails to read WHEN dismissedAt THEN emits null`() = runTest {
        // Arrange
        val store = DefaultPolymarketFastTradingBannerStore(dataStore = FailingDataStore())

        // Act
        val actual = store.dismissedAt(WALLET_A, PolymarketFastTradingBannerKind.ACTIVATE).first()

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN the data store fails to write WHEN dismiss THEN it does not throw`() = runTest {
        // Arrange
        val store = DefaultPolymarketFastTradingBannerStore(dataStore = FailingDataStore())

        // Act
        val actual = runCatching { store.dismiss(WALLET_A, PolymarketFastTradingBannerKind.ACTIVATE, DISMISSED_AT) }

        // Assert
        assertThat(actual.isSuccess).isTrue()
    }

    @Test
    fun `GIVEN both kinds dismissed for two wallets WHEN clear THEN only that wallet loses both`() = runTest {
        // Arrange
        val dataStore = MockStateDataStore<Map<String, Map<String, Long>>>(default = emptyMap())
        val store = DefaultPolymarketFastTradingBannerStore(dataStore = dataStore)
        PolymarketFastTradingBannerKind.entries.forEach { kind ->
            store.dismiss(WALLET_A, kind, at = DISMISSED_AT)
            store.dismiss(WALLET_B, kind, at = DISMISSED_AT)
        }

        // Act
        store.clear(WALLET_A)

        // Assert
        PolymarketFastTradingBannerKind.entries.forEach { kind ->
            assertThat(store.dismissedAt(WALLET_A, kind).first()).isNull()
            assertThat(store.dismissedAt(WALLET_B, kind).first()).isEqualTo(DISMISSED_AT)
        }
        assertThat(dataStore.data.first().keys).containsExactly(WALLET_B.stringValue)
    }

    @Test
    fun `GIVEN a collector WHEN the banner is dismissed and cleared THEN it re-emits both times`() = runTest {
        // Arrange
        val store = createStore()

        // Act & Assert
        store.dismissedAt(WALLET_A, PolymarketFastTradingBannerKind.EXTEND).test {
            assertThat(awaitItem()).isNull()

            store.dismiss(WALLET_A, PolymarketFastTradingBannerKind.EXTEND, at = DISMISSED_AT)
            assertThat(awaitItem()).isEqualTo(DISMISSED_AT)

            store.clear(WALLET_A)
            assertThat(awaitItem()).isNull()
        }
    }

    private fun createStore() = DefaultPolymarketFastTradingBannerStore(
        dataStore = MockStateDataStore(default = emptyMap()),
    )

    private class FailingDataStore : DataStore<Map<String, Map<String, Long>>> {
        override val data: Flow<Map<String, Map<String, Long>>> = flow { throw IOException("disk unavailable") }

        override suspend fun updateData(
            transform: suspend (t: Map<String, Map<String, Long>>) -> Map<String, Map<String, Long>>,
        ) = throw IOException("disk unavailable")
    }

    private companion object {
        val WALLET_A = UserWalletId("011")
        val WALLET_B = UserWalletId("022")
        val DISMISSED_AT: Instant = Instant.ofEpochMilli(1_762_776_000_000)
        val EXTEND_DISMISSED_AT: Instant = Instant.ofEpochMilli(1_762_862_400_000)
    }
}