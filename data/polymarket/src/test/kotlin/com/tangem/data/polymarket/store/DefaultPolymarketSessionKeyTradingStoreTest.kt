package com.tangem.data.polymarket.store

import com.google.common.truth.Truth.assertThat
import com.tangem.data.polymarket.entity.PolymarketSessionKeyReadinessDTO
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyReconciliation
import com.tangem.test.core.datastore.MockStateDataStore
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.Instant

internal class DefaultPolymarketSessionKeyTradingStoreTest {

    private val dataStore = MockStateDataStore<Map<String, PolymarketSessionKeyReadinessDTO>>(default = emptyMap())
    private val store = DefaultPolymarketSessionKeyTradingStore(readinessDataStore = dataStore)

    @Test
    fun `GIVEN nothing recorded WHEN isReadyOnExchange THEN false`() = runTest {
        // Act
        val actual = store.isReadyOnExchange(WALLET_A, KEY, VALID_UNTIL)

        // Assert
        assertThat(actual).isFalse()
    }

    @Test
    fun `GIVEN the key marked ready WHEN isReadyOnExchange THEN true for that key and validUntil only`() = runTest {
        // Act
        store.markReadyOnExchange(WALLET_A, KEY.uppercase().replace("0X", "0x"), VALID_UNTIL)

        // Assert
        assertThat(store.isReadyOnExchange(WALLET_A, KEY, VALID_UNTIL)).isTrue()
        assertThat(store.isReadyOnExchange(WALLET_A, KEY, VALID_UNTIL.plusSeconds(1))).isFalse()
        assertThat(store.isReadyOnExchange(WALLET_A, OTHER_KEY, VALID_UNTIL)).isFalse()
        assertThat(store.isReadyOnExchange(WALLET_B, KEY, VALID_UNTIL)).isFalse()
    }

    @Test
    fun `GIVEN a renewal WHEN marked ready again THEN only the new validUntil is ready`() = runTest {
        // Arrange
        store.markReadyOnExchange(WALLET_A, KEY, VALID_UNTIL)
        val renewed = VALID_UNTIL.plusSeconds(86_400)

        // Act
        store.markReadyOnExchange(WALLET_A, KEY, renewed)

        // Assert
        assertThat(store.isReadyOnExchange(WALLET_A, KEY, renewed)).isTrue()
        assertThat(store.isReadyOnExchange(WALLET_A, KEY, VALID_UNTIL)).isFalse()
    }

    @Test
    fun `GIVEN a refused key WHEN marked ready THEN it is no longer refused`() = runTest {
        // Arrange
        store.markRefused(WALLET_A, KEY)
        val refusedBefore = store.isRefused(WALLET_A, KEY)

        // Act
        store.markReadyOnExchange(WALLET_A, KEY, VALID_UNTIL)

        // Assert
        assertThat(refusedBefore).isTrue()
        assertThat(store.isRefused(WALLET_A, KEY)).isFalse()
    }

    @Test
    fun `GIVEN a checked reconciliation WHEN isMissingOnExchange THEN true for the missing keys of that wallet`() {
        // Act
        store.setReconciliation(WALLET_A, checked(missing = setOf(KEY)))

        // Assert
        assertThat(store.isMissingOnExchange(WALLET_A, KEY)).isTrue()
        assertThat(store.isMissingOnExchange(WALLET_A, OTHER_KEY)).isFalse()
        assertThat(store.isMissingOnExchange(WALLET_B, KEY)).isFalse()
    }

    @Test
    fun `GIVEN a missing key WHEN reconciliation is unavailable THEN the previous result is kept`() {
        // Arrange
        store.setReconciliation(WALLET_A, checked(missing = setOf(KEY)))

        // Act
        store.setReconciliation(WALLET_A, PolymarketSessionKeyReconciliation.Unavailable)

        // Assert
        assertThat(store.isMissingOnExchange(WALLET_A, KEY)).isTrue()
    }

    @Test
    fun `GIVEN a missing key WHEN a later reconciliation lists it THEN it is no longer missing`() {
        // Arrange
        store.setReconciliation(WALLET_A, checked(missing = setOf(KEY)))

        // Act
        store.setReconciliation(WALLET_A, checked(missing = emptySet()))

        // Assert
        assertThat(store.isMissingOnExchange(WALLET_A, KEY)).isFalse()
    }

    @Test
    fun `GIVEN a ready key WHEN refused THEN a restarted store does not see it ready`() = runTest {
        // Arrange
        store.markReadyOnExchange(WALLET_A, KEY, VALID_UNTIL)
        store.markReadyOnExchange(WALLET_B, KEY, VALID_UNTIL)

        // Act
        store.markRefused(WALLET_A, KEY.uppercase().replace("0X", "0x"))

        // Assert
        val restarted = DefaultPolymarketSessionKeyTradingStore(readinessDataStore = dataStore)
        assertThat(restarted.isReadyOnExchange(WALLET_A, KEY, VALID_UNTIL)).isFalse()
        assertThat(restarted.isReadyOnExchange(WALLET_B, KEY, VALID_UNTIL)).isTrue()
    }

    @Test
    fun `GIVEN another key is ready WHEN an older key is refused THEN the ready key keeps its readiness`() = runTest {
        // Arrange
        store.markReadyOnExchange(WALLET_A, KEY, VALID_UNTIL)

        // Act
        store.markRefused(WALLET_A, OTHER_KEY)

        // Assert
        assertThat(store.isReadyOnExchange(WALLET_A, KEY, VALID_UNTIL)).isTrue()
    }

    @Test
    fun `GIVEN a key refused on one wallet WHEN isRefused THEN the other wallet is unaffected`() = runTest {
        // Act
        store.markRefused(WALLET_A, KEY)

        // Assert
        assertThat(store.isRefused(WALLET_A, KEY)).isTrue()
        assertThat(store.isRefused(WALLET_B, KEY)).isFalse()
    }

    @Test
    fun `GIVEN a wallet with every record WHEN clear THEN nothing is left for it and the other wallet keeps its own`() =
        runTest {
            // Arrange
            store.markReadyOnExchange(WALLET_A, KEY, VALID_UNTIL)
            store.markReadyOnExchange(WALLET_B, OTHER_KEY, VALID_UNTIL)
            store.markRefused(WALLET_A, KEY)
            store.setReconciliation(WALLET_A, checked(missing = setOf(KEY)))

            // Act
            store.clear(WALLET_A)

            // Assert
            assertThat(store.isReadyOnExchange(WALLET_A, KEY, VALID_UNTIL)).isFalse()
            assertThat(store.isRefused(WALLET_A, KEY)).isFalse()
            assertThat(store.isMissingOnExchange(WALLET_A, KEY)).isFalse()
            assertThat(store.isReadyOnExchange(WALLET_B, OTHER_KEY, VALID_UNTIL)).isTrue()
        }

    private fun checked(missing: Set<String>) = PolymarketSessionKeyReconciliation.Checked(
        foreignSigners = emptyList(),
        missingOnExchange = missing,
    )

    private companion object {
        const val KEY = "0x5e55000000000000000000000000000000000001"
        const val OTHER_KEY = "0x01d0000000000000000000000000000000000009"

        val WALLET_A = UserWalletId("011")
        val WALLET_B = UserWalletId("022")
        val VALID_UNTIL: Instant = Instant.ofEpochSecond(1_804_982_400)
    }
}