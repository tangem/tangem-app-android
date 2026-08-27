package com.tangem.datasource.api.common.config

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.config.ApiEnvironment
import com.tangem.datasource.api.common.AuthProvider
import com.tangem.datasource.utils.AuthenticationHeader
import com.tangem.datasource.utils.TangemApiKeyHeader
import com.tangem.utils.ProviderSuspend
import com.tangem.utils.info.AppInfoProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class TangemTechConfigTest {

    private val authProvider = mockk<AuthProvider>()
    private val appInfoProvider = mockk<AppInfoProvider>()
    private val apiKeyProvider = mockk<ProviderSuspend<String>>()

    // The config builds its per-environment headers eagerly at construction (reading the mocked
    // providers), so it must be created after the stubbing below — not as a field initializer.
    private lateinit var config: TangemTech

    @BeforeEach
    fun setup() {
        clearMocks(authProvider, appInfoProvider)

        every { appInfoProvider.appVersion } returns VERSION_NAME
        every { appInfoProvider.osVersion } returns OS_VERSION
        every { appInfoProvider.language } returns LANGUAGE
        every { appInfoProvider.device } returns DEVICE
        every { appInfoProvider.deviceScale } returns DEVICE_SCALE

        every { authProvider.getApiKey(any()) } returns apiKeyProvider
        coEvery { apiKeyProvider.invoke() } returns TANGEM_API_KEY
        coEvery { authProvider.getCardId() } returns CARD_ID
        coEvery { authProvider.getCardPublicKey() } returns CARD_PUBLIC_KEY

        config = TangemTech(
            apiKeyHeader = { environment -> TangemApiKeyHeader(authProvider, environment) },
            cardAuthHeader = { AuthenticationHeader(authProvider) },
            appInfoProvider = appInfoProvider,
        )
    }

    @Test
    fun `GIVEN config WHEN read id THEN TangemTech id`() {
        // Assert
        assertThat(config.id).isEqualTo(TangemTech.ID)
    }

    @Test
    fun `GIVEN config WHEN read environments THEN expected base urls`() {
        // Act
        val actual = config.environmentConfigs.associate { it.environment to it.baseUrl }

        // Assert
        assertThat(actual).containsExactlyEntriesIn(
            mapOf(
                ApiEnvironment.DEV to "[REDACTED_ENV_URL]",
                ApiEnvironment.STAGE to "[REDACTED_ENV_URL]",
                ApiEnvironment.MOCK to "[REDACTED_ENV_URL]",
                ApiEnvironment.PROD to "https://api.tangem.org/",
            ),
        )
    }

    @Test
    fun `GIVEN config WHEN read PROD headers THEN expected headers`() {
        // Arrange
        val prod = config.environmentConfigs.first { it.environment == ApiEnvironment.PROD }

        // Act
        val actual = prod.headers.mapValues { runBlocking { it.value() } }

        // Assert
        assertThat(actual.keys).containsExactly(
            "api-key", "card_id", "card_public_key", "version", "platform", "system_version",
            "language", "timezone", "device",
        )
        assertThat(actual).containsAtLeastEntriesIn(
            mapOf(
                "api-key" to TANGEM_API_KEY,
                "card_id" to CARD_ID,
                "card_public_key" to CARD_PUBLIC_KEY,
                "version" to VERSION_NAME,
                "platform" to "android",
                "system_version" to OS_VERSION,
                "language" to LANGUAGE,
                "device" to DEVICE,
            ),
        )
        // timezone is derived from TimeZone.getDefault() at runtime — assert presence, not a fixed value.
        assertThat(actual["timezone"]).isNotNull()
    }

    private companion object {
        const val VERSION_NAME = "debug"
        const val OS_VERSION = "Android 16"
        const val LANGUAGE = "en-US"
        const val DEVICE = "manufacturer model"
        const val DEVICE_SCALE = 3f
        const val CARD_ID = "app_card_id"
        const val CARD_PUBLIC_KEY = "Bearer app_public_key"
        const val TANGEM_API_KEY = "tangem_api_key"
    }
}