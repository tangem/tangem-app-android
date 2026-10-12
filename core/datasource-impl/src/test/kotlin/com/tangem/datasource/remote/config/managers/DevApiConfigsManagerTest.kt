package com.tangem.datasource.remote.config.managers

import androidx.datastore.preferences.core.emptyPreferences
import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.tangem.core.remote.config.ApiConfig
import com.tangem.core.remote.config.ApiEnvironment
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.test.core.TestAppCoroutineScope
import com.tangem.test.core.datastore.MockStateDataStore
import com.tangem.test.core.datastore.createAppPreferencesStore
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class DevApiConfigsManagerTest {

    private val dispatchers = TestingCoroutineDispatcherProvider()
    private val moshi = Moshi.Builder().build()

    private fun TestScope.createStore(): AppPreferencesStore = createAppPreferencesStore(
        moshi = moshi,
        dispatchers = dispatchers,
        preferencesDataStore = MockStateDataStore(default = emptyPreferences()),
    )

    @Test
    fun `GIVEN not initialized WHEN getEnvironmentConfig THEN returns default env and initialized is false`() = runTest {
        // Arrange
        val config = apiConfig(id = "A", default = ApiEnvironment.PROD, environments = listOf(ApiEnvironment.PROD, ApiEnvironment.DEV))
        val manager = DevApiConfigsManager(mapOf("A" to config), createStore(), TestAppCoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        // Assert
        assertThat(manager.getEnvironmentConfig(ApiConfig.ID("A")).environment).isEqualTo(ApiEnvironment.PROD)
        assertThat(manager.initializedState.value).isFalse()
    }

    @Test
    fun `GIVEN environment persisted WHEN initialize THEN saved config uses saved env and unsaved keeps default`() = runTest {
        // Arrange
        val a = apiConfig(id = "A", default = ApiEnvironment.PROD, environments = listOf(ApiEnvironment.PROD, ApiEnvironment.DEV))
        val b = apiConfig(id = "B", default = ApiEnvironment.PROD, environments = listOf(ApiEnvironment.PROD))
        val manager = DevApiConfigsManager(mapOf("A" to a, "B" to b), createStore(), TestAppCoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        manager.changeEnvironment(id = "A", environment = ApiEnvironment.DEV)
        advanceUntilIdle()

        // Act
        manager.initialize()
        advanceUntilIdle()

        // Assert
        assertThat(manager.getEnvironmentConfig(ApiConfig.ID("A")).environment).isEqualTo(ApiEnvironment.DEV)
        assertThat(manager.getEnvironmentConfig(ApiConfig.ID("B")).environment).isEqualTo(ApiEnvironment.PROD)
        assertThat(manager.initializedState.value).isTrue()
    }

    @Test
    fun `GIVEN no saved environments WHEN initialize THEN configs keep default and initialized becomes true`() = runTest {
        // Arrange
        val config = apiConfig(id = "A", default = ApiEnvironment.PROD, environments = listOf(ApiEnvironment.PROD, ApiEnvironment.DEV))
        val manager = DevApiConfigsManager(mapOf("A" to config), createStore(), TestAppCoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        // Act
        manager.initialize()
        advanceUntilIdle()

        // Assert
        assertThat(manager.getEnvironmentConfig(ApiConfig.ID("A")).environment).isEqualTo(ApiEnvironment.PROD)
        assertThat(manager.initializedState.value).isTrue()
    }

    @Test
    fun `GIVEN unknown id WHEN getEnvironmentConfig THEN throws`() = runTest {
        // Arrange
        val manager = DevApiConfigsManager(emptyMap(), createStore(), TestAppCoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        // Act
        val error = runCatching { manager.getEnvironmentConfig(ApiConfig.ID("x")) }.exceptionOrNull()

        // Assert
        assertThat(error).isInstanceOf(IllegalStateException::class.java)
    }
}