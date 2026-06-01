package com.tangem.datasource.local.logs

import com.google.common.truth.Truth
import com.tangem.datasource.local.logs.SensitiveUrlMasker.Companion.MASKED_VALUE
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SensitiveUrlMaskerTest {

    @ParameterizedTest
    @ProvideTestModels
    fun mask(model: TestModel) {
        // Arrange
        val masker = SensitiveUrlMasker(model.sensitiveValues)

        // Act
        val actual = masker.mask(model.input)

        // Assert
        Truth.assertThat(actual).isEqualTo(model.expected)
    }

    @Test
    fun `mask returns url unchanged when no sensitive values provided`() {
        // Arrange
        val masker = SensitiveUrlMasker(emptyList())
        val url = "https://api.tangem.com/v1/cards/abc123"

        // Act
        val actual = masker.mask(url)

        // Assert
        Truth.assertThat(actual).isEqualTo(url)
    }

    @Test
    fun `constructor deduplicates input values`() {
        // Arrange — same secret repeated; if no dedup, replace would be invoked twice
        // (idempotent on already-masked string, but we assert behavior is identical
        // to a single-value masker as a smoke-check)
        val withDuplicates = SensitiveUrlMasker(listOf("secret123", "secret123", "secret123"))
        val withSingle = SensitiveUrlMasker(listOf("secret123"))
        val url = "https://api.tangem.com/?key=secret123"

        // Act
        val withDup = withDuplicates.mask(url)
        val withSingleResult = withSingle.mask(url)

        // Assert
        Truth.assertThat(withDup).isEqualTo(withSingleResult)
        Truth.assertThat(withDup).isEqualTo("https://api.tangem.com/?key=$MASKED_VALUE")
    }

    private fun provideTestModels() = listOf(
        TestModel(
            input = "https://api.tangem.com/?key=secret123",
            sensitiveValues = listOf("secret123"),
            expected = "https://api.tangem.com/?key=$MASKED_VALUE",
        ),
        TestModel(
            input = "https://api.tangem.com/?a=alpha&b=beta",
            sensitiveValues = listOf("alpha", "beta"),
            expected = "https://api.tangem.com/?a=$MASKED_VALUE&b=$MASKED_VALUE",
        ),
        TestModel(
            input = "https://api.tangem.com/?key=SECRET123",
            sensitiveValues = listOf("secret123"),
            expected = "https://api.tangem.com/?key=$MASKED_VALUE",
        ),
        TestModel(
            input = "https://api.tangem.com/v1/balance",
            sensitiveValues = listOf("notInUrl"),
            expected = "https://api.tangem.com/v1/balance",
        ),
        TestModel(
            input = "https://api.tangem.com/?key=secret123&other=secret123",
            sensitiveValues = listOf("secret123"),
            expected = "https://api.tangem.com/?key=$MASKED_VALUE&other=$MASKED_VALUE",
        ),
        TestModel(
            input = "https://api.tangem.com/v1/cards",
            sensitiveValues = emptyList(),
            expected = "https://api.tangem.com/v1/cards",
        ),
        // Regression: when one value is a prefix of another, the longer one must be masked first
        // regardless of input order, otherwise the suffix leaks (e.g. "my-node-prod" -> "******-prod").
        TestModel(
            input = "https://my-node-prod.example.com/v1",
            sensitiveValues = listOf("my-node", "my-node-prod"),
            expected = "https://$MASKED_VALUE.example.com/v1",
        ),
        TestModel(
            input = "https://my-node-prod.example.com/v1",
            sensitiveValues = listOf("my-node-prod", "my-node"),
            expected = "https://$MASKED_VALUE.example.com/v1",
        ),
    )

    data class TestModel(
        val input: String,
        val sensitiveValues: List<String>,
        val expected: String,
    )
}