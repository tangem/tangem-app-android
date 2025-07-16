package com.tangem.data.notifications

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.utils.info.AppInfoProvider
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.tangem.data.notifications.converters.NotificationsEligibleNetworkConverter
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.tangemTech.models.*
import com.tangem.domain.notifications.models.ApplicationId
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DefaultNotificationsRepositoryTest {
    private val tangemTechApi: TangemTechApi = mockk()
    private val appInfoProvider: AppInfoProvider = mockk()
    private val preferencesDataStore: DataStore<Preferences> = mockk()
    private val appPreferencesStore = AppPreferencesStore(
        moshi = Moshi.Builder().build(),
        dispatchers = TestingCoroutineDispatcherProvider(),
        preferencesDataStore = preferencesDataStore,
    )
    private val repository = DefaultNotificationsRepository(
        tangemTechApi = tangemTechApi,
        appInfoProvider = appInfoProvider,
        appPreferencesStore = appPreferencesStore,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @Test
    fun `GIVEN valid push token WHEN createApplicationId THEN returns application id`() = runTest {
        // GIVEN
        val pushToken = "test-push-token"
        val expectedAppId = ApplicationId("test-app-id")
        val expectedAppIdResponse = NotificationApplicationIdResponse(
            appId = expectedAppId.value,
        )
        coEvery { appInfoProvider.platform } returns "android"
        coEvery { appInfoProvider.device } returns "test-device"
        coEvery { appInfoProvider.osVersion } returns "11"
        coEvery { appInfoProvider.language } returns "en"
        coEvery { appInfoProvider.appVersion } returns "5.21.1"
        coEvery { appInfoProvider.timezone } returns "UTC"
        coEvery { tangemTechApi.createApplicationId(any()) } returns ApiResponse.Success(
            expectedAppIdResponse,
        )

        // WHEN
        val result = repository.createApplicationId(pushToken)

        // THEN
        assertThat(result).isEqualTo(expectedAppId)
        coVerify {
            tangemTechApi.createApplicationId(
                NotificationApplicationCreateBody(
                    platform = "android",
                    device = "test-device",
                    systemVersion = "11",
                    language = "en",
                    timezone = "UTC",
                    version = "5.21.1",
                    pushToken = pushToken,
                ),
            )
        }
    }

    @Test
    fun `GIVEN application id WHEN saveApplicationId THEN stores it in preferences`() = runTest {
        // GIVEN
        val appId = ApplicationId("test-app-id")
        val preferences = mockk<Preferences>(relaxed = true)
        coEvery { preferencesDataStore.updateData(any()) } returns preferences

        // WHEN
        repository.saveApplicationId(appId)

        // THEN
        coVerify { preferencesDataStore.updateData(any()) }
    }

    @Test
    fun `GIVEN stored application id WHEN getApplicationId THEN returns it`() = runTest {
        // GIVEN
        val expectedAppId = ApplicationId("test-app-id")
        val preferences = mockk<Preferences>(relaxed = true)
        val key = stringPreferencesKey(PreferencesKeys.NOTIFICATIONS_APPLICATION_ID_KEY.name)
        every { preferences[key] } returns expectedAppId.value
        coEvery { preferencesDataStore.data } returns flowOf(preferences)

        // WHEN
        val result = repository.getApplicationId()

        // THEN
        assertThat(result).isEqualTo(expectedAppId)
    }

    @Test
    fun `GIVEN application id and push token WHEN sendPushToken THEN updates push token`() = runTest {
        // GIVEN
        val appId = ApplicationId("test-app-id")
        val pushToken = "test-push-token"
        coEvery { appInfoProvider.device } returns "test-device"
        coEvery { appInfoProvider.osVersion } returns "11"
        coEvery { appInfoProvider.language } returns "en"
        coEvery { appInfoProvider.appVersion } returns "5.21.1"
        coEvery { appInfoProvider.timezone } returns "UTC"
        coEvery {
            tangemTechApi.updatePushTokenForApplicationId(
                appId.value,
                NotificationApplicationCreateBody(
                    pushToken = pushToken,
                    systemVersion = "11",
                    language = "en",
                    timezone = "UTC",
                    version = "5.21.1",
                ),
            )
        } returns ApiResponse.Success(Unit)

        // WHEN
        repository.sendPushToken(appId, pushToken)

        // THEN
        coVerify {
            tangemTechApi.updatePushTokenForApplicationId(
                appId.value,
                NotificationApplicationCreateBody(
                    pushToken = pushToken,
                    systemVersion = "11",
                    language = "en",
                    timezone = "UTC",
                    version = "5.21.1",
                ),
            )
        }
    }

    @Test
    fun `GIVEN eligible networks WHEN getEligibleNetworks THEN returns converted networks`() = runTest {
        // GIVEN
        val expectedNetworks = listOf(
            CryptoNetworkResponse(
                id = 1,
                name = "Ethereum",
                networkId = "ethereum",
            ),
            CryptoNetworkResponse(
                id = 2,
                name = "Bitcoin",
                networkId = "bitcoin",
            ),
        )
        coEvery { tangemTechApi.getEligibleNetworksForPushNotifications() } returns ApiResponse.Success(
            expectedNetworks,
        )

        // WHEN
        val result = repository.getEligibleNetworks()

        // THEN
        assertThat(result).hasSize(2)
        assertThat(result[0]).isEqualTo(NotificationsEligibleNetworkConverter.convert(expectedNetworks[0]))
        assertThat(result[1]).isEqualTo(NotificationsEligibleNetworkConverter.convert(expectedNetworks[1]))
        coVerify { tangemTechApi.getEligibleNetworksForPushNotifications() }
    }
}