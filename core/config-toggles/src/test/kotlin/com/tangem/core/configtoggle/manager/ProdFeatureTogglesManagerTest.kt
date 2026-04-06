package com.tangem.core.configtoggle.manager

import com.google.common.truth.Truth
import com.tangem.core.configtoggle.feature.impl.ProdFeatureTogglesManager
import com.tangem.core.configtoggle.feature.provider.FeatureTogglesProvider
import com.tangem.core.configtoggle.version.VersionProvider
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.*
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ProdFeatureTogglesManagerTest {

    private val versionProvider = mockk<VersionProvider>()
    private val featureTogglesProvider = mockk<FeatureTogglesProvider>()

    private val testToggles = mapOf(
        "ENABLED_TOGGLE" to "1.0.0",
        "DISABLED_TOGGLE" to "undefined",
    )

    private fun getExpectedToggles(appVersion: String?): Map<String, Boolean> {
        return testToggles.mapValues { (_, version) ->
            version != "undefined" && !appVersion.isNullOrEmpty()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Initialization {

        @BeforeEach
        fun setupEach() {
            every { featureTogglesProvider.getToggles() } returns testToggles
        }

        @AfterEach
        fun tearDownEach() {
            clearMocks(versionProvider, featureTogglesProvider)
        }

        @Test
        fun `successfully initialize storage`() = runTest {
            // Arrange
            val appVersion = "1.0.0"
            every { versionProvider.get() } returns appVersion

            // Act
            val actual = ProdFeatureTogglesManager(versionProvider, featureTogglesProvider).getProdFeatureToggles()

            // Assert
            val expected = getExpectedToggles(appVersion)
            Truth.assertThat(actual).containsExactlyEntriesIn(expected)

            coVerifyOrder { versionProvider.get() }
        }

        @Test
        fun `successfully initialize storage if versionProvider returns null`() = runTest {
            // Arrange
            val appVersion: String? = null
            every { versionProvider.get() } returns appVersion

            // Act
            val actual = ProdFeatureTogglesManager(versionProvider, featureTogglesProvider).getProdFeatureToggles()

            // Assert
            val expected = getExpectedToggles(appVersion)
            Truth.assertThat(actual).containsExactlyEntriesIn(expected)

            coVerifyOrder { versionProvider.get() }
        }

        @Test
        fun `successfully initialize storage if versionProvider returns empty string`() = runTest {
            // Arrange
            val appVersion = ""
            every { versionProvider.get() } returns appVersion

            // Act
            val actual = ProdFeatureTogglesManager(versionProvider, featureTogglesProvider).getProdFeatureToggles()

            // Assert
            val expected = getExpectedToggles(appVersion)
            Truth.assertThat(actual).containsExactlyEntriesIn(expected)

            coVerifyOrder { versionProvider.get() }
        }

        @Test
        fun `failure initialize storage if versionProvider throws exception`() = runTest {
            // Arrange
            val exception = Exception("Test exception")
            every { versionProvider.get() } throws exception

            // Act
            val actual = runCatching { ProdFeatureTogglesManager(versionProvider, featureTogglesProvider) }
                .exceptionOrNull()!!

            // Assert
            Truth.assertThat(actual).isInstanceOf(exception::class.java)
            Truth.assertThat(actual).hasMessageThat().isEqualTo(exception.message)

            coVerifyOrder { versionProvider.get() }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class IsFeatureEnabled {

        private lateinit var manager: ProdFeatureTogglesManager

        @BeforeAll
        fun setupAll() {
            every { featureTogglesProvider.getToggles() } returns testToggles
            every { versionProvider.get() } returns "1.0.0"
            manager = ProdFeatureTogglesManager(versionProvider, featureTogglesProvider)
        }

        @AfterAll
        fun tearDownAll() {
            clearMocks(versionProvider, featureTogglesProvider)
        }

        @ParameterizedTest
        @ProvideTestModels
        fun isFeatureEnabled(model: IsFeatureEnabledModel) {
            // Act
            val actual = manager.isFeatureEnabledByName(name = model.name)

            // Assert
            Truth.assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            IsFeatureEnabledModel(name = "ENABLED_TOGGLE", expected = true),
            IsFeatureEnabledModel(name = "DISABLED_TOGGLE", expected = false),
        )
    }

    data class IsFeatureEnabledModel(val name: String, val expected: Boolean)
}