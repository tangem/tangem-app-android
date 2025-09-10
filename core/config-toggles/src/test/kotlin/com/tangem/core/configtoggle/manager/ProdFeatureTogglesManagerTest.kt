package com.tangem.core.configtoggle.manager

import com.google.common.truth.Truth
import com.tangem.common.test.utils.ProvideTestModels
import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.impl.ProdFeatureTogglesManager
import com.tangem.core.configtoggle.version.VersionProvider
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.*
import org.junit.jupiter.params.ParameterizedTest

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ProdFeatureTogglesManagerTest {

    private val versionProvider = mockk<VersionProvider>()

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Initialization {

        @BeforeAll
        fun setupAll() {
            val featureToggles = mapOf("TOGGLE_1" to "1.0.0", "TOGGLE_2" to "2.0.0")

            mockkObject(FeatureToggles)
            every { FeatureToggles.values } returns featureToggles
        }

        @AfterAll
        fun tearDownAll() {
            unmockkObject(FeatureToggles)
        }

        @AfterEach
        fun tearDownEach() {
            clearMocks(versionProvider)
        }

        @Test
        fun `successfully initialize storage`() = runTest {
            // Arrange
            val appVersion = "1.0.0"
            every { versionProvider.get() } returns appVersion

            // Act
            val actual = ProdFeatureTogglesManager(versionProvider).getProdFeatureToggles()

            // Assert
            val expected = mapOf("TOGGLE_1" to true, "TOGGLE_2" to false)
            Truth.assertThat(actual).containsExactlyEntriesIn(expected)

            coVerifyOrder { versionProvider.get() }
        }

        @Test
        fun `successfully initialize storage if versionProvider returns null`() = runTest {
            // Arrange
            val appVersion = null
            every { versionProvider.get() } returns appVersion

            // Act
            val actual = ProdFeatureTogglesManager(versionProvider).getProdFeatureToggles()

            // Assert
            val expected = mapOf("TOGGLE_1" to false, "TOGGLE_2" to false)
            Truth.assertThat(actual).containsExactlyEntriesIn(expected)

            coVerifyOrder { versionProvider.get() }
        }

        @Test
        fun `successfully initialize storage if versionProvider returns empty string`() = runTest {
            // Arrange
            val appVersion = ""
            every { versionProvider.get() } returns appVersion

            // Act
            val actual = ProdFeatureTogglesManager(versionProvider).getProdFeatureToggles()

            // Assert
            val expected = mapOf("TOGGLE_1" to false, "TOGGLE_2" to false)
            Truth.assertThat(actual).containsExactlyEntriesIn(expected)

            coVerifyOrder { versionProvider.get() }
        }

        @Test
        fun `failure initialize storage if versionProvider throws exception`() = runTest {
            // Arrange
            val exception = Exception("Test exception")
            every { versionProvider.get() } throws exception

            // Act
            val actual = runCatching { ProdFeatureTogglesManager(versionProvider) }.exceptionOrNull()!!

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
            every { versionProvider.get() } returns "1.0.0"

            val featureToggles = mapOf(
                "INACTIVE_TEST_FEATURE_ENABLED" to "undefined",
                "ACTIVE2_TEST_FEATURE_ENABLED" to "1.0.0",
            )

            mockkObject(FeatureToggles)
            every { FeatureToggles.values } returns featureToggles

            manager = ProdFeatureTogglesManager(versionProvider)
        }

        @AfterAll
        fun tearDownAll() {
            clearMocks(versionProvider)
            unmockkObject(FeatureToggles)
        }

        @ParameterizedTest
        @ProvideTestModels
        fun isFeatureEnabled(model: IsFeatureEnabledModel) {
            // Act
            val actual = manager.isFeatureEnabled(name = model.name)

            // Assert
            val expected = model.expected
            Truth.assertThat(actual).isEqualTo(expected)
        }

        private fun provideTestModels() = listOf(
            IsFeatureEnabledModel(name = "ACTIVE2_TEST_FEATURE_ENABLED", expected = true),
            IsFeatureEnabledModel(name = "INACTIVE_TEST_FEATURE_ENABLED", expected = false),
            IsFeatureEnabledModel(name = "UNKNOWN_FEATURE_ENABLED", expected = false),
            IsFeatureEnabledModel(name = "", expected = false),
        )
    }

    data class IsFeatureEnabledModel(val name: String, val expected: Boolean)
}