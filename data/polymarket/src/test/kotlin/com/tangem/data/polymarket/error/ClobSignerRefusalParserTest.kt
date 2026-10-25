package com.tangem.data.polymarket.error

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ClobSignerRefusalParserTest {

    @ParameterizedTest
    @ProvideTestModels
    fun parse(model: ParseModel) {
        // Act
        val actual = ClobSignerRefusalParser.parse(model.errorBody)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    internal data class ParseModel(val name: String, val errorBody: String?, val expected: String?) {
        override fun toString(): String = name
    }

    private fun provideTestModels() = listOf(
        ParseModel(
            name = "an invalid signature is a refusal",
            errorBody = "{\"error\":\"invalid signature\"}",
            expected = "invalid signature",
        ),
        ParseModel(
            name = "a signer not bound to the API key is a refusal",
            errorBody = "{\"error\":\"the order signer address has to be the address of the API KEY\"}",
            expected = "the order signer address has to be the address of the API KEY",
        ),
        ParseModel(
            name = "an invalid api key is a refusal",
            errorBody = "{\"error\":\"Unauthorized/Invalid api key\"}",
            expected = "Unauthorized/Invalid api key",
        ),
        ParseModel(
            name = "a market error is not a refusal",
            errorBody = "{\"error\":\"not enough balance / allowance\"}",
            expected = null,
        ),
        ParseModel(
            name = "a marker outside the error field is not a refusal",
            errorBody = "{\"detail\":\"invalid signature\"}",
            expected = null,
        ),
        ParseModel(name = "a plain-text body is not a refusal", errorBody = "invalid signature", expected = null),
        ParseModel(name = "a JSON array is not a refusal", errorBody = "[\"invalid signature\"]", expected = null),
        ParseModel(name = "no body is not a refusal", errorBody = null, expected = null),
    )
}