package com.tangem.core.configtoggle.blockchain.impl

import com.google.common.truth.Truth
import com.tangem.core.configtoggle.ExcludedBlockchainToggles
import com.tangem.core.configtoggle.version.VersionProvider
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ProdExcludedBlockchainsManagerTest {

    private val versionProvider = mockk<VersionProvider>()

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
            clearMocks(versionProvider)
        }

        @Test
        fun `successfully initialize excluded blockchains`() = runTest {
            // Arrange
            val appVersion = "1.0.0"
            every { versionProvider.get() } returns appVersion

            // Act
            val actual = ProdExcludedBlockchainsManager(versionProvider).excludedBlockchainsIds

            // Assert
            val expected = setOf("CHAIN_2")
            Truth.assertThat(actual).containsExactlyElementsIn(expected)

            coVerifyOrder { versionProvider.get() }
        }

        @Test
        fun `all blockchains excluded if versionProvider returns null`() = runTest {
            // Arrange
            every { versionProvider.get() } returns null

            // Act
            val actual = ProdExcludedBlockchainsManager(versionProvider).excludedBlockchainsIds

            // Assert
            val expected = setOf("CHAIN_1", "CHAIN_2")
            Truth.assertThat(actual).containsExactlyElementsIn(expected)

            coVerifyOrder { versionProvider.get() }
        }

        @Test
        fun `all blockchains excluded if versionProvider returns empty string`() = runTest {
            // Arrange
            every { versionProvider.get() } returns ""

            // Act
            val actual = ProdExcludedBlockchainsManager(versionProvider).excludedBlockchainsIds

            // Assert
            val expected = setOf("CHAIN_1", "CHAIN_2")
            Truth.assertThat(actual).containsExactlyElementsIn(expected)

            coVerifyOrder { versionProvider.get() }
        }

        @Test
        fun `failure initialize if versionProvider throws exception`() = runTest {
            // Arrange
            val exception = Exception("Test exception")
            every { versionProvider.get() } throws exception

            // Act
            val actual = runCatching { ProdExcludedBlockchainsManager(versionProvider) }.exceptionOrNull()!!

            // Assert
            Truth.assertThat(actual).isInstanceOf(exception::class.java)
            Truth.assertThat(actual).hasMessageThat().isEqualTo(exception.message)

            coVerifyOrder { versionProvider.get() }
        }
    }
}