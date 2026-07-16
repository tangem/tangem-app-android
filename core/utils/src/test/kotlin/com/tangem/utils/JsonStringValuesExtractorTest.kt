package com.tangem.utils

import com.google.common.truth.Truth
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JsonStringValuesExtractorTest {

    @Test
    fun `extract returns single value for string primitive`() {
        // Arrange
        val json = JsonPrimitive("hello")

        // Act
        val actual = JsonStringValuesExtractor.extract(json)

        // Assert
        Truth.assertThat(actual).containsExactly("hello")
    }

    @Test
    fun `extract returns empty for numeric primitive`() {
        // Arrange
        val json = JsonPrimitive(42)

        // Act
        val actual = JsonStringValuesExtractor.extract(json)

        // Assert
        Truth.assertThat(actual).isEmpty()
    }

    @Test
    fun `extract returns empty for boolean primitive`() {
        // Arrange
        val json = JsonPrimitive(true)

        // Act
        val actual = JsonStringValuesExtractor.extract(json)

        // Assert
        Truth.assertThat(actual).isEmpty()
    }

    @Test
    fun `extract returns empty for json null`() {
        // Act
        val actual = JsonStringValuesExtractor.extract(JsonNull)

        // Assert
        Truth.assertThat(actual).isEmpty()
    }

    @Test
    fun `extract returns all string values from flat object`() {
        // Arrange
        val json = Json.parseToJsonElement(
            """{"apiKey":"abc","secret":"xyz","count":42,"enabled":true}""",
        )

        // Act
        val actual = JsonStringValuesExtractor.extract(json)

        // Assert
        Truth.assertThat(actual).containsExactly("abc", "xyz")
    }

    @Test
    fun `extract returns all string values from flat array`() {
        // Arrange
        val json = Json.parseToJsonElement("""["one","two",3,true,null]""")

        // Act
        val actual = JsonStringValuesExtractor.extract(json)

        // Assert
        Truth.assertThat(actual).containsExactly("one", "two").inOrder()
    }

    @Test
    fun `extract recurses into nested objects`() {
        // Arrange
        val json = Json.parseToJsonElement(
            """{"outer":{"inner":{"key":"deep"}},"top":"shallow"}""",
        )

        // Act
        val actual = JsonStringValuesExtractor.extract(json)

        // Assert
        Truth.assertThat(actual).containsExactly("deep", "shallow")
    }

    @Test
    fun `extract recurses into nested arrays`() {
        // Arrange
        val json = Json.parseToJsonElement("""[["a","b"],["c",["d"]]]""")

        // Act
        val actual = JsonStringValuesExtractor.extract(json)

        // Assert
        Truth.assertThat(actual).containsExactly("a", "b", "c", "d").inOrder()
    }

    @Test
    fun `extract handles mixed nested objects and arrays`() {
        // Arrange
        val json = Json.parseToJsonElement(
            """{"keys":["k1","k2"],"nested":{"items":[{"name":"x"},{"name":"y"}]}}""",
        )

        // Act
        val actual = JsonStringValuesExtractor.extract(json)

        // Assert
        Truth.assertThat(actual).containsExactly("k1", "k2", "x", "y")
    }

    @Test
    fun `extract returns empty for empty object`() {
        // Arrange
        val json = Json.parseToJsonElement("""{}""")

        // Act
        val actual = JsonStringValuesExtractor.extract(json)

        // Assert
        Truth.assertThat(actual).isEmpty()
    }

    @Test
    fun `extract returns empty for empty array`() {
        // Arrange
        val json = Json.parseToJsonElement("""[]""")

        // Act
        val actual = JsonStringValuesExtractor.extract(json)

        // Assert
        Truth.assertThat(actual).isEmpty()
    }

    @Test
    fun `extract preserves duplicate values`() {
        // Arrange — extractor does NOT dedupe; that's the caller's concern
        val json = Json.parseToJsonElement("""{"a":"same","b":"same","c":"other"}""")

        // Act
        val actual = JsonStringValuesExtractor.extract(json)

        // Assert
        Truth.assertThat(actual).containsExactly("same", "same", "other")
    }

    @Test
    fun `extract returns empty string when string primitive is empty`() {
        // Arrange
        val json = Json.parseToJsonElement("""{"a":"","b":"x"}""")

        // Act
        val actual = JsonStringValuesExtractor.extract(json)

        // Assert — extractor returns "" too; filtering is caller's job
        Truth.assertThat(actual).containsExactly("", "x")
    }
}