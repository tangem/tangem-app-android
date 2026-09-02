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

internal class CollectiblesConfigTest {

    private val appInfoProvider = mockk<AppInfoProvider>()

    private val config = Collectibles(appInfoProvider = appInfoProvider)

    @BeforeEach
    fun setup() {
        clearMocks(appInfoProvider)

        every { appInfoProvider.osVersion } returns OS_VERSION
        every { appInfoProvider.appVersion } returns VERSION_NAME
        every { appInfoProvider.language } returns LANGUAGE
        every { appInfoProvider.device } returns DEVICE
    }

    @Test
    fun `GIVEN config WHEN read id THEN Collectibles id`() {
        // Assert
        assertThat(config.id).isEqualTo(Collectibles.ID)
    }

    @Test
    fun `GIVEN config WHEN read environments THEN expected base urls`() {
        // Act
        val actual = config.environmentConfigs.associate { it.environment to it.baseUrl }

        // Assert
        assertThat(actual).containsExactlyEntriesIn(
            mapOf(
                ApiEnvironment.DEV to "[REDACTED_ENV_URL]",
                ApiEnvironment.MOCK to "[REDACTED_ENV_URL]",
                ApiEnvironment.PROD to "[REDACTED_ENV_URL]",
            ),
        )
    }

    @Test
    fun `GIVEN config WHEN read default environment THEN it is declared in environmentConfigs`() {
        // Act
        val declared = config.environmentConfigs.map { it.environment }

        // Assert
        // A default the configs don't declare makes the manager fail at startup for that build type.
        assertThat(declared).contains(config.defaultEnvironment)
    }

    @Test
    fun `GIVEN config WHEN read MOCK headers THEN expected headers`() {
        // Arrange
        val mock = config.environmentConfigs.first { it.environment == ApiEnvironment.MOCK }

        // Act
        val actual = mock.headers.mapValues { runBlocking { it.value() } }

        // Assert
        assertThat(actual.keys).containsExactly(
            "system_version", "version", "platform", "language", "timezone", "device",
        )
        assertThat(actual).containsAtLeastEntriesIn(
            mapOf(
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
        const val OS_VERSION = "Android 16"
        const val VERSION_NAME = "debug"
        const val LANGUAGE = "en-US"
        const val DEVICE = "manufacturer model"
    }
}