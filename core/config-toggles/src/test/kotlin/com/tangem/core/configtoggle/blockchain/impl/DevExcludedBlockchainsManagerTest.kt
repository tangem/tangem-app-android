package com.tangem.core.configtoggle.blockchain.impl

import com.google.common.truth.Truth
import com.tangem.core.configtoggle.blockchain.provider.ExcludedBlockchainTogglesProvider
import com.tangem.core.configtoggle.storage.LocalTogglesStorage
import com.tangem.core.configtoggle.version.VersionProvider
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DevExcludedBlockchainsManagerTest {

    private val versionProvider = mockk<VersionProvider>()
    private val excludedBlockchainTogglesProvider = mockk<ExcludedBlockchainTogglesProvider>()
    private val localTogglesStorage = mockk<LocalTogglesStorage>(relaxUnitFun = true)

    private val testVersion = "5.30"

    private val testToggles = mapOf(
        "ENABLED_BLOCKCHAIN" to "5.21.0",
        "DISABLED_BLOCKCHAIN" to "undefined",
        "FUTURE_BLOCKCHAIN" to "6.0.0",
    )

    private fun getExpectedFileToggles(appVersion: String?): Map<String, Boolean> {
        if (appVersion.isNullOrEmpty()) {
            return testToggles.mapValues { false }
        }
        return testToggles.mapValues { (_, version) ->
            version != "undefined" && isVersionSufficient(appVersion, version)
        }
    }

    private fun getExcludedBlockchains(appVersion: String?): Set<String> {
        return getExpectedFileToggles(appVersion).filterValues { !it }.keys
    }

    private fun isVersionSufficient(appVersion: String, requiredVersion: String): Boolean {
        val appParts = appVersion.split(".").mapNotNull { it.toIntOrNull() }
        val reqParts = requiredVersion.split(".").mapNotNull { it.toIntOrNull() }
        for (i in 0 until maxOf(appParts.size, reqParts.size)) {
            val app = appParts.getOrElse(i) { 0 }
            val req = reqParts.getOrElse(i) { 0 }
            if (app > req) return true
            if (app < req) return false
        }
        return true
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Initialization {

        @BeforeEach
        fun setupEach() {
            every { excludedBlockchainTogglesProvider.getToggles() } returns testToggles
        }

        @AfterEach
        fun tearDownEach() {
            clearMocks(versionProvider, excludedBlockchainTogglesProvider, localTogglesStorage)
        }

        @Test
        fun `successfully initialize manager`() = runTest {
            // Arrange
            val appVersion = testVersion
            val fileToggles = getExpectedFileToggles(appVersion)
            val savedToggles = fileToggles.mapValues { !it.value }
            every { versionProvider.get() } returns appVersion
            coEvery { localTogglesStorage.getSyncOrEmpty() } returns savedToggles

            // Act
            val actual = DevExcludedBlockchainsManager(
                versionProvider,
                excludedBlockchainTogglesProvider,
                localTogglesStorage,
            ).excludedBlockchainsIds

            // Assert
            val expected = savedToggles.filterValues { !it }.keys
            Truth.assertThat(actual).containsExactlyElementsIn(expected)

            coVerifyOrder {
                versionProvider.get()
                localTogglesStorage.getSyncOrEmpty()
            }
        }

        @Test
        fun `successfully initialize manager if versionProvider returns null`() = runTest {
            // Arrange
            val appVersion: String? = null
            every { versionProvider.get() } returns appVersion
            coEvery { localTogglesStorage.getSyncOrEmpty() } returns emptyMap()

            // Act
            val actual = DevExcludedBlockchainsManager(
                versionProvider,
                excludedBlockchainTogglesProvider,
                localTogglesStorage,
            ).excludedBlockchainsIds

            // Assert
            val expected = getExcludedBlockchains(appVersion)
            Truth.assertThat(actual).containsExactlyElementsIn(expected)

            coVerifyOrder {
                versionProvider.get()
                localTogglesStorage.getSyncOrEmpty()
            }
        }

        @Test
        fun `successfully initialize manager if storage returns empty map`() = runTest {
            // Arrange
            val appVersion = testVersion
            every { versionProvider.get() } returns appVersion
            coEvery { localTogglesStorage.getSyncOrEmpty() } returns emptyMap()

            // Act
            val actual = DevExcludedBlockchainsManager(
                versionProvider,
                excludedBlockchainTogglesProvider,
                localTogglesStorage,
            ).excludedBlockchainsIds

            // Assert
            val expected = getExcludedBlockchains(appVersion)
            Truth.assertThat(actual).containsExactlyElementsIn(expected)

            coVerifyOrder {
                versionProvider.get()
                localTogglesStorage.getSyncOrEmpty()
            }
        }

        @Test
        fun `failure initialize manager if storage throws exception`() = runTest {
            // Arrange
            val appVersion = testVersion
            val exception = Exception("Test exception")
            every { versionProvider.get() } returns appVersion
            coEvery { localTogglesStorage.getSyncOrEmpty() } throws exception

            // Act
            val actual = runCatching {
                DevExcludedBlockchainsManager(
                    versionProvider,
                    excludedBlockchainTogglesProvider,
                    localTogglesStorage,
                )
            }.exceptionOrNull()!!

            // Assert
            Truth.assertThat(actual).isInstanceOf(exception::class.java)
            Truth.assertThat(actual).hasMessageThat().isEqualTo(exception.message)

            coVerifyOrder {
                versionProvider.get()
                localTogglesStorage.getSyncOrEmpty()
            }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ExcludeBlockchain {

        @BeforeEach
        fun setupEach() {
            every { excludedBlockchainTogglesProvider.getToggles() } returns testToggles
        }

        @AfterEach
        fun tearDownEach() {
            clearMocks(versionProvider, excludedBlockchainTogglesProvider, localTogglesStorage)
        }

        @Test
        fun excludeBlockchain_changesStatusAndSaves() = runTest {
            // Arrange
            every { versionProvider.get() } returns testVersion
            coEvery { localTogglesStorage.getSyncOrEmpty() } returns emptyMap()
            val manager = DevExcludedBlockchainsManager(
                versionProvider,
                excludedBlockchainTogglesProvider,
                localTogglesStorage,
            )

            // Act
            manager.excludeBlockchain("ENABLED_BLOCKCHAIN", false)
            val actual = manager.excludedBlockchainsIds

            // Assert
            Truth.assertThat(actual).contains("ENABLED_BLOCKCHAIN")
            coVerify { localTogglesStorage.store(match { it["ENABLED_BLOCKCHAIN"] == false }) }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class IsMatchLocalConfig {

        @BeforeEach
        fun setupEach() {
            every { excludedBlockchainTogglesProvider.getToggles() } returns testToggles
        }

        @AfterEach
        fun tearDownEach() {
            clearMocks(versionProvider, excludedBlockchainTogglesProvider, localTogglesStorage)
        }

        @Test
        fun isMatchLocalConfig_returnsTrueIfMatchesFile() = runTest {
            // Arrange
            every { versionProvider.get() } returns testVersion
            coEvery { localTogglesStorage.getSyncOrEmpty() } returns emptyMap()
            val manager = DevExcludedBlockchainsManager(
                versionProvider,
                excludedBlockchainTogglesProvider,
                localTogglesStorage,
            )

            // Act
            val actual = manager.isMatchLocalConfig()

            // Assert
            Truth.assertThat(actual).isTrue()
        }

        @Test
        fun isMatchLocalConfig_returnsFalseIfDiffersFromFile() = runTest {
            // Arrange
            every { versionProvider.get() } returns testVersion
            val fileToggles = getExpectedFileToggles(testVersion)
            coEvery { localTogglesStorage.getSyncOrEmpty() } returns emptyMap()
            val manager = DevExcludedBlockchainsManager(
                versionProvider,
                excludedBlockchainTogglesProvider,
                localTogglesStorage,
            )

            // Act
            manager.excludeBlockchain("ENABLED_BLOCKCHAIN", !fileToggles.getValue("ENABLED_BLOCKCHAIN"))
            val actual = manager.isMatchLocalConfig()

            // Assert
            Truth.assertThat(actual).isFalse()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class RecoverLocalConfig {

        @BeforeEach
        fun setupEach() {
            every { excludedBlockchainTogglesProvider.getToggles() } returns testToggles
        }

        @AfterEach
        fun tearDownEach() {
            clearMocks(versionProvider, excludedBlockchainTogglesProvider, localTogglesStorage)
        }

        @Test
        fun recoverLocalConfig_resetsToFileAndSaves() = runTest {
            // Arrange
            val appVersion = testVersion
            every { versionProvider.get() } returns appVersion
            coEvery { localTogglesStorage.getSyncOrEmpty() } returns emptyMap()
            val manager = DevExcludedBlockchainsManager(
                versionProvider,
                excludedBlockchainTogglesProvider,
                localTogglesStorage,
            )
            manager.excludeBlockchain("ENABLED_BLOCKCHAIN", false)

            // Act
            manager.recoverLocalConfig()
            val actual = manager.excludedBlockchainsIds

            // Assert
            val expected = getExcludedBlockchains(appVersion)
            Truth.assertThat(actual).containsExactlyElementsIn(expected)
        }
    }
}