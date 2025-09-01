package com.tangem.core.configtoggle.manager

import com.google.common.truth.Truth
import com.tangem.common.test.utils.ProvideTestModels
import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.impl.DevFeatureTogglesManager
import com.tangem.core.configtoggle.manager.ProdFeatureTogglesManagerTest.IsFeatureEnabledModel
import com.tangem.core.configtoggle.storage.FeatureTogglesLocalStorage
import com.tangem.core.configtoggle.version.VersionProvider
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.*
import org.junit.jupiter.params.ParameterizedTest

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DevFeatureTogglesManagerTest {

    private val versionProvider = mockk<VersionProvider>()
    private val featureTogglesLocalStorage = mockk<FeatureTogglesLocalStorage>(relaxUnitFun = true)

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
            clearMocks(versionProvider, featureTogglesLocalStorage)
        }

        @Test
        fun `successfully initialize manager`() = runTest {
            // Arrange
            val appVersion = "1.0.0"
            val savedFeatureToggles = mapOf("TOGGLE_1" to false, "TOGGLE_2" to true)

            every { versionProvider.get() } returns appVersion
            coEvery { featureTogglesLocalStorage.getSyncOrEmpty() } returns savedFeatureToggles

            // Act
            val actual = DevFeatureTogglesManager(versionProvider, featureTogglesLocalStorage).getFeatureToggles()

            // Assert
            val expected = savedFeatureToggles
            Truth.assertThat(actual).containsExactlyEntriesIn(expected)

            coVerifyOrder {
                versionProvider.get()
                featureTogglesLocalStorage.getSyncOrEmpty()
            }
        }

        @Test
        fun `successfully initialize manager if versionProvider returns null`() = runTest {
            // Arrange
            val appVersion = null
            val savedFeatureToggles = mapOf("TOGGLE_1" to false, "TOGGLE_2" to true)

            every { versionProvider.get() } returns appVersion
            coEvery { featureTogglesLocalStorage.getSyncOrEmpty() } returns savedFeatureToggles

            // Act
            val actual = DevFeatureTogglesManager(versionProvider, featureTogglesLocalStorage).getFeatureToggles()

            // Assert
            val expected = savedFeatureToggles
            Truth.assertThat(actual).containsExactlyEntriesIn(expected)

            coVerifyOrder {
                versionProvider.get()
                featureTogglesLocalStorage.getSyncOrEmpty()
            }
        }

        @Test
        fun `successfully initialize manager if versionProvider returns empty string`() = runTest {
            // Arrange
            val appVersion = ""
            val savedFeatureToggles = mapOf("TOGGLE_1" to false, "TOGGLE_2" to true)

            every { versionProvider.get() } returns appVersion
            coEvery { featureTogglesLocalStorage.getSyncOrEmpty() } returns savedFeatureToggles

            // Act
            val actual = DevFeatureTogglesManager(versionProvider, featureTogglesLocalStorage).getFeatureToggles()

            // Assert
            val expected = savedFeatureToggles
            Truth.assertThat(actual).containsExactlyEntriesIn(expected)

            coVerifyOrder {
                versionProvider.get()
                featureTogglesLocalStorage.getSyncOrEmpty()
            }
        }

        @Test
        fun `failure initialize manager if versionProvider throws exception`() = runTest {
            // Arrange
            val exception = Exception("Test exception")
            every { versionProvider.get() } throws exception

            // Act
            val actual = runCatching { DevFeatureTogglesManager(versionProvider, featureTogglesLocalStorage) }
                .exceptionOrNull()!!

            // Assert
            Truth.assertThat(actual).isInstanceOf(exception::class.java)
            Truth.assertThat(actual).hasMessageThat().isEqualTo(exception.message)

            coVerifyOrder { versionProvider.get() }
            coVerify(inverse = true) { featureTogglesLocalStorage.getSyncOrEmpty() }
        }

        @Test
        fun `successfully initialize manager if storage returns empty map`() = runTest {
            // Arrange
            val appVersion = "1.0.0"
            val savedFeatureToggles = emptyMap<String, Boolean>()

            every { versionProvider.get() } returns appVersion
            coEvery { featureTogglesLocalStorage.getSyncOrEmpty() } returns savedFeatureToggles

            // Act
            val actual = DevFeatureTogglesManager(versionProvider, featureTogglesLocalStorage).getFeatureToggles()

            // Assert
            val expected = mapOf("TOGGLE_1" to true, "TOGGLE_2" to false)
            Truth.assertThat(actual).containsExactlyEntriesIn(expected)

            coVerifyOrder {
                versionProvider.get()
                featureTogglesLocalStorage.getSyncOrEmpty()
            }
        }

        @Test
        fun `successfully initialize manager if storage returns unknown toggles`() = runTest {
            // Arrange
            val appVersion = "1.0.0"
            val savedFeatureToggles = mapOf("TOGGLE_3" to true)

            every { versionProvider.get() } returns appVersion
            coEvery { featureTogglesLocalStorage.getSyncOrEmpty() } returns savedFeatureToggles

            // Act
            val actual = DevFeatureTogglesManager(versionProvider, featureTogglesLocalStorage).getFeatureToggles()

            // Assert
            val expected = mapOf("TOGGLE_1" to true, "TOGGLE_2" to false)
            Truth.assertThat(actual).containsExactlyEntriesIn(expected)

            coVerifyOrder {
                versionProvider.get()
                featureTogglesLocalStorage.getSyncOrEmpty()
            }
        }

        @Test
        fun `failure initialize manager if storage throws exception`() = runTest {
            // Arrange
            val appVersion = "1.0.0"
            val exception = Exception("Test exception")

            every { versionProvider.get() } returns appVersion
            coEvery { featureTogglesLocalStorage.getSyncOrEmpty() } throws exception

            // Act
            val actual = runCatching { DevFeatureTogglesManager(versionProvider, featureTogglesLocalStorage) }
                .exceptionOrNull()!!

            // Assert
            Truth.assertThat(actual).isInstanceOf(exception::class.java)
            Truth.assertThat(actual).hasMessageThat().isEqualTo(exception.message)

            coVerifyOrder {
                versionProvider.get()
                featureTogglesLocalStorage.getSyncOrEmpty()
            }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class IsFeatureEnabled {

        private lateinit var manager: DevFeatureTogglesManager

        @BeforeAll
        fun setupAll() {
            every { versionProvider.get() } returns "1.0.0"

            val featureToggles = mapOf(
                "INACTIVE_TEST_FEATURE_ENABLED" to "undefined",
                "ACTIVE2_TEST_FEATURE_ENABLED" to "1.0.0",
            )

            mockkObject(FeatureToggles)
            every { FeatureToggles.values } returns featureToggles

            val savedFeatureToggles = mapOf(
                "INACTIVE_TEST_FEATURE_ENABLED" to true,
                "ACTIVE2_TEST_FEATURE_ENABLED" to false,
            )
            coEvery { featureTogglesLocalStorage.getSyncOrEmpty() } returns savedFeatureToggles

            manager = DevFeatureTogglesManager(versionProvider, featureTogglesLocalStorage)
        }

        @AfterAll
        fun tearDownAll() {
            clearMocks(versionProvider, featureTogglesLocalStorage)
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
            IsFeatureEnabledModel(name = "ACTIVE2_TEST_FEATURE_ENABLED", expected = false),
            IsFeatureEnabledModel(name = "INACTIVE_TEST_FEATURE_ENABLED", expected = true),
            IsFeatureEnabledModel(name = "UNKNOWN_FEATURE_ENABLED", expected = false),
            IsFeatureEnabledModel(name = "", expected = false),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class IsMatchLocalConfig {

        @ParameterizedTest
        @ProvideTestModels
        fun isMatchLocalConfig(model: IsMatchLocalConfigModel) = runTest {
            // Arrange
            every { versionProvider.get() } returns "1.0.0"

            mockkObject(FeatureToggles)
            every { FeatureToggles.values } returns model.fileFeatureToggles
            coEvery { featureTogglesLocalStorage.getSyncOrEmpty() } returns emptyMap()

            val manager = DevFeatureTogglesManager(versionProvider, featureTogglesLocalStorage).apply {
                setFeatureToggles(model.storedFeatureToggles.toMutableMap())
            }

            // Act
            val actual = manager.isMatchLocalConfig()

            // Assert
            val expected = model.expected
            Truth.assertThat(actual).isEqualTo(expected)

            coVerifyOrder {
                versionProvider.get()
                featureTogglesLocalStorage.getSyncOrEmpty()
            }

            clearMocks(versionProvider, featureTogglesLocalStorage)
            unmockkObject(FeatureToggles)
        }

        private fun provideTestModels() = listOf(
            IsMatchLocalConfigModel(
                fileFeatureToggles = mapOf("TOGGLE_1" to "1.0.0", "TOGGLE_2" to "2.0.0"),
                storedFeatureToggles = mapOf("TOGGLE_1" to true, "TOGGLE_2" to false),
                expected = true,
            ),
            IsMatchLocalConfigModel(
                fileFeatureToggles = mapOf("TOGGLE_1" to "1.0.0", "TOGGLE_2" to "2.0.0"),
                storedFeatureToggles = mapOf("TOGGLE_1" to true, "TOGGLE_2" to true),
                expected = false,
            ),
            IsMatchLocalConfigModel(
                fileFeatureToggles = mapOf("TOGGLE_1" to "1.0.0", "TOGGLE_2" to "2.0.0"),
                storedFeatureToggles = mapOf("TOGGLE_1" to true),
                expected = false,
            ),
            IsMatchLocalConfigModel(
                fileFeatureToggles = mapOf("TOGGLE_1" to "1.0.0", "TOGGLE_2" to "2.0.0"),
                storedFeatureToggles = mapOf("TOGGLE_3" to true, "TOGGLE_4" to false),
                expected = false,
            ),
            IsMatchLocalConfigModel(
                fileFeatureToggles = emptyMap(),
                storedFeatureToggles = emptyMap(),
                expected = true,
            ),
            IsMatchLocalConfigModel(
                fileFeatureToggles = emptyMap(),
                storedFeatureToggles = mapOf("TOGGLE_1" to true),
                expected = false,
            ),
        )
    }

    data class IsMatchLocalConfigModel(
        val fileFeatureToggles: Map<String, String>,
        val storedFeatureToggles: Map<String, Boolean>,
        val expected: Boolean,
    )

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetFeatureToggles {

        @Test
        fun getFeatureToggles() = runTest {
            // Arrange
            every { versionProvider.get() } returns "1.0.0"

            val fileFeatureToggles = mapOf(
                "INACTIVE_TEST_FEATURE_ENABLED" to "undefined",
                "ACTIVE2_TEST_FEATURE_ENABLED" to "1.0.0",
            )

            mockkObject(FeatureToggles)
            every { FeatureToggles.values } returns fileFeatureToggles

            val savedFeatureToggles = mapOf(
                "INACTIVE_TEST_FEATURE_ENABLED" to true,
                "ACTIVE2_TEST_FEATURE_ENABLED" to false,
            )
            coEvery { featureTogglesLocalStorage.getSyncOrEmpty() } returns savedFeatureToggles

            val manager = DevFeatureTogglesManager(versionProvider, featureTogglesLocalStorage)

            // Act
            val actual = manager.getFeatureToggles()

            // Assert
            val expected = savedFeatureToggles
            Truth.assertThat(actual).containsExactlyEntriesIn(expected)

            coVerifyOrder {
                versionProvider.get()
                featureTogglesLocalStorage.getSyncOrEmpty()
            }

            clearMocks(versionProvider, featureTogglesLocalStorage)
            unmockkObject(FeatureToggles)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ChangeToggle {

        @ParameterizedTest
        @ProvideTestModels
        fun changeToggle(model: ChangeToggleModel) = runTest {
            // Arrange
            every { versionProvider.get() } returns "1.0.0"

            mockkObject(FeatureToggles)
            every { FeatureToggles.values } returns model.initialToggles
            coEvery { featureTogglesLocalStorage.getSyncOrEmpty() } returns emptyMap()

            val manager = DevFeatureTogglesManager(versionProvider, featureTogglesLocalStorage)

            // Act
            manager.changeToggle(name = model.name, isEnabled = model.isEnabled)
            val actual = manager.getFeatureToggles()

            // Assert
            val expected = model.expectedToggles
            Truth.assertThat(actual).containsExactlyEntriesIn(expected)

            coVerifyOrder {
                versionProvider.get()
                featureTogglesLocalStorage.getSyncOrEmpty()

                if (model.expectedStoreSaving) {
                    featureTogglesLocalStorage.store(model.expectedToggles)
                }
            }

            clearMocks(versionProvider, featureTogglesLocalStorage)
            unmockkObject(FeatureToggles)
        }

        private fun provideTestModels() = listOf(
            ChangeToggleModel(
                initialToggles = mapOf("TOGGLE_1" to "1.0.0", "TOGGLE_2" to "undefined"),
                name = "TOGGLE_1",
                isEnabled = false,
                expectedToggles = mapOf("TOGGLE_1" to false, "TOGGLE_2" to false),
                expectedStoreSaving = true,
            ),
            ChangeToggleModel(
                initialToggles = mapOf("TOGGLE_1" to "1.0.0", "TOGGLE_2" to "undefined"),
                name = "TOGGLE_2",
                isEnabled = true,
                expectedToggles = mapOf("TOGGLE_1" to true, "TOGGLE_2" to true),
                expectedStoreSaving = true,
            ),
            ChangeToggleModel(
                initialToggles = mapOf("TOGGLE_1" to "1.0.0", "TOGGLE_2" to "undefined"),
                name = "TOGGLE_3",
                isEnabled = true,
                expectedToggles = mapOf("TOGGLE_1" to true, "TOGGLE_2" to false),
                expectedStoreSaving = false,
            ),
            ChangeToggleModel(
                initialToggles = emptyMap(),
                name = "TOGGLE_1",
                isEnabled = true,
                expectedToggles = emptyMap(),
                expectedStoreSaving = false,
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class RecoverLocalConfig {

        @Test
        fun recoverLocalConfig() = runTest {
            // Arrange
            every { versionProvider.get() } returns "1.0.0"

            val fileFeatureToggles = mapOf(
                "INACTIVE_TEST_FEATURE_ENABLED" to "undefined",
                "ACTIVE2_TEST_FEATURE_ENABLED" to "1.0.0",
            )

            mockkObject(FeatureToggles)
            every { FeatureToggles.values } returns fileFeatureToggles

            val savedFeatureToggles = mapOf(
                "INACTIVE_TEST_FEATURE_ENABLED" to true,
                "ACTIVE2_TEST_FEATURE_ENABLED" to false,
            )
            coEvery { featureTogglesLocalStorage.getSyncOrEmpty() } returns savedFeatureToggles

            val manager = DevFeatureTogglesManager(versionProvider, featureTogglesLocalStorage)

            // Act
            manager.recoverLocalConfig()
            val actual = manager.getFeatureToggles()

            // Assert
            val expected = mapOf(
                "INACTIVE_TEST_FEATURE_ENABLED" to false,
                "ACTIVE2_TEST_FEATURE_ENABLED" to true,
            )
            Truth.assertThat(actual).containsExactlyEntriesIn(expected)

            coVerifyOrder {
                versionProvider.get()
                featureTogglesLocalStorage.getSyncOrEmpty()
                featureTogglesLocalStorage.store(expected)
            }

            clearMocks(versionProvider, featureTogglesLocalStorage)
            unmockkObject(FeatureToggles)
        }
    }

    data class ChangeToggleModel(
        val initialToggles: Map<String, String>,
        val name: String,
        val isEnabled: Boolean,
        val expectedToggles: Map<String, Boolean>,
        val expectedStoreSaving: Boolean,
    )
}