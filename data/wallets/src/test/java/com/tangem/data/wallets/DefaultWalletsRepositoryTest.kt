package com.tangem.data.wallets

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.squareup.moshi.Moshi
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.WalletBody
import com.tangem.datasource.api.tangemTech.models.WalletResponse
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class DefaultWalletsRepositoryTest {
    private lateinit var repository: DefaultWalletsRepository
    private val preferencesDataStore = mockk<DataStore<Preferences>>(relaxed = true)
    private val appPreferenceStore = AppPreferencesStore(
        moshi = Moshi.Builder().build(),
        dispatchers = TestingCoroutineDispatcherProvider(),
        preferencesDataStore = preferencesDataStore,
    )
    private lateinit var tangemTechApi: TangemTechApi
    private lateinit var dispatchers: CoroutineDispatcherProvider

    private val testWalletId = UserWalletId("1234567890abcdef")
    private val testWalletResponse = WalletResponse(
        id = testWalletId.stringValue,
        notifyStatus = true,
    )

    @Before
    fun setup() {
        tangemTechApi = mockk()
        dispatchers = TestingCoroutineDispatcherProvider()
        repository = DefaultWalletsRepository(
            appPreferencesStore = appPreferenceStore,
            tangemTechApi = tangemTechApi,
            userWalletsStore = mockk(),
            seedPhraseNotificationVisibilityStore = mockk(),
            dispatchers = dispatchers,
        )
    }

    @Test
    fun `GIVEN force is true WHEN isNotificationsEnabled THEN should fetch from API and update local storage`() =
        runTest {
            // GIVEN
            coEvery { tangemTechApi.getWalletById(testWalletId.stringValue) } returns ApiResponse.Success(
                testWalletResponse,
            )
            coEvery { preferencesDataStore.updateData(any()) } returns mockk<Preferences>()

            // WHEN
            val result = repository.isNotificationsEnabled(testWalletId, force = true)

            // THEN
            assertThat(result).isTrue()
            coVerify(exactly = 1) {
                tangemTechApi.getWalletById(testWalletId.stringValue)
            }
            coVerify(exactly = 0) {
                tangemTechApi.setNotificationsEnabled(any(), any())
            }
            coVerify(exactly = 1) { preferencesDataStore.updateData(any()) }
        }

    @Test
    fun `GIVEN local storage has value WHEN isNotificationsEnabled THEN should return local value`() = runTest {
        // GIVEN
        val expectedPreferences = """{"${testWalletId.stringValue}":true}"""
        val preferences = mockk<Preferences>()
        coEvery { preferencesDataStore.data } returns flowOf(preferences)
        coEvery { preferences[PreferencesKeys.NOTIFICATIONS_ENABLED_STATES_KEY] } returns expectedPreferences
        coEvery { tangemTechApi.getWalletById(any()) } returns ApiResponse.Success(testWalletResponse)

        // WHEN
        val result = repository.isNotificationsEnabled(testWalletId, force = false)

        // THEN
        assertThat(result).isTrue()
        coVerify(inverse = true) {
            tangemTechApi.getWalletById(any())
            tangemTechApi.setNotificationsEnabled(any(), any())
        }
    }

    @Test
    fun `GIVEN local storage is empty WHEN isNotificationsEnabled THEN should fetch from API and update local storage`() = runTest {
        // GIVEN
        val preferences = mockk<Preferences>()
        coEvery { preferencesDataStore.data } returns flowOf(preferences)
        coEvery { preferences[PreferencesKeys.NOTIFICATIONS_ENABLED_STATES_KEY] } returns "{}"
        coEvery { tangemTechApi.getWalletById(testWalletId.stringValue) } returns ApiResponse.Success(
            testWalletResponse,
        )
        coEvery { preferencesDataStore.updateData(any()) } returns mockk<Preferences>()

        // WHEN
        val result = repository.isNotificationsEnabled(testWalletId, force = false)

        // THEN
        assertThat(result).isTrue()
        coVerify(exactly = 1) {
            tangemTechApi.getWalletById(testWalletId.stringValue)
        }
        coVerify(exactly = 0) {
            tangemTechApi.setNotificationsEnabled(any(), any())
        }
        coVerify(exactly = 1) { preferencesDataStore.updateData(any()) }
    }

    @Test
    fun `GIVEN enabled status WHEN setNotificationsEnabled THEN should update API and local storage`() = runTest {
        // GIVEN
        coEvery {
            tangemTechApi.setNotificationsEnabled(
                eq(testWalletId.stringValue),
                eq(WalletBody(notifyStatus = true)),
            )
        } returns ApiResponse.Success(Unit)
        coEvery { preferencesDataStore.updateData(any()) } returns mockk<Preferences>()

        // WHEN
        repository.setNotificationsEnabled(testWalletId, isEnabled = true)

        // THEN
        coVerify(exactly = 1) {
            tangemTechApi.setNotificationsEnabled(
                eq(testWalletId.stringValue),
                eq(WalletBody(notifyStatus = true)),
            )
        }
        coVerify(exactly = 1) { preferencesDataStore.updateData(any()) }
    }

    @Test
    fun `GIVEN disabled status WHEN setNotificationsEnabled THEN should update API and local storage`() = runTest {
        // GIVEN
        coEvery {
            tangemTechApi.setNotificationsEnabled(
                eq(testWalletId.stringValue),
                eq(WalletBody(notifyStatus = false)),
            )
        } returns ApiResponse.Success(Unit)
        coEvery { preferencesDataStore.updateData(any()) } returns mockk<Preferences>()

        // WHEN
        repository.setNotificationsEnabled(testWalletId, isEnabled = false)

        // THEN
        coVerifyOrder {
            tangemTechApi.setNotificationsEnabled(
                eq(testWalletId.stringValue),
                eq(WalletBody(notifyStatus = false)),
            )
            preferencesDataStore.updateData(any())
        }
        coVerify(exactly = 1) {
            tangemTechApi.setNotificationsEnabled(
                eq(testWalletId.stringValue),
                eq(WalletBody(notifyStatus = false)),
            )
        }
        coVerify(exactly = 1) { preferencesDataStore.updateData(any()) }
    }
}