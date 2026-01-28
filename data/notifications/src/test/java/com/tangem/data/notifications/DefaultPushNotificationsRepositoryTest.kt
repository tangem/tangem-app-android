package com.tangem.data.notifications

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.tangem.data.notifications.converters.NotificationsEligibleNetworkConverter
import arrow.core.Either
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.CryptoNetworkResponse
import com.tangem.datasource.api.tangemTech.models.NotificationApplicationCreateBody
import com.tangem.datasource.api.tangemTech.models.NotificationApplicationIdResponse
import com.tangem.datasource.local.appsflyer.AppsFlyerStore
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.domain.notifications.models.ApplicationId
import com.tangem.domain.notifications.models.NotificationsError
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import com.tangem.utils.info.AppInfoProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DefaultPushNotificationsRepositoryTest {
    private val tangemTechApi: TangemTechApi = mockk()
    private val appInfoProvider: AppInfoProvider = mockk()
    private val preferencesDataStore: DataStore<Preferences> = mockk()
    private val appPreferencesStore = AppPreferencesStore(
        moshi = Moshi.Builder().build(),
        dispatchers = TestingCoroutineDispatcherProvider(),
        preferencesDataStore = preferencesDataStore,
    )
    private val appsFlyerStore: AppsFlyerStore = mockk()
    private val repository = DefaultPushNotificationsRepository(
        tangemTechApi = tangemTechApi,
        appInfoProvider = appInfoProvider,
        appPreferencesStore = appPreferencesStore,
        appsFlyerStore = appsFlyerStore,
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
        coEvery { appsFlyerStore.getUID() } returns "UID"
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
                    appsflyerId = "UID",
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
    fun `GIVEN stored application id WHEN clearApplicationId THEN removes it from preferences`() = runTest {
        // GIVEN
        val preferences = mockk<Preferences>(relaxed = true)
        coEvery { preferencesDataStore.updateData(any()) } returns preferences

        // WHEN
        repository.clearApplicationId()

        // THEN
        coVerify { preferencesDataStore.updateData(any()) }
    }

    @Test
    fun `GIVEN application id and push token WHEN sendPushToken succeeds THEN returns Right Unit`() = runTest {
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
        val result = repository.sendPushToken(appId, pushToken)

        // THEN
        assertThat(result).isEqualTo(Either.Right(Unit))
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
    fun `GIVEN application id not found WHEN sendPushToken THEN returns Left ApplicationIdNotFound`() = runTest {
        // GIVEN
        val appId = ApplicationId("test-app-id")
        val pushToken = "test-push-token"
        coEvery { appInfoProvider.osVersion } returns "11"
        coEvery { appInfoProvider.language } returns "en"
        coEvery { appInfoProvider.appVersion } returns "5.21.1"
        coEvery { appInfoProvider.timezone } returns "UTC"
        coEvery {
            tangemTechApi.updatePushTokenForApplicationId(any(), any())
        } returns (ApiResponse.Error(
            ApiResponseError.HttpException(
                code = ApiResponseError.HttpException.Code.NOT_FOUND,
                message = "Not found",
                errorBody = null,
            ),
        ) as ApiResponse<Unit>)

        // WHEN
        val result = repository.sendPushToken(appId, pushToken)

        // THEN
        assertThat(result).isEqualTo(Either.Left(NotificationsError.ApplicationIdNotFound))
    }

    @Test
    fun `GIVEN api error WHEN sendPushToken THEN returns Left DataError`() = runTest {
        // GIVEN
        val appId = ApplicationId("test-app-id")
        val pushToken = "test-push-token"
        val errorMessage = "Server error"
        coEvery { appInfoProvider.osVersion } returns "11"
        coEvery { appInfoProvider.language } returns "en"
        coEvery { appInfoProvider.appVersion } returns "5.21.1"
        coEvery { appInfoProvider.timezone } returns "UTC"
        coEvery {
            tangemTechApi.updatePushTokenForApplicationId(any(), any())
        } returns (ApiResponse.Error(
            ApiResponseError.HttpException(
                code = ApiResponseError.HttpException.Code.BAD_REQUEST,
                message = errorMessage,
                errorBody = null,
            ),
        ) as ApiResponse<Unit>)

        // WHEN
        val result = repository.sendPushToken(appId, pushToken)

        // THEN
        assertThat(result).isEqualTo(Either.Left(NotificationsError.DataError(errorMessage)))
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