package com.tangem.lib.auth.api.config

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.config.ApiEnvironment
import org.junit.jupiter.api.Test

internal class AuthConfigTest {

    private val config = Auth()

    @Test
    fun `GIVEN config WHEN read id THEN Auth id`() {
        // Assert
        assertThat(config.id).isEqualTo(Auth.ID)
    }

    @Test
    fun `GIVEN config WHEN read environments THEN expected base urls`() {
        // Act
        val actual = config.environmentConfigs.associate { it.environment to it.baseUrl }

        // Assert
        assertThat(actual).containsExactlyEntriesIn(
            mapOf(
                ApiEnvironment.DEV to "[REDACTED_ENV_URL]",
                ApiEnvironment.PROD to "https://api.tangem.org/",
            ),
        )
    }

    @Test
    fun `GIVEN config WHEN read environments THEN no config-level headers`() {
        // Act
        val hasHeaders = config.environmentConfigs.any { it.headers.isNotEmpty() }

        // Assert
        assertThat(hasHeaders).isFalse()
    }
}