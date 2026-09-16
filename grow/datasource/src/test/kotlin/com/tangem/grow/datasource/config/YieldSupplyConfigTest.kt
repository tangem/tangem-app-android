package com.tangem.grow.datasource.config

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.config.ApiEnvironment
import com.tangem.core.remote.header.CardAuthHeaderProvider
import com.tangem.core.remote.header.RequestHeader
import com.tangem.utils.ProviderSuspend
import com.tangem.utils.info.AppInfoProvider
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class YieldSupplyConfigTest {

    private val growEnvironmentConfig = mockk<GrowEnvironmentConfig>()
    private val cardAuthHeader = mockk<CardAuthHeaderProvider>()
    private val appInfoProvider = mockk<AppInfoProvider>()

    // Lazy: YieldSupply reads cardAuthHeader.get() eagerly while building environmentConfigs, so the
    // config must be constructed after the mocks are stubbed in setup().
    private val config by lazy {
        YieldSupply(
            growEnvironmentConfig = growEnvironmentConfig,
            cardAuthHeader = cardAuthHeader,
            appInfoProvider = appInfoProvider,
        )
    }

    @BeforeEach
    fun setup() {
        clearMocks(growEnvironmentConfig, cardAuthHeader, appInfoProvider)

        every { growEnvironmentConfig.yieldModuleApiKey } returns YIELD_API_KEY
        every { cardAuthHeader.get() } returns RequestHeader(
            "card_id" to ProviderSuspend { CARD_ID },
            "card_public_key" to ProviderSuspend { CARD_PUBLIC_KEY },
        )
        every { appInfoProvider.osVersion } returns OS_VERSION
        every { appInfoProvider.appVersion } returns VERSION_NAME
        every { appInfoProvider.language } returns LANGUAGE
        every { appInfoProvider.device } returns DEVICE
    }

    @Test
    fun `GIVEN config WHEN read id THEN YieldSupply id`() {
        // Assert
        assertThat(config.id).isEqualTo(YieldSupply.ID)
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
                ApiEnvironment.PROD to "https://yield.tangem.org/",
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
            "card_id", "card_public_key",
        )
        assertThat(actual).containsAtLeastEntriesIn(
            mapOf(
                "api-key" to YIELD_API_KEY,
                "system_version" to OS_VERSION,
                "version" to VERSION_NAME,
                "platform" to "android",
                "language" to LANGUAGE,
                "device" to DEVICE,
                "card_id" to CARD_ID,
                "card_public_key" to CARD_PUBLIC_KEY,
            ),
        )
        // timezone is derived from TimeZone.getDefault() at runtime — assert presence, not a fixed value.
        assertThat(actual["timezone"]).isNotNull()
    }

    private companion object {
        const val YIELD_API_KEY = "yield_api_key"
        const val OS_VERSION = "Android 16"
        const val VERSION_NAME = "debug"
        const val LANGUAGE = "en-US"
        const val DEVICE = "manufacturer model"
        const val CARD_ID = "app_card_id"
        const val CARD_PUBLIC_KEY = "Bearer app_public_key"
    }
}