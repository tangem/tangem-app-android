package com.tangem.grow.datasource.config

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.config.ApiEnvironment
import com.tangem.grow.datasource.collectibles.CollectiblesAuthProvider
import com.tangem.utils.info.AppInfoProvider
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

internal class CollectiblesConfigTest {

    private val collectiblesAuthProvider = mockk<CollectiblesAuthProvider>()
    private val appInfoProvider = mockk<AppInfoProvider>()

    private val config = Collectibles(
        collectiblesAuthProvider = collectiblesAuthProvider,
        appInfoProvider = appInfoProvider,
    )

    @BeforeEach
    fun setup() {
        clearMocks(collectiblesAuthProvider, appInfoProvider)

        every { collectiblesAuthProvider.getSessionId() } returns SESSION_ID
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
    fun `GIVEN config WHEN read PROD headers THEN expected headers`() {
        // Arrange
        val prod = config.environmentConfigs.first { it.environment == ApiEnvironment.PROD }

        // Act
        val actual = prod.headers.mapValues { runBlocking { it.value() } }

        // Assert
        assertThat(actual.keys).containsExactly(
            "session-id", "system_version", "version", "platform", "language", "timezone", "device",
        )
        assertThat(actual).containsAtLeastEntriesIn(
            mapOf(
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

    @Test
    fun `GIVEN config WHEN read MOCK headers THEN expected headers`() {
        // Arrange
        val mock = config.environmentConfigs.first { it.environment == ApiEnvironment.MOCK }

        // Act
        val actual = mock.headers.mapValues { runBlocking { it.value() } }

        // Assert
        assertThat(actual.keys).containsExactly(
            "session-id", "system_version", "version", "platform", "language", "timezone", "device",
        )
        assertThat(actual).containsAtLeastEntriesIn(
            mapOf(
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

    @Test
    fun `GIVEN config WHEN read session-id THEN it is delegated to the auth provider`() {
        // Arrange
        val header = config.environmentConfigs.first().headers.getValue("session-id")

        // Act
        val actual = runBlocking { header() }

        // Assert
        assertThat(actual).isEqualTo(SESSION_ID)
        verify(exactly = 1) { collectiblesAuthProvider.getSessionId() }
    }

    /**

     * inside the config would change per request or per environment instead of identifying the app session.
     */
    @Test
    fun `GIVEN a session scoped provider WHEN session-id read twice THEN the same id is returned`() {
        // Arrange
        val sessionScoped = Collectibles(
            collectiblesAuthProvider = SessionScopedAuthProvider(),
            appInfoProvider = appInfoProvider,
        )
        val dev = sessionScoped.environmentConfigs.first { it.environment == ApiEnvironment.DEV }
        val prod = sessionScoped.environmentConfigs.first { it.environment == ApiEnvironment.PROD }

        // Act
        val first = runBlocking { dev.headers.getValue("session-id")() }
        val second = runBlocking { dev.headers.getValue("session-id")() }
        val otherEnvironment = runBlocking { prod.headers.getValue("session-id")() }

        // Assert
        assertThat(first).isEqualTo(second)
        assertThat(first).isEqualTo(otherEnvironment)
    }

    /** Mirrors `DefaultCollectiblesAuthProvider`: one id per instance, which DI scopes to the app. */
    private class SessionScopedAuthProvider : CollectiblesAuthProvider {

        private val uuid = AtomicReference(UUID.randomUUID())

        override fun getSessionId(): String = uuid.get().toString()
    }

    private companion object {
        const val SESSION_ID = "session_id"
        const val OS_VERSION = "Android 16"
        const val VERSION_NAME = "debug"
        const val LANGUAGE = "en-US"
        const val DEVICE = "manufacturer model"
    }
}