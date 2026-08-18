package com.tangem.domain.jointaccount.signing

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.jointaccount.model.JointAccountCreationPayload
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CanonicalJsonTest {

    @ParameterizedTest
    @ProvideTestModels
    fun canonicalize(model: TestModel) {
        // Act
        val actual = CanonicalJson.canonicalize(model.input).toString(Charsets.UTF_8)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        TestModel(
            input = mapOf("b" to 1, "a" to 2),
            expected = """{"a":2,"b":1}""",
        ),
        // RFC 8785 orders keys by UTF-16 code units: uppercase before lowercase, digits before letters
        TestModel(
            input = mapOf("b" to 0, "B" to 0, "1" to 0, "a" to 0),
            expected = """{"1":0,"B":0,"a":0,"b":0}""",
        ),
        TestModel(
            input = mapOf("nested" to mapOf("y" to listOf(1, 2), "x" to "v"), "flag" to true, "none" to null),
            expected = """{"flag":true,"nested":{"x":"v","y":[1,2]},"none":null}""",
        ),
        // Emoji and non-ASCII are not escaped — serialized as raw UTF-8
        TestModel(
            input = mapOf("name" to "Семья 👨‍👩‍👧"),
            expected = """{"name":"Семья 👨‍👩‍👧"}""",
        ),
        // Control characters and JSON specials are escaped per RFC 8785
        TestModel(
            input = mapOf("s" to "a\"b\\c\nd\te\rf\bg\u000Ch\u0001i"),
            expected = "{\"s\":\"a\\\"b\\\\c\\nd\\te\\rf\\bg\\fh\\u0001i\"}",
        ),
        // Strings are not normalized: leading/trailing spaces survive
        TestModel(
            input = mapOf("name" to " Alice "),
            expected = """{"name":" Alice "}""",
        ),
    )

    @Test
    fun `GIVEN creation payload WHEN canonicalize THEN produces the exact form the backend verifies`() {
        // Arrange
        val payload = JointAccountCreationPayload(
            config = JointAccountCreationPayload.Config(
                name = "Family",
                icon = "Family",
                iconColor = "Azure",
                membersCount = 3,
                threshold = 2,
            ),
            creator = JointAccountCreationPayload.Creator(
                walletId = "4B2F1C8A9E7D6053A1B4C7E2F8D9A0B3C5E7F1A2D4B6C8E0F2A4B6C8D0E2F4A6",
                name = "Alice",
                address = "0x7e5f4552091a69125d5DfCb7b8C2659029395Bdf",
                derivation = 0,
            ),
        )

        // Act
        val actual = CanonicalJson.canonicalize(payload.toCanonicalMap()).toString(Charsets.UTF_8)

        // Assert
        val expected = """{"config":{"icon":"Family","iconColor":"Azure","membersCount":3,"name":"Family",""" +
            """"threshold":2},"creator":{"address":"0x7e5f4552091a69125d5DfCb7b8C2659029395Bdf","derivation":0,""" +
            """"name":"Alice","walletId":"4B2F1C8A9E7D6053A1B4C7E2F8D9A0B3C5E7F1A2D4B6C8E0F2A4B6C8D0E2F4A6"}}"""
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN floating point value WHEN canonicalize THEN throws instead of guessing the format`() {
        // Act
        val exception = runCatching { CanonicalJson.canonicalize(mapOf("x" to 1.5)) }.exceptionOrNull()

        // Assert
        assertThat(exception).isInstanceOf(IllegalStateException::class.java)
    }

    data class TestModel(val input: Map<String, Any?>, val expected: String)
}