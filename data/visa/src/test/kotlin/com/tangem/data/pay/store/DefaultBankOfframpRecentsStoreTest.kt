package com.tangem.data.pay.store

import androidx.datastore.preferences.core.emptyPreferences
import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.core.datastore.MockStateDataStore
import com.tangem.test.core.datastore.createAppPreferencesStore
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class DefaultBankOfframpRecentsStoreTest {

    private var nowMillis = 100L
    private val prefs = createAppPreferencesStore(
        moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build(),
        dispatchers = TestingCoroutineDispatcherProvider(),
        preferencesDataStore = MockStateDataStore(default = emptyPreferences()),
    )
    private val store = DefaultBankOfframpRecentsStore(prefs = prefs, currentTimeMillis = { nowMillis })

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

    private companion object {
        val WALLET_A = UserWalletId("0123456789abcdef")
        val WALLET_B = UserWalletId("fedcba9876543210")
    }
}