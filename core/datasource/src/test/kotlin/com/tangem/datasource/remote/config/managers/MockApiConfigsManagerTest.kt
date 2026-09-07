package com.tangem.datasource.remote.config.managers

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.config.ApiConfig
import com.tangem.core.remote.config.ApiEnvironment
import com.tangem.core.remote.config.ApiEnvironmentConfig
import com.tangem.core.remote.config.managers.MutableApiConfigsManager
import com.tangem.test.core.TestAppCoroutineScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class MockApiConfigsManagerTest {

    @Test
    fun `GIVEN configs WHEN getEnvironmentConfig THEN returns default environment initially`() = runTest {
        // Arrange
        val config = apiConfig(id = "A", default = ApiEnvironment.MOCK, environments = listOf(ApiEnvironment.MOCK, ApiEnvironment.DEV))
        val manager = MockApiConfigsManager(mapOf("A" to config), TestAppCoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        // Act
        val actual = manager.getEnvironmentConfig(ApiConfig.ID("A"))

        // Assert
        assertThat(actual.environment).isEqualTo(ApiEnvironment.MOCK)
    }

    @Test
    fun `GIVEN changeEnvironment for one id WHEN getEnvironmentConfig THEN only that config changes`() = runTest {
        // Arrange
        val a = apiConfig(id = "A", default = ApiEnvironment.MOCK, environments = listOf(ApiEnvironment.MOCK, ApiEnvironment.DEV))
        val b = apiConfig(id = "B", default = ApiEnvironment.MOCK, environments = listOf(ApiEnvironment.MOCK, ApiEnvironment.DEV))
        val manager = MockApiConfigsManager(mapOf("A" to a, "B" to b), TestAppCoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        // Act
        manager.changeEnvironment(id = "A", environment = ApiEnvironment.DEV)

        // Assert
        assertThat(manager.getEnvironmentConfig(ApiConfig.ID("A")).environment).isEqualTo(ApiEnvironment.DEV)
        assertThat(manager.getEnvironmentConfig(ApiConfig.ID("B")).environment).isEqualTo(ApiEnvironment.MOCK)
    }

    @Test
    fun `GIVEN changeEnvironment for all WHEN read THEN supported configs change and unsupported keep current`() = runTest {
        // Arrange
        val supported = apiConfig(id = "A", default = ApiEnvironment.MOCK, environments = listOf(ApiEnvironment.MOCK, ApiEnvironment.DEV))
        val unsupported = apiConfig(id = "B", default = ApiEnvironment.MOCK, environments = listOf(ApiEnvironment.MOCK))
        val manager = MockApiConfigsManager(mapOf("A" to supported, "B" to unsupported), TestAppCoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        // Act
        manager.changeEnvironment(environment = ApiEnvironment.DEV)

        // Assert
        assertThat(manager.getEnvironmentConfig(ApiConfig.ID("A")).environment).isEqualTo(ApiEnvironment.DEV)
        assertThat(manager.getEnvironmentConfig(ApiConfig.ID("B")).environment).isEqualTo(ApiEnvironment.MOCK)
    }

    @Test
    fun `GIVEN unknown id WHEN getEnvironmentConfig THEN throws`() = runTest {
        // Arrange
        val manager = MockApiConfigsManager(emptyMap(), TestAppCoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        // Act
        val error = runCatching { manager.getEnvironmentConfig(ApiConfig.ID("x")) }.exceptionOrNull()

        // Assert
        assertThat(error).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `GIVEN registered listener WHEN environment changes THEN listener notified with current then new config`() = runTest {
        // Arrange
        val config = apiConfig(id = "A", default = ApiEnvironment.MOCK, environments = listOf(ApiEnvironment.MOCK, ApiEnvironment.DEV))
        val manager = MockApiConfigsManager(mapOf("A" to config), TestAppCoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        val received = mutableListOf<ApiEnvironment>()
        manager.addListener(
            object : MutableApiConfigsManager.ApiConfigEnvChangeListener(ApiConfig.ID("A")) {
                override fun onChange(environmentConfig: ApiEnvironmentConfig) {
                    received += environmentConfig.environment
                }
            },
        )
        advanceUntilIdle()

        // Act
        manager.changeEnvironment(id = "A", environment = ApiEnvironment.DEV)
        advanceUntilIdle()

        // Assert
        assertThat(received).containsExactly(ApiEnvironment.MOCK, ApiEnvironment.DEV).inOrder()
    }
}