package com.tangem.data.hotwallet

import androidx.datastore.preferences.core.emptyPreferences
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.store.datasource.preferences.StorePreferencesKeys
import com.tangem.test.core.datastore.MockStateDataStore
import com.tangem.test.core.datastore.createAppPreferencesStore
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class DefaultHotWalletRepositoryTest {

    private val dataStore = MockStateDataStore(default = emptyPreferences())
    private val repository = DefaultHotWalletRepository(
        appPreferencesStore = createAppPreferencesStore(
            dispatchers = TestingCoroutineDispatcherProvider(),
            preferencesDataStore = dataStore,
        ),
        storePreferencesKeys = StorePreferencesKeys(),
    )

    @Test
    fun `GIVEN nothing stored WHEN the threshold flag is read THEN it is not reached`() = runTest {
        // Act
        val actual = repository.isBalanceThresholdReached(WALLET_ID).first()

        // Assert
        assertThat(actual).isFalse()
    }

    @Test
    fun `GIVEN the threshold reached by one wallet WHEN the flags are read THEN only that wallet has it`() = runTest {
        // Act
        repository.setBalanceThresholdReached(WALLET_ID, isReached = true)

        // Assert
        assertThat(repository.isBalanceThresholdReached(WALLET_ID).first()).isTrue()
        assertThat(repository.isBalanceThresholdReached(OTHER_WALLET_ID).first()).isFalse()
    }

    @Test
    fun `GIVEN the threshold reached by two wallets WHEN one is reset THEN the other keeps the flag`() = runTest {
        // Arrange
        repository.setBalanceThresholdReached(WALLET_ID, isReached = true)
        repository.setBalanceThresholdReached(OTHER_WALLET_ID, isReached = true)

        // Act
        repository.setBalanceThresholdReached(WALLET_ID, isReached = false)

        // Assert
        assertThat(repository.isBalanceThresholdReached(WALLET_ID).first()).isFalse()
        assertThat(repository.isBalanceThresholdReached(OTHER_WALLET_ID).first()).isTrue()
    }

    @Test
    fun `GIVEN no flag stored WHEN a wallet is reset THEN nothing is written`() = runTest {
        // Act
        repository.setBalanceThresholdReached(WALLET_ID, isReached = false)

        // Assert
        assertThat(dataStore.data.first()).isEqualTo(emptyPreferences())
    }

    private companion object {
        val WALLET_ID = UserWalletId("011")
        val OTHER_WALLET_ID = UserWalletId("022")
    }
}