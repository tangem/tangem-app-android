package com.tangem.grow.datasource.polymarket.clob.models

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PolymarketSessionSignersResponseTest {

    private val adapter = Moshi.Builder().build().adapter(PolymarketSessionSignersResponse::class.java)

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a session signers body WHEN decoded THEN valid_until is read as seconds`(model: DecodeModel) {
        // Act
        val actual = adapter.fromJson(model.json)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        DecodeModel(
            json = """{"wallet":"0xdw","signers":[{"address":"0xAB","scopes":["CLOB"],"valid_until":1804982400}]}""",
            expected = response(validUntil = 1804982400),
        ),
        DecodeModel(
            json = """{"wallet":"0xdw","signers":[{"address":"0xAB","scopes":["CLOB"],"valid_until":"1804982400"}]}""",
            expected = response(validUntil = 1804982400),
        ),
        DecodeModel(
            json = """{"wallet":"0xdw","signers":[{"address":"0xAB","scopes":["CLOB"],"valid_until":null}]}""",
            expected = response(validUntil = null),
        ),
        DecodeModel(
            json = """{"wallet":"0xdw","signers":[]}""",
            expected = PolymarketSessionSignersResponse(wallet = "0xdw", signers = emptyList()),
        ),
    )

    private fun response(validUntil: Long?) = PolymarketSessionSignersResponse(
        wallet = "0xdw",
        signers = listOf(
            PolymarketSessionSignerDto(address = "0xAB", scopes = listOf("CLOB"), validUntil = validUntil),
        ),
    )

    data class DecodeModel(val json: String, val expected: PolymarketSessionSignersResponse)
}