package com.tangem.grow.datasource.config

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.config.ApiEnvironment
import com.tangem.grow.datasource.express.ExpressAuthProvider
import com.tangem.utils.info.AppInfoProvider
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class ExpressConfigTest {

    private val growEnvironmentConfig = mockk<GrowEnvironmentConfig>()
    private val expressAuthProvider = mockk<ExpressAuthProvider>()
    private val appInfoProvider = mockk<AppInfoProvider>()

    private val config = Express(
        growEnvironmentConfig = growEnvironmentConfig,
        expressAuthProvider = expressAuthProvider,
        appInfoProvider = appInfoProvider,
    )

    @BeforeEach
    fun setup() {
        clearMocks(growEnvironmentConfig, expressAuthProvider, appInfoProvider)

        every { growEnvironmentConfig.express } returns
            GrowEnvironmentConfig.ExpressModel(apiKey = EXPRESS_API_KEY, signVerifierPublicKey = "")
        every { expressAuthProvider.getSessionId() } returns SESSION_ID
        every { appInfoProvider.osVersion } returns OS_VERSION
        every { appInfoProvider.appVersion } returns VERSION_NAME
        every { appInfoProvider.language } returns LANGUAGE
        every { appInfoProvider.device } returns DEVICE
    }

    @Test
    fun `GIVEN config WHEN read id THEN Express id`() {
        // Assert
        assertThat(config.id).isEqualTo(Express.ID)
    }

    @Test
    fun `GIVEN config WHEN read environments THEN expected base urls`() {
        // Act
        val actual = config.environmentConfigs.associate { it.environment to it.baseUrl }

        // Assert
        assertThat(actual).containsExactlyEntriesIn(
            mapOf(
                ApiEnvironment.DEV to "[REDACTED_ENV_URL]",
                ApiEnvironment.DEV_2 to "[REDACTED_ENV_URL]",
                ApiEnvironment.DEV_3 to "[REDACTED_ENV_URL]",
                ApiEnvironment.STAGE to "[REDACTED_ENV_URL]",
                ApiEnvironment.STAGE_2 to "[REDACTED_ENV_URL]",
                ApiEnvironment.STAGE_3 to "[REDACTED_ENV_URL]",
                ApiEnvironment.MOCK to "[REDACTED_ENV_URL]",
                ApiEnvironment.PROD to "https://express.tangem.com/v1/",
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
            "api-key", "session-id", "system_version", "version", "platform", "language", "timezone",
            "device",
        )
        assertThat(actual).containsAtLeastEntriesIn(
            mapOf(
                "api-key" to EXPRESS_API_KEY,
                "session-id" to SESSION_ID,
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
        const val EXPRESS_API_KEY = "express_api_key"
        const val SESSION_ID = "session_id"
        const val OS_VERSION = "Android 16"
        const val VERSION_NAME = "debug"
        const val LANGUAGE = "en-US"
        const val DEVICE = "manufacturer model"
    }
}