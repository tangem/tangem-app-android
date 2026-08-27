package com.tangem.data.polymarket.store

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.core.datastore.MockStateDataStore
import app.cash.turbine.test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class DefaultPolymarketOnboardedStoreTest {

    @Test
    fun `GIVEN nothing recorded WHEN isOnboardedSync THEN false`() = runTest {
        // Arrange
        val store = createStore()

        // Act
        val actual = store.isOnboardedSync(WALLET_A)

        // Assert
        assertThat(actual).isFalse()
    }

    @Test
    fun `GIVEN a wallet is marked WHEN isOnboardedSync THEN true for it alone`() = runTest {
        // Arrange
        val store = createStore()

        // Act
        store.markOnboarded(WALLET_A)

        // Assert
        assertThat(store.isOnboardedSync(WALLET_A)).isTrue()
        assertThat(store.isOnboardedSync(WALLET_B)).isFalse()
    }

    @Test
    fun `GIVEN a marked wallet WHEN marked again THEN it stays recorded once`() = runTest {
        // Arrange
        val dataStore = MockStateDataStore<Set<String>>(default = emptySet())
        val store = DefaultPolymarketOnboardedStore(dataStore = dataStore)
        store.markOnboarded(WALLET_A)

        // Act
        store.markOnboarded(WALLET_A)

        // Assert
        assertThat(dataStore.data.first()).containsExactly(WALLET_A.stringValue)
    }

    @Test
    fun `GIVEN a marked wallet WHEN cleared THEN the other wallet is untouched`() = runTest {
        // Arrange
        val store = createStore()
        store.markOnboarded(WALLET_A)
        store.markOnboarded(WALLET_B)

        // Act
        store.clear(WALLET_A)

        // Assert
        assertThat(store.isOnboardedSync(WALLET_A)).isFalse()
        assertThat(store.isOnboardedSync(WALLET_B)).isTrue()
    }

    @Test
    fun `GIVEN nothing recorded WHEN isOnboarded THEN emits false`() = runTest {
        // Arrange
        val store = createStore()

        // Act
        val actual = store.isOnboarded(WALLET_A).first()

        // Assert
        assertThat(actual).isFalse()
    }

    @Test
    fun `GIVEN a collector WHEN the wallet is marked and cleared THEN it re-emits both times`() = runTest {
        // Arrange
        val store = createStore()

        // Act & Assert
        store.isOnboarded(WALLET_A).test {
            assertThat(awaitItem()).isFalse()

            store.markOnboarded(WALLET_A)
            assertThat(awaitItem()).isTrue()

            store.clear(WALLET_A)
            assertThat(awaitItem()).isFalse()
        }
    }

    @Test
    fun `GIVEN a collector WHEN another wallet is marked THEN it does not re-emit`() = runTest {
        // Arrange
        val store = createStore()

        // Act & Assert
        store.isOnboarded(WALLET_A).test {
            assertThat(awaitItem()).isFalse()

            store.markOnboarded(WALLET_B)

            expectNoEvents()
        }
    }

    private fun createStore() = DefaultPolymarketOnboardedStore(
        dataStore = MockStateDataStore(default = emptySet()),
    )

    private companion object {
        val WALLET_A = UserWalletId("011")
        val WALLET_B = UserWalletId("022")
    }
}