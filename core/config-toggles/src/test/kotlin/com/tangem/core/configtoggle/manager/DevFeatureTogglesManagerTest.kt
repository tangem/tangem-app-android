package com.tangem.core.configtoggle.manager

import com.google.common.truth.Truth
import com.tangem.core.configtoggle.feature.impl.DevFeatureTogglesManager
import com.tangem.core.configtoggle.feature.provider.FeatureTogglesProvider
import com.tangem.core.configtoggle.storage.LocalTogglesStorage
import com.tangem.core.configtoggle.version.VersionProvider
import com.tangem.test.core.ProvideTestModels
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.*
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DevFeatureTogglesManagerTest {

    private val versionProvider = mockk<VersionProvider>()
    private val featureTogglesProvider = mockk<FeatureTogglesProvider>()
    private val featureTogglesLocalStorage = mockk<LocalTogglesStorage>(relaxUnitFun = true)

    private val testToggles = mapOf(
        "ENABLED_TOGGLE" to "1.0.0",
        "DISABLED_TOGGLE" to "undefined",
    )

    private fun getExpectedFileToggles(appVersion: String?): Map<String, Boolean> =
        testToggles.mapValues { (_, version) ->
            version != "undefined" && !appVersion.isNullOrEmpty()
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
            clearMocks(versionProvider, featureTogglesProvider, featureTogglesLocalStorage)
        }

        @Test
        fun `successfully initialize manager`() = runTest {
            // Arrange
            val appVersion = "1.0.0"
            val fileToggles = getExpectedFileToggles(appVersion)
            val savedToggles = fileToggles.mapValues { !it.value }
            every { versionProvider.get() } returns appVersion
            coEvery { featureTogglesLocalStorage.getSyncOrEmpty() } returns savedToggles

            // Act
            val actual = DevFeatureTogglesManager(
                versionProvider,
                featureTogglesProvider,
                featureTogglesLocalStorage,
            ).getFeatureToggles()

            // Assert
            val expected = savedToggles
            Truth.assertThat(actual).containsExactlyEntriesIn(expected)

            coVerifyOrder {
                versionProvider.get()
                featureTogglesLocalStorage.getSyncOrEmpty()
            }
        }

        @Test
        fun `successfully initialize manager if versionProvider returns null`() = runTest {
            // Arrange
            val appVersion: String? = null
            val fileToggles = getExpectedFileToggles(appVersion)
            val savedToggles = fileToggles.mapValues { !it.value }
            every { versionProvider.get() } returns appVersion
            coEvery { featureTogglesLocalStorage.getSyncOrEmpty() } returns savedToggles

            // Act
            val actual = DevFeatureTogglesManager(
                versionProvider,
                featureTogglesProvider,
                featureTogglesLocalStorage,
            ).getFeatureToggles()

            // Assert
            val expected = savedToggles
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
            val fileToggles = getExpectedFileToggles(appVersion)
            val savedToggles = fileToggles.mapValues { !it.value }
            every { versionProvider.get() } returns appVersion
            coEvery { featureTogglesLocalStorage.getSyncOrEmpty() } returns savedToggles

            // Act
            val actual = DevFeatureTogglesManager(
                versionProvider,
                featureTogglesProvider,
                featureTogglesLocalStorage,
            ).getFeatureToggles()

            // Assert
            val expected = savedToggles
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
            val actual = runCatching {
                DevFeatureTogglesManager(
                    versionProvider,
                    featureTogglesProvider,
                    featureTogglesLocalStorage,
                )
            }.exceptionOrNull()!!

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
            every { versionProvider.get() } returns appVersion
            coEvery { featureTogglesLocalStorage.getSyncOrEmpty() } returns emptyMap()

            // Act
            val actual = DevFeatureTogglesManager(
                versionProvider,
                featureTogglesProvider,
                featureTogglesLocalStorage,
            ).getFeatureToggles()

            // Assert
            val expected = getExpectedFileToggles(appVersion)
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
            val fileToggles = getExpectedFileToggles(appVersion)
            val unknownSavedToggles = fileToggles + ("UNKNOWN_TOGGLE" to true)
            every { versionProvider.get() } returns appVersion
            coEvery { featureTogglesLocalStorage.getSyncOrEmpty() } returns unknownSavedToggles

            // Act
            val actual = DevFeatureTogglesManager(
                versionProvider,
                featureTogglesProvider,
                featureTogglesLocalStorage,
            ).getFeatureToggles()

            // Assert
            val expected = fileToggles
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
            val actual = runCatching {
                DevFeatureTogglesManager(
                    versionProvider,
                    featureTogglesProvider,
                    featureTogglesLocalStorage,
                )
            }.exceptionOrNull()!!

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
            every { featureTogglesProvider.getToggles() } returns testToggles
            every { versionProvider.get() } returns "1.0.0"
            val savedToggles = mapOf(
                "DISABLED_TOGGLE" to true,
                "ENABLED_TOGGLE" to false,
            )
            coEvery { featureTogglesLocalStorage.getSyncOrEmpty() } returns savedToggles
            manager = DevFeatureTogglesManager(versionProvider, featureTogglesProvider, featureTogglesLocalStorage)
        }

        @AfterAll
        fun tearDownAll() {
            clearMocks(versionProvider, featureTogglesProvider, featureTogglesLocalStorage)
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
            IsFeatureEnabledModel(name = "ENABLED_TOGGLE", expected = false),
            IsFeatureEnabledModel(name = "DISABLED_TOGGLE", expected = true),
        )
    }

    data class IsFeatureEnabledModel(val name: String, val expected: Boolean)

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class IsMatchLocalConfig {

        @BeforeEach
        fun setupEach() {
            every { featureTogglesProvider.getToggles() } returns testToggles
        }

        @AfterEach
        fun tearDownEach() {
            clearMocks(versionProvider, featureTogglesProvider, featureTogglesLocalStorage)
        }

        @ParameterizedTest
        @ProvideTestModels
        fun isMatchLocalConfig(model: IsMatchLocalConfigModel) = runTest {
            // Arrange
            every { versionProvider.get() } returns "1.0.0"
            coEvery { featureTogglesLocalStorage.getSyncOrEmpty() } returns emptyMap()
            val manager = DevFeatureTogglesManager(
                versionProvider,
                featureTogglesProvider,
                featureTogglesLocalStorage,
            ).apply {
                setFeatureToggles(model.storedFeatureToggles.toMutableMap())
            }

            // Act
            val actual = manager.isMatchLocalConfig()

            // Assert
            Truth.assertThat(actual).isEqualTo(model.expected)

            coVerifyOrder {
                versionProvider.get()
                featureTogglesLocalStorage.getSyncOrEmpty()
            }
        }

        private fun provideTestModels(): List<IsMatchLocalConfigModel> {
            val appVersion = "1.0.0"
            val fileToggles = getExpectedFileToggles(appVersion)
            val firstToggleName = "ENABLED_TOGGLE"

            return listOf(
                IsMatchLocalConfigModel(
                    storedFeatureToggles = fileToggles,
                    expected = true,
                ),
                IsMatchLocalConfigModel(
                    storedFeatureToggles = fileToggles.mapValues { !it.value },
                    expected = false,
                ),
                IsMatchLocalConfigModel(
                    storedFeatureToggles = mapOf(firstToggleName to fileToggles.getValue(firstToggleName)),
                    expected = false,
                ),
                IsMatchLocalConfigModel(
                    storedFeatureToggles = mapOf("UNKNOWN_TOGGLE" to true),
                    expected = false,
                ),
                IsMatchLocalConfigModel(
                    storedFeatureToggles = emptyMap(),
                    expected = false,
                ),
            )
        }
    }

    data class IsMatchLocalConfigModel(
        val storedFeatureToggles: Map<String, Boolean>,
        val expected: Boolean,
    )

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetFeatureToggles {

        @BeforeEach
        fun setupEach() {
            every { featureTogglesProvider.getToggles() } returns testToggles
        }

        @AfterEach
        fun tearDownEach() {
            clearMocks(versionProvider, featureTogglesProvider, featureTogglesLocalStorage)
        }

        @Test
        fun getFeatureToggles() = runTest {
            // Arrange
            val appVersion = "1.0.0"
            val fileToggles = getExpectedFileToggles(appVersion)
            val savedToggles = mapOf(
                "DISABLED_TOGGLE" to true,
                "ENABLED_TOGGLE" to false,
            )
            every { versionProvider.get() } returns appVersion
            coEvery { featureTogglesLocalStorage.getSyncOrEmpty() } returns savedToggles
            val manager = DevFeatureTogglesManager(versionProvider, featureTogglesProvider, featureTogglesLocalStorage)

            // Act
            val actual = manager.getFeatureToggles()

            // Assert
            val expected = fileToggles + savedToggles
            Truth.assertThat(actual).containsExactlyEntriesIn(expected)

            coVerifyOrder {
                versionProvider.get()
                featureTogglesLocalStorage.getSyncOrEmpty()
            }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ChangeToggle {

        @BeforeEach
        fun setupEach() {
            every { featureTogglesProvider.getToggles() } returns testToggles
        }

        @AfterEach
        fun tearDownEach() {
            clearMocks(versionProvider, featureTogglesProvider, featureTogglesLocalStorage)
        }

        @ParameterizedTest
        @ProvideTestModels
        fun changeToggle(model: ChangeToggleModel) = runTest {
            // Arrange
            every { versionProvider.get() } returns "1.0.0"
            coEvery { featureTogglesLocalStorage.getSyncOrEmpty() } returns emptyMap()
            val manager = DevFeatureTogglesManager(versionProvider, featureTogglesProvider, featureTogglesLocalStorage)

            // Act
            manager.changeToggle(name = model.name, isEnabled = model.isEnabled)
            val actual = manager.getFeatureToggles()

            // Assert
            Truth.assertThat(actual).containsExactlyEntriesIn(model.expectedToggles)

            coVerifyOrder {
                versionProvider.get()
                featureTogglesLocalStorage.getSyncOrEmpty()

                if (model.expectedStoreSaving) {
                    featureTogglesLocalStorage.store(model.expectedToggles)
                }
            }
        }

        private fun provideTestModels(): List<ChangeToggleModel> {
            val appVersion = "1.0.0"
            val fileToggles = getExpectedFileToggles(appVersion)
            val enabledName = "ENABLED_TOGGLE"
            val disabledName = "DISABLED_TOGGLE"

            return listOf(
                ChangeToggleModel(
                    name = enabledName,
                    isEnabled = false,
                    expectedToggles = fileToggles + (enabledName to false),
                    expectedStoreSaving = true,
                ),
                ChangeToggleModel(
                    name = disabledName,
                    isEnabled = true,
                    expectedToggles = fileToggles + (disabledName to true),
                    expectedStoreSaving = true,
                ),
                ChangeToggleModel(
                    name = "UNKNOWN_TOGGLE",
                    isEnabled = true,
                    expectedToggles = fileToggles,
                    expectedStoreSaving = false,
                ),
            )
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class RecoverLocalConfig {

        @BeforeEach
        fun setupEach() {
            every { featureTogglesProvider.getToggles() } returns testToggles
        }

        @AfterEach
        fun tearDownEach() {
            clearMocks(versionProvider, featureTogglesProvider, featureTogglesLocalStorage)
        }

        @Test
        fun recoverLocalConfig() = runTest {
            // Arrange
            val appVersion = "1.0.0"
            val savedToggles = mapOf(
                "DISABLED_TOGGLE" to true,
                "ENABLED_TOGGLE" to false,
            )
            every { versionProvider.get() } returns appVersion
            coEvery { featureTogglesLocalStorage.getSyncOrEmpty() } returns savedToggles
            val manager = DevFeatureTogglesManager(versionProvider, featureTogglesProvider, featureTogglesLocalStorage)

            // Act
            manager.recoverLocalConfig()
            val actual = manager.getFeatureToggles()

            // Assert
            val expected = getExpectedFileToggles(appVersion)
            Truth.assertThat(actual).containsExactlyEntriesIn(expected)

            coVerifyOrder {
                versionProvider.get()
                featureTogglesLocalStorage.getSyncOrEmpty()
                featureTogglesLocalStorage.store(expected)
            }
        }
    }

    data class ChangeToggleModel(
        val name: String,
        val isEnabled: Boolean,
        val expectedToggles: Map<String, Boolean>,
        val expectedStoreSaving: Boolean,
    )
}