package com.tangem.data.common.cache.etag

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.common.truth.Truth.assertThat
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultETagsStoreTest {

    private val appPreferencesStore: AppPreferencesStore = mockk()

    private val store = DefaultETagsStore(appPreferencesStore = appPreferencesStore)

    @BeforeEach
    fun resetMocks() {
        clearMocks(appPreferencesStore)
    }

    @Test
    fun `GIVEN wallets WHEN clear list THEN all keys of all wallets removed in a single edit`() = runTest {
        // Arrange
        val transform = slot<suspend AppPreferencesStore.(MutablePreferences) -> Unit>()
        coEvery { appPreferencesStore.editData(capture(transform)) } returns mutablePreferencesOf()

        val preferences = mutablePreferencesOf().apply {
            ETagsStore.Key.entries.forEach { key ->
                set(stringPreferencesKey("etag_${key}_${WALLET_A.stringValue}"), "a")
                set(stringPreferencesKey("etag_${key}_${WALLET_B.stringValue}"), "b")
            }
            set(stringPreferencesKey("unrelated"), "keep")
        }

        // Act
        store.clear(userWalletIds = listOf(WALLET_A, WALLET_B))
        transform.captured.invoke(appPreferencesStore, preferences)

        // Assert
        coVerify(exactly = 1) { appPreferencesStore.editData(any()) }
        assertThat(preferences.asMap().keys.map(Preferences.Key<*>::name)).containsExactly("unrelated")
    }

    @Test
    fun `GIVEN empty list WHEN clear list THEN nothing is edited`() = runTest {
        // Act
        store.clear(userWalletIds = emptyList())

        // Assert
        coVerify(exactly = 0) { appPreferencesStore.editData(any()) }
    }

    private companion object {
        val WALLET_A = UserWalletId("011")
        val WALLET_B = UserWalletId("022")
    }
}