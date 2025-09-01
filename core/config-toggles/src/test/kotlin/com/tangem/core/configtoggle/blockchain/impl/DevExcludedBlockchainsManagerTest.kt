package com.tangem.core.configtoggle.blockchain.impl

import com.google.common.truth.Truth
import com.tangem.core.configtoggle.ExcludedBlockchainToggles
import com.tangem.core.configtoggle.storage.LocalTogglesStorage
import com.tangem.core.configtoggle.version.VersionProvider
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DevExcludedBlockchainsManagerTest {

    private val versionProvider = mockk<VersionProvider>()
    private val localTogglesStorage = mockk<LocalTogglesStorage>(relaxUnitFun = true)

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Initialization {

        @BeforeAll
        fun setupAll() {
            val toggles = mapOf("CHAIN_1" to "1.0.0", "CHAIN_2" to "2.0.0")
            mockkObject(ExcludedBlockchainToggles)
            every { ExcludedBlockchainToggles.values } returns toggles
        }

        @AfterAll
        fun tearDownAll() {
            unmockkObject(ExcludedBlockchainToggles)
        }

        @AfterEach
        fun tearDownEach() {
            clearMocks(versionProvider, localTogglesStorage)
        }

        @Test
        fun `successfully initialize manager`() = runTest {
            // Arrange
            val appVersion = "1.0.0"
            val savedToggles = mapOf("CHAIN_1" to false, "CHAIN_2" to true)
            every { versionProvider.get() } returns appVersion
            coEvery { localTogglesStorage.getSyncOrEmpty() } returns savedToggles

            // Act
            val actual = DevExcludedBlockchainsManager(versionProvider, localTogglesStorage).excludedBlockchainsIds

            // Assert
            val expected = setOf("CHAIN_1")
            Truth.assertThat(actual).containsExactlyElementsIn(expected)

            coVerifyOrder {
                versionProvider.get()
                localTogglesStorage.getSyncOrEmpty()
            }
        }

        @Test
        fun `successfully initialize manager if versionProvider returns null`() = runTest {
            // Arrange
            every { versionProvider.get() } returns null
            coEvery { localTogglesStorage.getSyncOrEmpty() } returns emptyMap()

            // Act
            val actual = DevExcludedBlockchainsManager(versionProvider, localTogglesStorage).excludedBlockchainsIds

            // Assert
            Truth.assertThat(actual).containsExactly("CHAIN_1", "CHAIN_2")

            coVerifyOrder {
                versionProvider.get()
                localTogglesStorage.getSyncOrEmpty()
            }
        }

        @Test
        fun `successfully initialize manager if storage returns empty map`() = runTest {
            // Arrange
            every { versionProvider.get() } returns "1.0.0"
            coEvery { localTogglesStorage.getSyncOrEmpty() } returns emptyMap()

            // Act
            val actual = DevExcludedBlockchainsManager(versionProvider, localTogglesStorage).excludedBlockchainsIds

            // Assert
            Truth.assertThat(actual).containsExactly("CHAIN_2")

            coVerifyOrder {
                versionProvider.get()
                localTogglesStorage.getSyncOrEmpty()
            }
        }

        @Test
        fun `failure initialize manager if storage throws exception`() = runTest {
            // Arrange
            every { versionProvider.get() } returns "1.0.0"
            val exception = Exception("Test exception")
            coEvery { localTogglesStorage.getSyncOrEmpty() } throws exception

            // Act
            val actual = runCatching { DevExcludedBlockchainsManager(versionProvider, localTogglesStorage) }
                .exceptionOrNull()!!

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

        @Test
        fun excludeBlockchain_changesStatusAndSaves() = runTest {
            // Arrange
            every { versionProvider.get() } returns "1.0.0"
            coEvery { localTogglesStorage.getSyncOrEmpty() } returns emptyMap()

            val manager = DevExcludedBlockchainsManager(versionProvider, localTogglesStorage)
            manager.excludeBlockchain("CHAIN_1", false)

            // Act
            val actual = manager.excludedBlockchainsIds

            // Assert
            Truth.assertThat(actual).contains("CHAIN_1")
            coVerify { localTogglesStorage.store(match { it["CHAIN_1"] == false }) }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class IsMatchLocalConfig {

        @Test
        fun isMatchLocalConfig_returnsTrueIfMatchesFile() = runTest {
            // Arrange
            every { versionProvider.get() } returns "1.0.0"
            coEvery { localTogglesStorage.getSyncOrEmpty() } returns emptyMap()

            val manager = DevExcludedBlockchainsManager(versionProvider, localTogglesStorage)

            // Act
            val actual = manager.isMatchLocalConfig()

            // Assert
            Truth.assertThat(actual).isTrue()
        }

        @Test
        fun isMatchLocalConfig_returnsFalseIfDiffersFromFile() = runTest {
            // Arrange
            every { versionProvider.get() } returns "1.0.0"
            coEvery { localTogglesStorage.getSyncOrEmpty() } returns emptyMap()

            val manager = DevExcludedBlockchainsManager(versionProvider, localTogglesStorage)
            manager.excludeBlockchain("CHAIN_1", false)

            // Act
            val actual = manager.isMatchLocalConfig()

            // Assert
            Truth.assertThat(actual).isFalse()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class RecoverLocalConfig {

        @Test
        fun recoverLocalConfig_resetsToFileAndSaves() = runTest {
            // Arrange
            every { versionProvider.get() } returns "1.0.0"

            val toggles = mapOf("CHAIN_1" to "2.0.0", "CHAIN_2" to "2.0.0")
            mockkObject(ExcludedBlockchainToggles)
            every { ExcludedBlockchainToggles.values } returns toggles

            coEvery { localTogglesStorage.getSyncOrEmpty() } returns mapOf("CHAIN_1" to true)

            val manager = DevExcludedBlockchainsManager(versionProvider, localTogglesStorage)
            manager.recoverLocalConfig()

            // Act
            val actual = manager.excludedBlockchainsIds

            // Assert
            Truth.assertThat(actual).containsExactly("CHAIN_1", "CHAIN_2")

            unmockkObject(ExcludedBlockchainToggles)
            clearMocks(versionProvider, localTogglesStorage)
        }
    }
}