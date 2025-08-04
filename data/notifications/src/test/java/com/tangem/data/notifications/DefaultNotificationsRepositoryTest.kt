package com.tangem.data.notifications

import com.google.common.truth.Truth.assertThat
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import androidx.datastore.preferences.core.Preferences
import com.squareup.moshi.Moshi
import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.flowOf

class DefaultNotificationsRepositoryTest {
    private val preferencesDataStore: DataStore<Preferences> = mockk()
    private val appPreferencesStore = AppPreferencesStore(
        moshi = Moshi.Builder().build(),
        dispatchers = TestingCoroutineDispatcherProvider(),
        preferencesDataStore = preferencesDataStore,
    )
    private val repository = DefaultNotificationsRepository(
        appPreferencesStore = appPreferencesStore,
    )

    @Test
    fun `GIVEN shouldShowNotification returns true WHEN called THEN returns true`() = runTest {
        // GIVEN
        val key = "test-key"
        val preferences = mockk<Preferences>(relaxed = true)
        every { preferences[PreferencesKeys.getShouldShowNotificationKey(key)] } returns true
        coEvery { preferencesDataStore.data } returns flowOf(preferences)

        // WHEN
        val result = repository.shouldShowNotification(key)

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `GIVEN shouldShowNotification returns false WHEN called THEN returns false`() = runTest {
        // GIVEN
        val key = "test-key"
        val preferences = mockk<Preferences>(relaxed = true)
        every { preferences[PreferencesKeys.getShouldShowNotificationKey(key)] } returns false
        coEvery { preferencesDataStore.data } returns flowOf(preferences)

        // WHEN
        val result = repository.shouldShowNotification(key)

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `GIVEN setShouldShowNotifications WHEN called THEN stores value in preferences`() = runTest {
        // GIVEN
        val key = "test-key"
        val value = false
        coEvery { preferencesDataStore.updateData(any()) } returns mockk(relaxed = true)

        // WHEN
        repository.setShouldShowNotifications(key, value)

        // THEN
        coVerify { preferencesDataStore.updateData(any()) }
    }

    @Test
    fun `GIVEN incrementTronTokenFeeNotificationShowCounter WHEN called THEN increments counter`() = runTest {
        // GIVEN
        coEvery { preferencesDataStore.updateData(any()) } returns mockk(relaxed = true)

        // WHEN
        repository.incrementTronTokenFeeNotificationShowCounter()

        // THEN
        coVerify { preferencesDataStore.updateData(any()) }
    }

    @Test
    fun `GIVEN getTronTokenFeeNotificationShowCounter WHEN called THEN returns counter value`() = runTest {
        // GIVEN
        val expectedCount = 5
        val preferences = mockk<Preferences>(relaxed = true)
        every { preferences[PreferencesKeys.TRON_NETWORK_FEE_NOTIFICATION_SHOW_COUNT_KEY] } returns expectedCount
        coEvery { preferencesDataStore.data } returns flowOf(preferences)

        // WHEN
        val result = repository.getTronTokenFeeNotificationShowCounter()

        // THEN
        assertThat(result).isEqualTo(expectedCount)
    }
}