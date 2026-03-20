package com.tangem.core.configtoggle.blockchain.impl

import com.google.common.truth.Truth
import com.tangem.core.configtoggle.blockchain.provider.ExcludedBlockchainTogglesProvider
import com.tangem.core.configtoggle.version.VersionProvider
import io.mockk.clearMocks
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ProdExcludedBlockchainsManagerTest {

    private val versionProvider = mockk<VersionProvider>()
    private val excludedBlockchainTogglesProvider = mockk<ExcludedBlockchainTogglesProvider>()

    private val testVersion = "5.30"

    private val testToggles = mapOf(
        "ENABLED_BLOCKCHAIN" to "5.21.0",
        "DISABLED_BLOCKCHAIN" to "undefined",
        "FUTURE_BLOCKCHAIN" to "6.0.0",
    )

    private fun getExcludedBlockchains(appVersion: String?): Set<String> {
        if (appVersion.isNullOrEmpty()) {
            return testToggles.keys
        }
        return testToggles.filterValues { version ->
            version == "undefined" || !isVersionSufficient(appVersion, version)
        }.keys
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
            clearMocks(versionProvider, excludedBlockchainTogglesProvider)
        }

        @Test
        fun `successfully initialize excluded blockchains`() = runTest {
            // Arrange
            val appVersion = testVersion
            every { versionProvider.get() } returns appVersion

            // Act
            val actual = ProdExcludedBlockchainsManager(
                versionProvider,
                excludedBlockchainTogglesProvider,
            ).excludedBlockchainsIds

            // Assert
            val expected = getExcludedBlockchains(appVersion)
            Truth.assertThat(actual).containsExactlyElementsIn(expected)

            coVerifyOrder { versionProvider.get() }
        }

        @Test
        fun `all blockchains excluded if versionProvider returns null`() = runTest {
            // Arrange
            val appVersion: String? = null
            every { versionProvider.get() } returns appVersion

            // Act
            val actual = ProdExcludedBlockchainsManager(
                versionProvider,
                excludedBlockchainTogglesProvider,
            ).excludedBlockchainsIds

            // Assert
            val expected = getExcludedBlockchains(appVersion)
            Truth.assertThat(actual).containsExactlyElementsIn(expected)

            coVerifyOrder { versionProvider.get() }
        }

        @Test
        fun `all blockchains excluded if versionProvider returns empty string`() = runTest {
            // Arrange
            val appVersion = ""
            every { versionProvider.get() } returns appVersion

            // Act
            val actual = ProdExcludedBlockchainsManager(
                versionProvider,
                excludedBlockchainTogglesProvider,
            ).excludedBlockchainsIds

            // Assert
            val expected = getExcludedBlockchains(appVersion)
            Truth.assertThat(actual).containsExactlyElementsIn(expected)

            coVerifyOrder { versionProvider.get() }
        }

        @Test
        fun `failure initialize if versionProvider throws exception`() = runTest {
            // Arrange
            val exception = Exception("Test exception")
            every { versionProvider.get() } throws exception

            // Act
            val actual = runCatching {
                ProdExcludedBlockchainsManager(versionProvider, excludedBlockchainTogglesProvider)
            }.exceptionOrNull()!!

            // Assert
            Truth.assertThat(actual).isInstanceOf(exception::class.java)
            Truth.assertThat(actual).hasMessageThat().isEqualTo(exception.message)

            coVerifyOrder { versionProvider.get() }
        }
    }
}