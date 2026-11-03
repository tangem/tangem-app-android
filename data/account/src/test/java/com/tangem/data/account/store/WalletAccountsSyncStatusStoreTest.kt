package com.tangem.data.account.store

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.preferencesOf
import com.google.common.truth.Truth.assertThat
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class WalletAccountsSyncStatusStoreTest {

    private val appPreferencesStore: AppPreferencesStore = mockk()

    private val syncState = WalletAccountsSyncStatusStore(appPreferencesStore = appPreferencesStore)

    @BeforeEach
    fun resetMocks() {
        clearMocks(appPreferencesStore)
    }

    @Test
    fun `GIVEN no local-only marker WHEN isSyncedWithRemote THEN returns true`() = runTest {
        // Arrange
        every { appPreferencesStore.data } returns flowOf(emptyPreferences())

        // Act
        val actual = syncState.isSyncedWithRemote(WALLET_A)

        // Assert
        assertThat(actual).isTrue()
    }

    @Test
    fun `GIVEN local-only marker of the wallet WHEN isSyncedWithRemote THEN returns false`() = runTest {
        // Arrange
        every { appPreferencesStore.data } returns flowOf(preferencesOf(localOnlyKey(WALLET_A) to true))

        // Act
        val actual = syncState.isSyncedWithRemote(WALLET_A)

        // Assert
        assertThat(actual).isFalse()
    }

    @Test
    fun `GIVEN local-only marker of another wallet WHEN isSyncedWithRemote THEN returns true`() = runTest {
        // Arrange
        every { appPreferencesStore.data } returns flowOf(preferencesOf(localOnlyKey(WALLET_B) to true))

        // Act
        val actual = syncState.isSyncedWithRemote(WALLET_A)

        // Assert
        assertThat(actual).isTrue()
    }

    @Test
    fun `GIVEN local-only wallet WHEN markSyncedWithRemote THEN only its marker is removed`() = runTest {
        // Arrange
        val transform = slot<suspend AppPreferencesStore.(MutablePreferences) -> Unit>()
        coEvery { appPreferencesStore.editData(capture(transform)) } returns emptyPreferences()
        val preferences = mutablePreferencesOf(localOnlyKey(WALLET_A) to true, localOnlyKey(WALLET_B) to true)

        // Act
        syncState.markSyncedWithRemote(WALLET_A)
        transform.captured.invoke(appPreferencesStore, preferences)

        // Assert
        assertThat(preferences.asMap()).containsExactly(localOnlyKey(WALLET_B), true)
    }

    @Test
    fun `GIVEN wallets WHEN clear THEN their markers are removed in a single edit`() = runTest {
        // Arrange
        val transform = slot<suspend AppPreferencesStore.(MutablePreferences) -> Unit>()
        coEvery { appPreferencesStore.editData(capture(transform)) } returns emptyPreferences()
        val unrelatedKey = booleanPreferencesKey("unrelated")
        val preferences = mutablePreferencesOf(
            localOnlyKey(WALLET_A) to true,
            localOnlyKey(WALLET_B) to true,
            unrelatedKey to true,
        )

        // Act
        syncState.clear(listOf(WALLET_A, WALLET_B))
        transform.captured.invoke(appPreferencesStore, preferences)

        // Assert
        coVerify(exactly = 1) { appPreferencesStore.editData(any()) }
        assertThat(preferences.asMap()).containsExactly(unrelatedKey, true)
    }

    private fun localOnlyKey(userWalletId: UserWalletId): Preferences.Key<Boolean> {
        return booleanPreferencesKey("wallet_accounts_local_only_${userWalletId.stringValue}")
    }

    private companion object {
        val WALLET_A = UserWalletId("011")
        val WALLET_B = UserWalletId("022")
    }
}