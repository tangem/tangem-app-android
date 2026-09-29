package com.tangem.data.pay.store

import androidx.datastore.preferences.core.emptyPreferences
import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.json.NetworkJsonFactory
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.core.datastore.MockStateDataStore
import com.tangem.spend.datasource.preferences.SpendPreferencesKeys
import com.tangem.test.core.datastore.createAppPreferencesStore
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class DefaultBankOfframpRecentsStoreTest {

    private var nowMillis = 100L
    private val prefs = createAppPreferencesStore(
        dispatchers = TestingCoroutineDispatcherProvider(),
        preferencesDataStore = MockStateDataStore(default = emptyPreferences()),
    )
    private val store = DefaultBankOfframpRecentsStore(
        prefs = prefs,
        spendPreferencesKeys = SpendPreferencesKeys(),
        json = NetworkJsonFactory.create(),
        currentTimeMillis = { nowMillis },
    )

    @Test
    fun `GIVEN empty store WHEN getUsage THEN empty map`() = runTest {
        // Act
        val actual = store.getUsage(WALLET_A)

        // Assert
        assertThat(actual).isEmpty()
    }

    @Test
    fun `GIVEN two uses of different accounts WHEN getUsage THEN both timestamps kept`() = runTest {
        // Arrange
        nowMillis = 100L
        store.markUsed(WALLET_A, "acc-1")
        nowMillis = 200L
        store.markUsed(WALLET_A, "acc-2")

        // Act
        val actual = store.getUsage(WALLET_A)

        // Assert
        assertThat(actual).containsExactly("acc-1", 100L, "acc-2", 200L)
    }

    @Test
    fun `GIVEN account used twice WHEN getUsage THEN latest timestamp wins`() = runTest {
        // Arrange
        nowMillis = 100L
        store.markUsed(WALLET_A, "acc-1")
        nowMillis = 300L
        store.markUsed(WALLET_A, "acc-1")

        // Act
        val actual = store.getUsage(WALLET_A)

        // Assert
        assertThat(actual).containsExactly("acc-1", 300L)
    }

    @Test
    fun `GIVEN usage on one wallet WHEN getUsage for another THEN empty`() = runTest {
        // Arrange
        store.markUsed(WALLET_A, "acc-1")

        // Act
        val actual = store.getUsage(WALLET_B)

        // Assert
        assertThat(actual).isEmpty()
    }

    @Test
    fun `GIVEN a journal stored by the previous app version WHEN getUsage THEN it is read back`() = runTest {
        // Arrange
        prefs.editData { preferences ->
            preferences[SpendPreferencesKeys().bankOfframpRecents(WALLET_A)] = """{"acc-1":{"usedAtMillis":100}}"""
        }

        // Act
        val actual = store.getUsage(WALLET_A)

        // Assert
        assertThat(actual).containsExactly("acc-1", 100L)
    }

    @Test
    fun `GIVEN a malformed journal blob WHEN getUsage THEN empty map`() = runTest {
        // Arrange
        prefs.editData { preferences ->
            preferences[SpendPreferencesKeys().bankOfframpRecents(WALLET_A)] = """["acc-1"]"""
        }

        // Act
        val actual = store.getUsage(WALLET_A)

        // Assert
        assertThat(actual).isEmpty()
    }

    @Test
    fun `GIVEN a malformed journal blob WHEN markUsed THEN the entry is written over it`() = runTest {
        // Arrange
        prefs.editData { preferences ->
            preferences[SpendPreferencesKeys().bankOfframpRecents(WALLET_A)] = """["acc-1"]"""
        }
        nowMillis = 400L

        // Act
        store.markUsed(WALLET_A, "acc-2")

        // Assert
        assertThat(store.getUsage(WALLET_A)).containsExactly("acc-2", 400L)
    }

    private companion object {
        val WALLET_A = UserWalletId("0123456789abcdef")
        val WALLET_B = UserWalletId("fedcba9876543210")
    }
}