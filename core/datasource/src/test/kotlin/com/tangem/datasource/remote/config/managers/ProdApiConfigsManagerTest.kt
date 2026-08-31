package com.tangem.datasource.remote.config.managers

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.config.ApiConfig
import com.tangem.core.remote.config.ApiEnvironment
import org.junit.jupiter.api.Test

internal class ProdApiConfigsManagerTest {

    @Test
    fun `GIVEN config present WHEN getEnvironmentConfig THEN returns default environment config`() {
        // Arrange
        val config = apiConfig(id = "A", default = ApiEnvironment.PROD, environments = listOf(ApiEnvironment.DEV, ApiEnvironment.PROD))
        val manager = ProdApiConfigsManager(apiConfigs = mapOf("A" to config))

        // Act
        val actual = manager.getEnvironmentConfig(ApiConfig.ID("A"))

        // Assert
        assertThat(actual.environment).isEqualTo(ApiEnvironment.PROD)
    }

    @Test
    fun `GIVEN unknown id WHEN getEnvironmentConfig THEN throws`() {
        // Arrange
        val manager = ProdApiConfigsManager(apiConfigs = emptyMap())

        // Act
        val error = runCatching { manager.getEnvironmentConfig(ApiConfig.ID("missing")) }.exceptionOrNull()

        // Assert
        assertThat(error).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `GIVEN default environment missing from configs WHEN getEnvironmentConfig THEN throws`() {
        // Arrange
        val config = apiConfig(id = "A", default = ApiEnvironment.PROD, environments = listOf(ApiEnvironment.DEV))
        val manager = ProdApiConfigsManager(apiConfigs = mapOf("A" to config))

        // Act
        val error = runCatching { manager.getEnvironmentConfig(ApiConfig.ID("A")) }.exceptionOrNull()

        // Assert
        assertThat(error).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `GIVEN manager WHEN read initializedState THEN it is always true`() {
        // Arrange
        val manager = ProdApiConfigsManager(apiConfigs = emptyMap())

        // Act
        manager.initialize()

        // Assert
        assertThat(manager.initializedState.value).isTrue()
    }
}