package com.tangem.spend.datasource.config

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.config.ApiEnvironment
import com.tangem.utils.info.AppInfoProvider
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

internal class TangemPayConfigTest {

    private val appInfoProvider = mockk<AppInfoProvider>()

    private val environmentConfig = TangemPayEnvironmentConfig(
        bffStaticToken = PROD_TOKEN,
        bffStaticTokenDev = DEV_TOKEN,
    )

    @BeforeEach
    fun setup() {
        clearMocks(appInfoProvider)

        every { appInfoProvider.appVersion } returns VERSION_NAME
        every { appInfoProvider.deviceScale } returns DEVICE_SCALE
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Bff {

        private val config = TangemPay.Bff(
            environmentConfig = environmentConfig,
            appInfoProvider = appInfoProvider,
        )

        @Test
        fun `GIVEN config WHEN read id THEN Bff id`() {
            // Assert
            assertThat(config.id).isEqualTo(TangemPay.Bff.ID)
        }

        @Test
        fun `GIVEN config WHEN read environments THEN expected base urls`() {
            // Act
            val actual = config.environmentConfigs.associate { it.environment to it.baseUrl }

            // Assert
            assertThat(actual).containsExactlyEntriesIn(
                mapOf(
                    ApiEnvironment.DEV to "https://api.dev.us.paera.com/bff-v2/",
                    ApiEnvironment.MOCK to "[REDACTED_ENV_URL]",
                    ApiEnvironment.PROD to "https://api.us.paera.com/bff-v2/",
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
            assertExpectedHeaders(actual, expectedApiKey = PROD_TOKEN)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Auth {

        private val config = TangemPay.Auth(
            environmentConfig = environmentConfig,
            appInfoProvider = appInfoProvider,
        )

        @Test
        fun `GIVEN config WHEN read id THEN Auth id`() {
            // Assert
            assertThat(config.id).isEqualTo(TangemPay.Auth.ID)
        }

        @Test
        fun `GIVEN config WHEN read environments THEN expected base urls`() {
            // Act
            val actual = config.environmentConfigs.associate { it.environment to it.baseUrl }

            // Assert
            assertThat(actual).containsExactlyEntriesIn(
                mapOf(
                    ApiEnvironment.DEV to "https://api.dev.us.paera.com/",
                    ApiEnvironment.MOCK to "[REDACTED_ENV_URL]",
                    ApiEnvironment.PROD to "https://api.us.paera.com/",
                ),
            )
        }

        @Test
        fun `GIVEN config WHEN read DEV headers THEN expected headers`() {
            // Arrange
            val dev = config.environmentConfigs.first { it.environment == ApiEnvironment.DEV }

            // Act
            val actual = dev.headers.mapValues { runBlocking { it.value() } }

            // Assert
            assertExpectedHeaders(actual, expectedApiKey = DEV_TOKEN)
        }
    }

    private fun assertExpectedHeaders(actual: Map<String, String>, expectedApiKey: String) {
        assertThat(actual.keys).containsExactly(
            "version", "platform", "X-API-KEY", "X-Device-Scale", "Accept-Language",
        )
        assertThat(actual).containsAtLeastEntriesIn(
            mapOf(
                "version" to VERSION_NAME,
                "platform" to "Android",
                "X-API-KEY" to expectedApiKey,
                "X-Device-Scale" to DEVICE_SCALE.toString(),
            ),
        )
        // Accept-Language is derived from the device locale at runtime — assert presence, not a fixed value.
        assertThat(actual["Accept-Language"]).isNotNull()
    }

    private companion object {
        const val VERSION_NAME = "debug"
        const val DEVICE_SCALE = 3f
        const val DEV_TOKEN = "bff_static_token_dev"
        const val PROD_TOKEN = "bff_static_token_prod"
    }
}