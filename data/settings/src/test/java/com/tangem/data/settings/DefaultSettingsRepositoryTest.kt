package com.tangem.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.GeoResponse
import com.tangem.datasource.local.logs.AppLogsStore
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.store.datasource.preferences.StorePreferencesKeys
import com.tangem.datasource.local.preferences.utils.getSyncOrNull
import com.tangem.datasource.local.preferences.utils.store
import com.tangem.domain.settings.SettingsFeatureToggles
import com.tangem.domain.settings.usercountry.models.UserCountry
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.datastore.MockStateDataStore
import com.tangem.test.core.datastore.createAppPreferencesStore
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.io.IOException
import java.util.Locale

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultSettingsRepositoryTest {

    private val dataStore = MockStateDataStore(default = emptyPreferences())
    private val appPreferencesStore = createAppPreferencesStore(
        moshi = Moshi.Builder().build(),
        dispatchers = TestingCoroutineDispatcherProvider(),
        preferencesDataStore = dataStore,
    )
    private val appLogsStore: AppLogsStore = mockk(relaxed = true)
    private val tangemTechApi: TangemTechApi = mockk()
    private val settingsFeatureToggles: SettingsFeatureToggles = mockk()

    private val defaultLocale: Locale = Locale.getDefault()

    @BeforeEach
    fun setup() {
        clearMocks(tangemTechApi, settingsFeatureToggles)
        every { settingsFeatureToggles.isGeoCountryCacheFallbackEnabled } returns true
        runBlocking { dataStore.updateData { emptyPreferences() } }
    }

    @AfterEach
    fun tearDown() {
        Locale.setDefault(defaultLocale)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun fetchUserCountryCode(model: FetchModel) = runTest {
        // Arrange
        Locale.setDefault(model.locale)
        val repository = createRepository()
        model.cachedCode?.let { appPreferencesStore.store(key = GEO_COUNTRY_KEY, value = it) }
        val geoResponse = model.geoResponse
        if (geoResponse == null) {
            coEvery { tangemTechApi.getUserCountryCode() } throws IOException("timeout")
        } else {
            coEvery { tangemTechApi.getUserCountryCode() } returns geoResponse
        }

        // Act
        repository.fetchUserCountryCode()

        // Assert
        assertThat(repository.getUserCountryCode().value).isEqualTo(model.expectedCountry)
        assertThat(appPreferencesStore.getSyncOrNull(GEO_COUNTRY_KEY)).isEqualTo(model.expectedCachedCode)
        coVerify(exactly = 1) { tangemTechApi.getUserCountryCode() }
    }

    @Test
    fun `GIVEN toggle disabled and GB locale WHEN fetchUserCountryCode THEN geo is not requested`() = runTest {
        // Arrange
        every { settingsFeatureToggles.isGeoCountryCacheFallbackEnabled } returns false
        Locale.setDefault(Locale.UK)
        val repository = createRepository()

        // Act
        repository.fetchUserCountryCode()

        // Assert
        assertThat(repository.getUserCountryCode().value).isEqualTo(UserCountry.Other(code = "GB"))
        assertThat(appPreferencesStore.getSyncOrNull(GEO_COUNTRY_KEY)).isNull()
        coVerify(exactly = 0) { tangemTechApi.getUserCountryCode() }
    }

    @Test
    fun `GIVEN toggle disabled and geo returned ru WHEN fetchUserCountryCode THEN maps to Russia`() = runTest {
        // Arrange
        every { settingsFeatureToggles.isGeoCountryCacheFallbackEnabled } returns false
        Locale.setDefault(Locale.GERMANY)
        val repository = createRepository()
        coEvery { tangemTechApi.getUserCountryCode() } returns GeoResponse(code = "RU")

        // Act
        repository.fetchUserCountryCode()

        // Assert
        assertThat(repository.getUserCountryCode().value).isEqualTo(UserCountry.Russia)
        assertThat(appPreferencesStore.getSyncOrNull(GEO_COUNTRY_KEY)).isNull()
    }

    @Test
    fun `GIVEN toggle disabled and geo failed WHEN fetchUserCountryCode THEN cache is ignored`() = runTest {
        // Arrange
        every { settingsFeatureToggles.isGeoCountryCacheFallbackEnabled } returns false
        Locale.setDefault(Locale.GERMANY)
        val repository = createRepository()
        appPreferencesStore.store(key = GEO_COUNTRY_KEY, value = "fr")
        coEvery { tangemTechApi.getUserCountryCode() } throws IOException("timeout")

        // Act
        repository.fetchUserCountryCode()

        // Assert
        assertThat(repository.getUserCountryCode().value).isEqualTo(UserCountry.Other(code = "de"))
    }

    @Test
    fun `GIVEN country not fetched yet WHEN getUserCountryCodeSync THEN returns null`() = runTest {
        assertThat(createRepository().getUserCountryCodeSync()).isNull()
    }

    @Test
    fun `GIVEN country fetched WHEN getUserCountryCodeSync THEN returns it`() = runTest {
        // Arrange
        Locale.setDefault(Locale.GERMANY)
        val repository = createRepository()
        coEvery { tangemTechApi.getUserCountryCode() } returns GeoResponse(code = "fr")

        // Act
        repository.fetchUserCountryCode()

        // Assert
        assertThat(repository.getUserCountryCodeSync()).isEqualTo(UserCountry.Other(code = "fr"))
    }

    @Test
    fun `GIVEN country cached by previous launch WHEN geo fails THEN cached country is reused`() = runTest {
        // Arrange
        Locale.setDefault(Locale.UK)
        coEvery { tangemTechApi.getUserCountryCode() } returns GeoResponse(code = "fr")
        createRepository().fetchUserCountryCode()

        val nextLaunch = createRepository()
        coEvery { tangemTechApi.getUserCountryCode() } throws IOException("timeout")

        // Act
        nextLaunch.fetchUserCountryCode()

        // Assert
        assertThat(nextLaunch.getUserCountryCode().value).isEqualTo(UserCountry.Other(code = "fr"))
    }

    @Test
    fun `GIVEN cache write fails WHEN fetchUserCountryCode THEN country is still published`() = runTest {
        // Arrange
        Locale.setDefault(Locale.GERMANY)
        val failingStore = createAppPreferencesStore(
            moshi = Moshi.Builder().build(),
            dispatchers = TestingCoroutineDispatcherProvider(),
            preferencesDataStore = FailingWriteDataStore(),
        )
        val repository = createRepository(preferencesStore = failingStore)
        coEvery { tangemTechApi.getUserCountryCode() } returns GeoResponse(code = "fr")

        // Act
        repository.fetchUserCountryCode()

        // Assert
        assertThat(repository.getUserCountryCode().value).isEqualTo(UserCountry.Other(code = "fr"))
    }

    private class FailingWriteDataStore : DataStore<Preferences> {

        override val data: Flow<Preferences> = flowOf(emptyPreferences())

        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            error("preferences write failed")
        }
    }

    private val storePreferencesKeys = StorePreferencesKeys()

    private fun createRepository(preferencesStore: AppPreferencesStore = appPreferencesStore) = DefaultSettingsRepository(
        appPreferencesStore = preferencesStore,
        storePreferencesKeys = storePreferencesKeys,
        appLogsStore = appLogsStore,
        tangemTechApi = tangemTechApi,
        settingsFeatureToggles = settingsFeatureToggles,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    internal data class FetchModel(
        val locale: Locale,
        val geoResponse: GeoResponse?,
        val cachedCode: String?,
        val expectedCountry: UserCountry,
        val expectedCachedCode: String?,
    )

    private fun provideTestModels() = listOf(
        FetchModel(
            locale = Locale.GERMANY,
            geoResponse = GeoResponse(code = "FR"),
            cachedCode = null,
            expectedCountry = UserCountry.Other(code = "fr"),
            expectedCachedCode = "fr",
        ),
        FetchModel(
            locale = Locale.GERMANY,
            geoResponse = GeoResponse(code = "ru"),
            cachedCode = null,
            expectedCountry = UserCountry.Russia,
            expectedCachedCode = "ru",
        ),
        FetchModel(
            locale = Locale.UK,
            geoResponse = GeoResponse(code = "de"),
            cachedCode = "gb",
            expectedCountry = UserCountry.Other(code = "de"),
            expectedCachedCode = "de",
        ),
        FetchModel(
            locale = Locale.UK,
            geoResponse = null,
            cachedCode = "de",
            expectedCountry = UserCountry.Other(code = "de"),
            expectedCachedCode = "de",
        ),
        FetchModel(
            locale = Locale.UK,
            geoResponse = GeoResponse(code = ""),
            cachedCode = "de",
            expectedCountry = UserCountry.Other(code = "de"),
            expectedCachedCode = "de",
        ),
        FetchModel(
            locale = Locale.UK,
            geoResponse = null,
            cachedCode = null,
            expectedCountry = UserCountry.Other(code = "gb"),
            expectedCachedCode = null,
        ),
        FetchModel(
            locale = Locale.GERMANY,
            geoResponse = GeoResponse(code = " "),
            cachedCode = null,
            expectedCountry = UserCountry.Other(code = "de"),
            expectedCachedCode = null,
        ),
        FetchModel(
            locale = Locale.UK,
            geoResponse = null,
            cachedCode = " ",
            expectedCountry = UserCountry.Other(code = "gb"),
            expectedCachedCode = " ",
        ),
        FetchModel(
            locale = Locale("en"),
            geoResponse = null,
            cachedCode = null,
            expectedCountry = UserCountry.Other(code = ""),
            expectedCachedCode = null,
        ),
        FetchModel(
            locale = Locale("tr", "TR"),
            geoResponse = GeoResponse(code = "IT"),
            cachedCode = null,
            expectedCountry = UserCountry.Other(code = "it"),
            expectedCachedCode = "it",
        ),
    )

    private companion object {
        val GEO_COUNTRY_KEY = StorePreferencesKeys().geoUserCountryCodeKey
    }
}