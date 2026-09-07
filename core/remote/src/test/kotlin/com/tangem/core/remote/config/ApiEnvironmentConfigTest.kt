package com.tangem.core.remote.config

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class ApiEnvironmentConfigTest {

    @Test
    fun `GIVEN baseUrl without trailing slash WHEN construct THEN throws`() {
        // Act
        val error = runCatching {
            ApiEnvironmentConfig(environment = ApiEnvironment.PROD, baseUrl = "https://api.tangem.org")
        }.exceptionOrNull()

        // Assert
        assertThat(error).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `GIVEN baseUrl with trailing slash WHEN construct THEN it is accepted`() {
        // Act
        val config = ApiEnvironmentConfig(environment = ApiEnvironment.PROD, baseUrl = "https://api.tangem.org/")

        // Assert
        assertThat(config.baseUrl).endsWith("/")
    }
}