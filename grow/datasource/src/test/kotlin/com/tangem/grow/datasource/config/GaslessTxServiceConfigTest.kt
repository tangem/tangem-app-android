package com.tangem.grow.datasource.config

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.config.ApiEnvironment
import com.tangem.utils.info.AppInfoProvider
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class GaslessTxServiceConfigTest {

    private val growEnvironmentConfig = mockk<GrowEnvironmentConfig>()
    private val appInfoProvider = mockk<AppInfoProvider>()

    private val config = GaslessTxService(
        growEnvironmentConfig = growEnvironmentConfig,
        appInfoProvider = appInfoProvider,
    )

    @BeforeEach
    fun setup() {
        clearMocks(growEnvironmentConfig, appInfoProvider)

        every { growEnvironmentConfig.gaslessTxApiKey } returns GASLESS_API_KEY
        every { appInfoProvider.osVersion } returns OS_VERSION
        every { appInfoProvider.appVersion } returns VERSION_NAME
        every { appInfoProvider.language } returns LANGUAGE
        every { appInfoProvider.device } returns DEVICE
    }

    @Test
    fun `GIVEN config WHEN read id THEN GaslessTxService id`() {
        // Assert
        assertThat(config.id).isEqualTo(GaslessTxService.ID)
    }

    @Test
    fun `GIVEN config WHEN read environments THEN expected base urls`() {
        // Act
        val actual = config.environmentConfigs.associate { it.environment to it.baseUrl }

        // Assert
        assertThat(actual).containsExactlyEntriesIn(
            mapOf(
                ApiEnvironment.PROD to "https://gasless.tangem.org/",
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
            "system_version", "version", "platform", "language", "timezone", "device", "Authorization",
        )
        assertThat(actual).containsAtLeastEntriesIn(
            mapOf(
                "system_version" to OS_VERSION,
                "version" to VERSION_NAME,
                "platform" to "android",
                "language" to LANGUAGE,
                "device" to DEVICE,
                "Authorization" to "Bearer $GASLESS_API_KEY",
            ),
        )
        // timezone is derived from TimeZone.getDefault() at runtime — assert presence, not a fixed value.
        assertThat(actual["timezone"]).isNotNull()
    }

    private companion object {
        const val GASLESS_API_KEY = "gasless_api_key"
        const val OS_VERSION = "Android 16"
        const val VERSION_NAME = "debug"
        const val LANGUAGE = "en-US"
        const val DEVICE = "manufacturer model"
    }
}