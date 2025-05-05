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

    @Test
    fun `GIVEN API returns wallets WHEN getWalletsInfo THEN should return converted wallets and update cache if requested`() = runTest {
        // GIVEN
        val applicationId = "test_app_id"
        val wallet1Id = "1234567890abcdef"
        val wallet2Id = "fedcba0987654321"
        val walletResponses = listOf(
            WalletResponse(
                id = wallet1Id,
                notifyStatus = true,
            ),
            WalletResponse(
                id = wallet2Id,
                notifyStatus = false,
            ),
        )
        coEvery { tangemTechApi.getWallets(applicationId) } returns ApiResponse.Success(walletResponses)
        coEvery { preferencesDataStore.updateData(any()) } returns mockk<Preferences>()

        // WHEN
        val result = repository.getWalletsInfo(applicationId, updateCache = true)

        // THEN
        assertThat(result).hasSize(2)
        assertThat(result[0].walletId.stringValue).isEqualTo(wallet1Id)
        assertThat(result[0].isNotificationsEnabled).isTrue()
        assertThat(result[1].walletId.stringValue).isEqualTo(wallet2Id)
        assertThat(result[1].isNotificationsEnabled).isFalse()

        coVerify(exactly = 1) { tangemTechApi.getWallets(applicationId) }
        coVerify(exactly = 2) { preferencesDataStore.updateData(any()) }
    }

    @Test
    fun `GIVEN API returns wallets WHEN getWalletsInfo with updateCache false THEN should return converted wallets without updating cache`() = runTest {
        // GIVEN
        val applicationId = "test_app_id"
        val wallet1Id = "1234567890abcdef"
        val walletResponses = listOf(
            WalletResponse(
                id = wallet1Id,
                notifyStatus = true,
            ),
        )
        coEvery { tangemTechApi.getWallets(applicationId) } returns ApiResponse.Success(walletResponses)

        // WHEN
        val result = repository.getWalletsInfo(applicationId, updateCache = false)

        // THEN
        assertThat(result).hasSize(1)
        assertThat(result[0].walletId.stringValue).isEqualTo(wallet1Id)
        assertThat(result[0].isNotificationsEnabled).isTrue()

        coVerify(exactly = 1) { tangemTechApi.getWallets(applicationId) }
        coVerify(exactly = 0) { preferencesDataStore.updateData(any()) }
    }
}