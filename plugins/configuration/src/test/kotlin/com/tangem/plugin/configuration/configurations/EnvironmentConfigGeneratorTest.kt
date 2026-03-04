package com.tangem.plugin.configuration.configurations

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Tests for [EnvironmentConfigGenerator] covering JSON parsing edge cases.
 */
class EnvironmentConfigGeneratorTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var outputDir: File

    @BeforeEach
    fun setup() {
        outputDir = File(tempDir, "output")
    }

    @Test
    fun `generate handles string values correctly`() {
        // Arrange
        val json = """
            {
                "apiKey": "test-api-key",
                "baseUrl": "https://example.com"
            }
        """.trimIndent()

        // Act
        val generatedCode = generateAndReadOutput(json)

        // Assert
        assertThat(generatedCode).contains("""const val apiKey: String = "test-api-key"""")
        assertThat(generatedCode).contains("""const val baseUrl: String = "https://example.com"""")
    }

    @Test
    fun `generate handles empty string as nullable`() {
        // Arrange
        val json = """
            {
                "emptyValue": ""
            }
        """.trimIndent()

        // Act
        val generatedCode = generateAndReadOutput(json)

        // Assert
        assertThat(generatedCode).contains("val emptyValue: String? = null")
    }

    @Test
    fun `generate handles null values`() {
        // Arrange
        val json = """
            {
                "nullValue": null
            }
        """.trimIndent()

        // Act
        val generatedCode = generateAndReadOutput(json)

        // Assert
        assertThat(generatedCode).contains("val nullValue: String? = null")
    }

    @Test
    fun `generate handles boolean values`() {
        // Arrange
        val json = """
            {
                "isEnabled": true,
                "isDisabled": false
            }
        """.trimIndent()

        // Act
        val generatedCode = generateAndReadOutput(json)

        // Assert
        assertThat(generatedCode).contains("const val isEnabled: Boolean = true")
        assertThat(generatedCode).contains("const val isDisabled: Boolean = false")
    }

    @Test
    fun `generate handles integer values as Long`() {
        // Arrange
        val json = """
            {
                "count": 42,
                "negativeNumber": -100,
                "largeNumber": 9223372036854775807
            }
        """.trimIndent()

        // Act
        val generatedCode = generateAndReadOutput(json)

        // Assert
        assertThat(generatedCode).contains("const val count: Long = 42")
        assertThat(generatedCode).contains("const val negativeNumber: Long = -100")
        // KotlinPoet formats large numbers with underscores
        assertThat(generatedCode).contains("const val largeNumber: Long = 9_223_372_036_854_775_807")
    }

    @Test
    fun `generate handles double values`() {
        // Arrange
        val json = """
            {
                "ratio": 3.14,
                "negativeDouble": -2.5
            }
        """.trimIndent()

        // Act
        val generatedCode = generateAndReadOutput(json)

        // Assert
        assertThat(generatedCode).contains("const val ratio: Double = 3.14")
        assertThat(generatedCode).contains("const val negativeDouble: Double = -2.5")
    }

    @Test
    fun `generate handles string arrays`() {
        // Arrange
        val json = """
            {
                "items": ["one", "two", "three"]
            }
        """.trimIndent()

        // Act
        val generatedCode = generateAndReadOutput(json)

        // Assert
        assertThat(generatedCode).contains("val items: List<String> = listOf(")
        assertThat(generatedCode).contains(""""one",""")
        assertThat(generatedCode).contains(""""two",""")
        assertThat(generatedCode).contains(""""three",""")
    }

    @Test
    fun `generate handles empty arrays`() {
        // Arrange
        val json = """
            {
                "emptyList": []
            }
        """.trimIndent()

        // Act
        val generatedCode = generateAndReadOutput(json)

        // Assert
        assertThat(generatedCode).contains("val emptyList: List<String> = listOf(")
    }

    @Test
    fun `generate handles nested objects`() {
        // Arrange
        val json = """
            {
                "database": {
                    "host": "localhost",
                    "port": 5432
                }
            }
        """.trimIndent()

        // Act
        val generatedCode = generateAndReadOutput(json)

        // Assert
        assertThat(generatedCode).contains("object Database {")
        assertThat(generatedCode).contains("""const val host: String = "localhost"""")
        // KotlinPoet formats numbers >= 1000 with underscores
        assertThat(generatedCode).contains("const val port: Long = 5_432")
    }

    @Test
    fun `generate handles deeply nested objects`() {
        // Arrange
        val json = """
            {
                "level1": {
                    "level2": {
                        "level3": {
                            "deepValue": "deep"
                        }
                    }
                }
            }
        """.trimIndent()

        // Act
        val generatedCode = generateAndReadOutput(json)

        // Assert
        assertThat(generatedCode).contains("object Level1 {")
        assertThat(generatedCode).contains("object Level2 {")
        assertThat(generatedCode).contains("object Level3 {")
        assertThat(generatedCode).contains("""const val deepValue: String = "deep"""")
    }

    @Test
    fun `generate preserves camelCase object names without separators`() {
        // Arrange - object names like "AppsFlyer" should stay as "AppsFlyer", not become "Appsflyer"
        val json = """
            {
                "AppsFlyer": {
                    "DevKey": "key123"
                },
                "GetBlockAccessTokens": {
                    "ethereum": {
                        "jsonRpc": "token"
                    }
                }
            }
        """.trimIndent()

        // Act
        val generatedCode = generateAndReadOutput(json)

        // Assert - object names preserved, property names have first letter lowercased
        assertThat(generatedCode).contains("object AppsFlyer {")
        assertThat(generatedCode).contains("""const val devKey: String = "key123"""")
        assertThat(generatedCode).contains("object GetBlockAccessTokens {")
        assertThat(generatedCode).contains("object Ethereum {")
    }

    @Test
    fun `generate converts dash-separated names to PascalCase`() {
        // Arrange
        val json = """
            {
                "cosmos-hub": {
                    "chainId": "cosmoshub-4"
                }
            }
        """.trimIndent()

        // Act
        val generatedCode = generateAndReadOutput(json)

        // Assert
        assertThat(generatedCode).contains("object CosmosHub {")
    }

    @Test
    fun `generate converts underscore-separated names to PascalCase`() {
        // Arrange
        val json = """
            {
                "api_config": {
                    "timeout": 30
                }
            }
        """.trimIndent()

        // Act
        val generatedCode = generateAndReadOutput(json)

        // Assert
        assertThat(generatedCode).contains("object ApiConfig {")
    }

    @Test
    fun `generate handles consecutive dashes in names`() {
        // Arrange
        val json = """
            {
                "cosmos--hub": {
                    "testValue": "test"
                }
            }
        """.trimIndent()

        // Act
        val generatedCode = generateAndReadOutput(json)

        // Assert
        assertThat(generatedCode).contains("object CosmosHub {")
    }

    @Test
    fun `generate handles trailing dash in names`() {
        // Arrange
        val json = """
            {
                "config-": {
                    "testValue": "test"
                }
            }
        """.trimIndent()

        // Act
        val generatedCode = generateAndReadOutput(json)

        // Assert
        assertThat(generatedCode).contains("object Config {")
    }

    @Test
    fun `generate handles special characters in string values`() {
        // Arrange
        val json = """
            {
                "query": "SELECT * FROM users WHERE name = 'John'",
                "path": "C:\\Users\\test",
                "newline": "line1\nline2",
                "unicode": "Hello 世界"
            }
        """.trimIndent()

        // Act
        val generatedCode = generateAndReadOutput(json)

        // Assert
        assertThat(generatedCode).contains("const val query: String")
        assertThat(generatedCode).contains("const val path: String")
        assertThat(generatedCode).contains("const val newline: String")
        assertThat(generatedCode).contains("const val unicode: String")
    }

    @Test
    fun `generate handles arrays with special characters`() {
        // Arrange
        val json = """
            {
                "urls": [
                    "https://api.example.com/v1",
                    "https://api.example.com/v2?key=value&other=1"
                ]
            }
        """.trimIndent()

        // Act
        val generatedCode = generateAndReadOutput(json)

        // Assert
        assertThat(generatedCode).contains("val urls: List<String> = listOf(")
        assertThat(generatedCode).contains(""""https://api.example.com/v1",""")
    }

    @Test
    fun `generate adds file suppress annotations`() {
        // Arrange
        val json = """
            {
                "key": "value"
            }
        """.trimIndent()

        // Act
        val generatedCode = generateAndReadOutput(json)

        // Assert
        assertThat(generatedCode).contains("@file:Suppress(")
        assertThat(generatedCode).contains(""""MaximumLineLength"""")
        assertThat(generatedCode).contains(""""MaxLineLength"""")
        assertThat(generatedCode).contains(""""Indentation"""")
    }

    @Test
    fun `generate creates proper package declaration`() {
        // Arrange
        val json = """
            {
                "key": "value"
            }
        """.trimIndent()

        // Act
        val generatedCode = generateAndReadOutput(json)

        // Assert
        assertThat(generatedCode).contains("package com.tangem.datasource.local.config.environment.generated")
    }

    @Test
    fun `generate creates object with correct name`() {
        // Arrange
        val json = """
            {
                "key": "value"
            }
        """.trimIndent()

        // Act
        val generatedCode = generateAndReadOutput(json)

        // Assert
        assertThat(generatedCode).contains("object GeneratedEnvironmentConfig {")
    }

    @Test
    fun `generate adds kdoc with source file reference`() {
        // Arrange
        val json = """
            {
                "key": "value"
            }
        """.trimIndent()

        // Act
        val generatedCode = generateAndReadOutput(json)

        // Assert
        assertThat(generatedCode).contains("Generated from")
        assertThat(generatedCode).contains("Auto-generated - do not edit manually")
    }

    @Test
    fun `generate removes public modifiers`() {
        // Arrange
        val json = """
            {
                "key": "value"
            }
        """.trimIndent()

        // Act
        val generatedCode = generateAndReadOutput(json)

        // Assert
        assertThat(generatedCode).doesNotContain("public object")
        assertThat(generatedCode).doesNotContain("public val")
        assertThat(generatedCode).doesNotContain("public const val")
    }

    @Test
    fun `generate handles complex real-world config`() {
        // Arrange
        val json = """
            {
                "tangemComApiKey": "api-key-123",
                "moonPayApiKey": "moon-pay-key",
                "moonPayApiSecretKey": "secret-key",
                "mercuryoWidgetId": "",
                "blockchainSdkConfig": {
                    "blockchairApiKey": "blockchair-key",
                    "blockcypherTokens": ["token1", "token2"],
                    "quickNodeSolanaCredentials": {
                        "apiKey": "solana-key",
                        "subdomain": "solana-node"
                    }
                },
                "isFeatureEnabled": true,
                "maxRetryCount": 3
            }
        """.trimIndent()

        // Act
        val generatedCode = generateAndReadOutput(json)

        // Assert
        // Top-level properties
        assertThat(generatedCode).contains("""const val tangemComApiKey: String = "api-key-123"""")
        assertThat(generatedCode).contains("val mercuryoWidgetId: String? = null")
        assertThat(generatedCode).contains("const val isFeatureEnabled: Boolean = true")
        assertThat(generatedCode).contains("const val maxRetryCount: Long = 3")

        // Nested object
        assertThat(generatedCode).contains("object BlockchainSdkConfig {")
        assertThat(generatedCode).contains("""const val blockchairApiKey: String = "blockchair-key"""")
        assertThat(generatedCode).contains("val blockcypherTokens: List<String>")

        // Deeply nested object
        assertThat(generatedCode).contains("object QuickNodeSolanaCredentials {")
    }

    @Test
    fun `generate converts dot-separated object names to PascalCase`() {
        // Arrange - testing the customer.io case that caused the original build failure
        val json = """
            {
                "customer.io": {
                    "TrackSiteID": "site-id-123",
                    "TrackApiKey": "api-key-456"
                }
            }
        """.trimIndent()

        // Act
        val generatedCode = generateAndReadOutput(json)

        // Assert
        assertThat(generatedCode).contains("object CustomerIo {")
        // Property names have first letter lowercased (Kotlin convention)
        assertThat(generatedCode).contains("""const val trackSiteID: String = "site-id-123"""")
        assertThat(generatedCode).contains("""const val trackApiKey: String = "api-key-456"""")
    }

    @Test
    fun `generate converts dot-separated property names to camelCase`() {
        // Arrange - testing property names with dots (not nested objects)
        val json = """
            {
                "api.key": "test-key",
                "service.url": "https://example.com"
            }
        """.trimIndent()

        // Act
        val generatedCode = generateAndReadOutput(json)

        // Assert - dots in property names are converted to camelCase
        assertThat(generatedCode).contains("""const val apiKey: String = "test-key"""")
        assertThat(generatedCode).contains("""const val serviceUrl: String = "https://example.com"""")
    }

    @Test
    fun `generate preserves valid property names without transformation`() {
        // Arrange - valid Kotlin identifiers should not be transformed
        val json = """
            {
                "apiKey": "key1",
                "moonPayApiKey": "moon-pay-key",
                "isEnabled": true,
                "maxRetryCount": 5
            }
        """.trimIndent()

        // Act
        val generatedCode = generateAndReadOutput(json)

        // Assert - original names preserved exactly
        assertThat(generatedCode).contains("""const val apiKey: String = "key1"""")
        assertThat(generatedCode).contains("""const val moonPayApiKey: String = "moon-pay-key"""")
        assertThat(generatedCode).contains("const val isEnabled: Boolean = true")
        assertThat(generatedCode).contains("const val maxRetryCount: Long = 5")
    }

    private fun generateAndReadOutput(jsonContent: String): String {
        val inputFile = File(tempDir, "config.json").apply {
            writeText(jsonContent)
        }

        EnvironmentConfigGenerator.generate(inputFile, outputDir)

        val generatedFile = File(
            outputDir,
            "com/tangem/datasource/local/config/environment/generated/GeneratedEnvironmentConfig.kt"
        )

        assertThat(generatedFile.exists()).isTrue()
        return generatedFile.readText()
    }
}




