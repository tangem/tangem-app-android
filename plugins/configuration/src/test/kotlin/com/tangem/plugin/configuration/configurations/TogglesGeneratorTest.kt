package com.tangem.plugin.configuration.configurations

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Tests for [TogglesGenerator].
 */
class TogglesGeneratorTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var outputDir: File

    @BeforeEach
    fun setup() {
        outputDir = File(tempDir, "output")
    }

    @Test
    fun `generate creates enum class with correct package`() {
        // Arrange & Act
        val code = generateAndReadOutput(
            json = SINGLE_ENTRY_JSON,
            fileName = "FeatureToggles",
        )

        // Assert
        assertThat(code).contains("package com.tangem.core.configtoggle")
    }

    @Test
    fun `generate creates enum class with given name`() {
        // Arrange & Act
        val code = generateAndReadOutput(
            json = SINGLE_ENTRY_JSON,
            fileName = "FeatureToggles",
        )

        // Assert
        assertThat(code).contains("enum class FeatureToggles {")
    }

    @Test
    fun `generate creates enum entries and companion object`() {
        // Arrange & Act
        val code = generateAndReadOutput(
            json = SINGLE_ENTRY_JSON,
            fileName = "FeatureToggles",
        )

        // Assert
        assertThat(code).contains("STAKING_ETH_ENABLED,")
        assertThat(code).contains("companion object {")
    }

    @Test
    fun `generate creates values map with string literal keys`() {
        // Arrange & Act
        val code = generateAndReadOutput(
            json = SINGLE_ENTRY_JSON,
            fileName = "FeatureToggles",
        )

        // Assert
        assertThat(code).contains("val values: Map<String, String> = mapOf(")
        assertThat(code).contains(""""STAKING_ETH_ENABLED" to "undefined"""")
    }

    @Test
    fun `generate creates enum with multiple entries`() {
        // Arrange
        val json = """
            [
                { "name": "FEATURE_A", "version": "1.0" },
                { "name": "FEATURE_B", "version": "2.0" },
                { "name": "FEATURE_C", "version": "undefined" }
            ]
        """.trimIndent()

        // Act
        val code = generateAndReadOutput(json, "TestToggles")

        // Assert
        assertThat(code).contains("FEATURE_A,")
        assertThat(code).contains("FEATURE_B,")
        assertThat(code).contains("FEATURE_C,")
        assertThat(code).contains(""""FEATURE_A" to "1.0"""")
        assertThat(code).contains(""""FEATURE_B" to "2.0"""")
        assertThat(code).contains(""""FEATURE_C" to "undefined"""")
    }

    @Test
    fun `generate handles empty array`() {
        // Arrange & Act
        val code = generateAndReadOutput(
            json = "[]",
            fileName = "EmptyToggles",
        )

        // Assert
        assertThat(code).contains("enum class EmptyToggles {")
        assertThat(code).contains("val values: Map<String, String> = mapOf(")
    }

    @Test
    fun `generate converts slash in name to underscore in enum`() {
        // Arrange
        val json = """[{ "name": "NEXA/test", "version": "undefined" }]"""

        // Act
        val code = generateAndReadOutput(json, "Toggles")

        // Assert
        assertThat(code).contains("NEXA_TEST,")
        assertThat(code).contains(""""NEXA/test" to "undefined"""")
    }

    @Test
    fun `generate converts dash in name to underscore in enum`() {
        // Arrange
        val json = """[{ "name": "vanar-chain", "version": "undefined" }]"""

        // Act
        val code = generateAndReadOutput(json, "Toggles")

        // Assert
        assertThat(code).contains("VANAR_CHAIN,")
        assertThat(code).contains(""""vanar-chain" to "undefined"""")
    }

    @Test
    fun `generate uppercases lowercase names in enum`() {
        // Arrange
        val json = """[{ "name": "sonic", "version": "5.21.0" }]"""

        // Act
        val code = generateAndReadOutput(json, "Toggles")

        // Assert
        assertThat(code).contains("SONIC,")
        assertThat(code).contains(""""sonic" to "5.21.0"""")
    }

    @Test
    fun `generate adds kdoc with source file reference`() {
        // Arrange & Act
        val code = generateAndReadOutput(
            json = SINGLE_ENTRY_JSON,
            fileName = "FeatureToggles",
        )

        // Assert
        assertThat(code).contains("Generated from")
        assertThat(code).contains("Auto-generated - do not edit manually")
    }

    @Test
    fun `generate removes public modifiers`() {
        // Arrange & Act
        val code = generateAndReadOutput(
            json = SINGLE_ENTRY_JSON,
            fileName = "FeatureToggles",
        )

        // Assert
        assertThat(code).doesNotContain("public enum class")
        assertThat(code).doesNotContain("public companion object")
        assertThat(code).doesNotContain("public val")
    }

    @Test
    fun `generate handles real feature toggles config`() {
        // Arrange
        val json = """
            [
                { "name": "NEW_CARD_SCANNING_ENABLED", "version": "undefined" },
                { "name": "HOT_WALLET_CREATION_RESTRICTION_ENABLED", "version": "5.32.0" },
                { "name": "SWAP_MARKET_LIST_ENABLED", "version": "5.34" }
            ]
        """.trimIndent()

        // Act
        val code = generateAndReadOutput(json, "FeatureToggles")

        // Assert
        assertThat(code).contains("NEW_CARD_SCANNING_ENABLED,")
        assertThat(code).contains("HOT_WALLET_CREATION_RESTRICTION_ENABLED,")
        assertThat(code).contains("SWAP_MARKET_LIST_ENABLED,")
        assertThat(code).contains(""""NEW_CARD_SCANNING_ENABLED" to "undefined"""")
        assertThat(code).contains(""""HOT_WALLET_CREATION_RESTRICTION_ENABLED" to "5.32.0"""")
        assertThat(code).contains(""""SWAP_MARKET_LIST_ENABLED" to "5.34"""")
    }

    @Test
    fun `generate handles real excluded blockchains config`() {
        // Arrange
        val json = """
            [
                { "name": "NEXA", "version": "undefined" },
                { "name": "NEXA/test", "version": "undefined" },
                { "name": "sonic", "version": "5.21.0" }
            ]
        """.trimIndent()

        // Act
        val code = generateAndReadOutput(json, "ExcludedBlockchainToggles")

        // Assert
        assertThat(code).contains("enum class ExcludedBlockchainToggles {")
        assertThat(code).contains("NEXA,")
        assertThat(code).contains("NEXA_TEST,")
        assertThat(code).contains("SONIC,")
        assertThat(code).contains(""""NEXA" to "undefined"""")
        assertThat(code).contains(""""NEXA/test" to "undefined"""")
        assertThat(code).contains(""""sonic" to "5.21.0"""")
    }

    @Test
    fun `generate produces different objects for different file names`() {
        // Arrange
        val json = SINGLE_ENTRY_JSON

        // Act
        val code1 = generateAndReadOutput(json, "FeatureToggles")
        val code2 = generateAndReadOutput(json, "ExcludedBlockchainToggles")

        // Assert
        assertThat(code1).contains("enum class FeatureToggles {")
        assertThat(code2).contains("enum class ExcludedBlockchainToggles {")
    }

    private fun generateAndReadOutput(json: String, fileName: String): String {
        val inputFile = File(tempDir, "config.json").apply {
            writeText(json)
        }

        TogglesGenerator.generate(inputFile, outputDir, fileName)

        val generatedFile = File(outputDir, "com/tangem/core/configtoggle/$fileName.kt")

        assertThat(generatedFile.exists()).isTrue()
        return generatedFile.readText()
    }

    private companion object {
        const val SINGLE_ENTRY_JSON = """[{ "name": "STAKING_ETH_ENABLED", "version": "undefined" }]"""
    }
}