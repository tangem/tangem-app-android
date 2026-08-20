package com.tangem.tap.common.analytics.handlers.opentelemetry

import android.app.Application
import com.google.common.truth.Truth.assertThat
import com.tangem.datasource.local.config.environment.EnvironmentConfig
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

    private val featureToggles: OtelFeatureToggles = mockk()
    private val dispatchers = TestingCoroutineDispatcherProvider()

    @TempDir
    lateinit var tempDir: File

    private fun createApplication(): Application = mockk(relaxed = true) {
        every { cacheDir } returns tempDir
    }

    private fun createHolder(
        apiKey: String?,
        isToggleEnabled: Boolean,
        appScope: AppCoroutineScope,
        application: Application = createApplication(),
    ): OpenTelemetryMetricsHolder {
        every { featureToggles.isMetricsEnabled } returns isToggleEnabled
        return OpenTelemetryMetricsHolder(
            application = application,
            environmentConfig = EnvironmentConfig(otlpApiKey = apiKey),
            featureToggles = featureToggles,
            dispatchers = dispatchers,
            appScope = appScope,
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class InitializeSkipped {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN toggle or key missing WHEN initialize THEN meter stays null`(model: SkipModel) = runTest {
            // Arrange
            val holder = createHolder(
                apiKey = model.apiKey,
                isToggleEnabled = model.isToggleEnabled,
                appScope = TestAppCoroutineScope(testScope = this),
            )

            // Act
            holder.initialize()
            advanceUntilIdle()

            // Assert
            assertThat(holder.getMeter()).isNull()
        }

        private fun provideTestModels() = listOf(
            SkipModel(isToggleEnabled = false, apiKey = "key"),
            SkipModel(isToggleEnabled = true, apiKey = null),
            SkipModel(isToggleEnabled = true, apiKey = ""),
        )
    }

    data class SkipModel(val isToggleEnabled: Boolean, val apiKey: String?)

    @Test
    fun `GIVEN toggle on and key present WHEN initialize THEN meter is available and flush succeeds`() = runTest {
        // Arrange
        val holder = createHolder(
            apiKey = "dev-key",
            isToggleEnabled = true,
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
            isToggleEnabled = true,
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