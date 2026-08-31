package com.tangem.store.datasource.config

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.config.ApiEnvironment
import com.tangem.core.remote.config.TangemGatewayApiConfig
import com.tangem.core.remote.header.RequestHeader
import com.tangem.utils.ProviderSuspend
import com.tangem.utils.info.AppInfoProvider
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemTechConfigTest {

    private val appInfoProvider = mockk<AppInfoProvider>()

    private lateinit var config: TangemTech

    @BeforeEach
    fun setup() {
        every { appInfoProvider.osVersion } returns OS_VERSION
        every { appInfoProvider.appVersion } returns VERSION_NAME
        every { appInfoProvider.language } returns LANGUAGE
        every { appInfoProvider.device } returns DEVICE

        config = TangemTech(
            apiKeyHeader = { RequestHeader("api-key" to ProviderSuspend { TANGEM_API_KEY }) },
            cardAuthHeader = {
                RequestHeader(
                    "card_id" to ProviderSuspend { APP_CARD_ID },
                    "card_public_key" to ProviderSuspend { APP_CARD_PUBLIC_KEY },
                )
            },
            appInfoProvider = appInfoProvider,
        )
    }

    @Test
    fun `GIVEN config WHEN read id THEN it equals TangemTech ID`() {
        assertThat(config.id).isEqualTo(TangemGatewayApiConfig.TANGEM_TECH_ID)
    }

    @Test
    fun `GIVEN config WHEN read environments THEN base urls match the tangem gateway`() {
        // Arrange
        val expected = mapOf(
            ApiEnvironment.DEV to "[REDACTED_ENV_URL]",
            ApiEnvironment.STAGE to "[REDACTED_ENV_URL]",
            ApiEnvironment.MOCK to "[REDACTED_ENV_URL]",
            ApiEnvironment.PROD to "https://api.tangem.org/",
        )

        // Act
        val actual = config.environmentConfigs.associate { it.environment to it.baseUrl }

        // Assert
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN prod environment WHEN read headers THEN they carry api key, card auth and app info`() {
        // Arrange
        val prod = config.environmentConfigs.first { it.environment == ApiEnvironment.PROD }
        val resolved = prod.headers.mapValues { runBlocking { it.value() } }

        // Act
        val deterministic = resolved.filterKeys { it != TIMEZONE_HEADER }

        // Assert
        assertThat(deterministic).isEqualTo(
            mapOf(
                "api-key" to TANGEM_API_KEY,
                "card_id" to APP_CARD_ID,
                "card_public_key" to APP_CARD_PUBLIC_KEY,
                "system_version" to OS_VERSION,
                "version" to VERSION_NAME,
                "platform" to "android",
                "language" to LANGUAGE,
                "device" to DEVICE,
            ),
        )
        assertThat(resolved.keys).contains(TIMEZONE_HEADER)
    }

    private companion object {

        const val TIMEZONE_HEADER = "timezone"
        const val TANGEM_API_KEY = "tangem_api_key"
        const val APP_CARD_ID = "app_card_id"
        const val APP_CARD_PUBLIC_KEY = "Bearer app_public_key"
        const val OS_VERSION = "Android 16"
        const val VERSION_NAME = "debug"
        const val LANGUAGE = "en-US"
        const val DEVICE = "Samsung Galaxy"
    }
}