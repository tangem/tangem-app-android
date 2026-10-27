package com.tangem.tap.common.analytics.handlers.opentelemetry

import android.app.Application
import com.google.common.truth.Truth.assertThat
import com.tangem.datasource.local.config.environment.EnvironmentConfig
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.domain.settings.usercountry.models.UserCountry
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.TestAppCoroutineScope
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import java.io.File

internal class OpenTelemetryMetricsHolderTest {

    private val dispatchers = TestingCoroutineDispatcherProvider()

    @TempDir
    lateinit var tempDir: File

    private fun createApplication(): Application = mockk(relaxed = true) {
        every { cacheDir } returns tempDir
    }

    private fun createHolder(
        apiKey: String?,
        appScope: AppCoroutineScope,
        application: Application = createApplication(),
    ): OpenTelemetryMetricsHolder {
        val settingsRepository: SettingsRepository = mockk {
            every { getUserCountryCodeSync() } returns UserCountry.Other(code = "de")
        }
        return OpenTelemetryMetricsHolder(
            application = application,
            environmentConfig = EnvironmentConfig(otlpApiKey = apiKey),
            settingsRepository = settingsRepository,
            dispatchers = dispatchers,
            appScope = appScope,
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class InitializeSkipped {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN key missing WHEN initialize THEN meter stays null`(model: SkipModel) = runTest {
            // Arrange
            val holder = createHolder(
                apiKey = model.apiKey,
                appScope = TestAppCoroutineScope(testScope = this),
            )

            // Act
            holder.initialize()
            advanceUntilIdle()

            // Assert
            assertThat(holder.getMeter()).isNull()
        }

        private fun provideTestModels() = listOf(
            SkipModel(apiKey = null),
            SkipModel(apiKey = ""),
        )
    }

    data class SkipModel(val apiKey: String?)

    @Test
    fun `GIVEN key present WHEN initialize THEN meter is available and flush succeeds`() = runTest {
        // Arrange
        val holder = createHolder(
            apiKey = "dev-key",
            appScope = TestAppCoroutineScope(testScope = this),
        )

        // Act
        holder.initialize()
        advanceUntilIdle()

        // Assert
        assertThat(holder.getMeter()).isNotNull()
        holder.flush()
    }

    @Test
    fun `GIVEN client setup fails WHEN initialize THEN meter stays null and nothing is thrown`() = runTest {
        // Arrange
        val application: Application = mockk(relaxed = true) {
            every { cacheDir } throws IllegalStateException("no cache dir")
        }
        val holder = createHolder(
            apiKey = "dev-key",
            appScope = TestAppCoroutineScope(testScope = this),
            application = application,
        )

        // Act
        holder.initialize()
        advanceUntilIdle()

        // Assert
        assertThat(holder.getMeter()).isNull()
    }
}