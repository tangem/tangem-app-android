package com.tangem.store.datasource.config

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.config.ApiEnvironment
import com.tangem.core.remote.header.RequestHeader
import com.tangem.core.remote.header.TangemApiKeyHeaderProvider
import com.tangem.utils.ProviderSuspend
import com.tangem.utils.info.AppInfoProvider
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class NewsConfigTest {

    private val apiKeyHeader = mockk<TangemApiKeyHeaderProvider>()
    private val appInfoProvider = mockk<AppInfoProvider>()

    private lateinit var config: News

    @BeforeEach
    fun setup() {
        clearMocks(apiKeyHeader, appInfoProvider)

        every { apiKeyHeader.forEnvironment(any<ApiEnvironment>()) } returns
            RequestHeader("api-key" to ProviderSuspend { API_KEY })

        every { appInfoProvider.appVersion } returns VERSION_NAME
        every { appInfoProvider.osVersion } returns OS_VERSION
        every { appInfoProvider.language } returns LANGUAGE
        every { appInfoProvider.device } returns DEVICE

        config = News(
            apiKeyHeader = apiKeyHeader,
            appInfoProvider = appInfoProvider,
        )
    }

    @Test
    fun `GIVEN config WHEN read id THEN News id`() {
        // Assert
        assertThat(config.id).isEqualTo(News.ID)
    }

    @Test
    fun `GIVEN config WHEN read environments THEN expected base urls`() {
        // Act
        val actual = config.environmentConfigs.associate { it.environment to it.baseUrl }

        // Assert
        assertThat(actual).containsExactlyEntriesIn(
            mapOf(
                ApiEnvironment.PROD to "https://api.tangem.org/",
                ApiEnvironment.DEV to "[REDACTED_ENV_URL]",
                ApiEnvironment.MOCK to "[REDACTED_ENV_URL]",
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
            "api-key", "system_version", "version", "platform", "language", "timezone", "device",
        )
        assertThat(actual).containsAtLeastEntriesIn(
            mapOf(
                "api-key" to API_KEY,
                "system_version" to OS_VERSION,
                "version" to VERSION_NAME,
                "platform" to "android",
                "language" to LANGUAGE,
                "device" to DEVICE,
            ),
        )
        // timezone is derived from TimeZone.getDefault() at runtime — assert presence, not a fixed value.
        assertThat(actual["timezone"]).isNotNull()
    }

    private companion object {
        const val API_KEY = "news_api_key"
        const val VERSION_NAME = "debug"
        const val OS_VERSION = "Android 16"
        const val LANGUAGE = "en-US"
        const val DEVICE = "manufacturer model"
    }
}