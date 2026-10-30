package com.tangem.data.transaction.api.models

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.json.NetworkJsonFactory
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SendIntentResponseTest {

    private val json = NetworkJsonFactory.create()

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN raw status WHEN decoded THEN it maps to the known status or unknown`(model: StatusModel) {
        // Act
        val actual = json.decodeFromString<SendIntentResponse>(response(model.rawStatus)).status

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        StatusModel(rawStatus = "\"CREATED\"", expected = SendIntentStatus.CREATED),
        StatusModel(rawStatus = "\"created\"", expected = SendIntentStatus.CREATED),
        StatusModel(rawStatus = "\"COMPLETED\"", expected = SendIntentStatus.COMPLETED),
        StatusModel(rawStatus = "\"EXPIRED\"", expected = SendIntentStatus.UNKNOWN),
        StatusModel(rawStatus = "null", expected = SendIntentStatus.UNKNOWN),
    )

    data class StatusModel(val rawStatus: String, val expected: SendIntentStatus)

    private fun response(rawStatus: String) = """
        {
          "intentId": "3f7c2a1e-9b4d-4c6a-8e21-5d0f7a9b1c33",
          "status": $rawStatus,
          "networkId": "polygon-pos",
          "fromAddress": "0xd8da6bf26964af9d7eed9e03e53415d37aa96045",
          "toAddress": "0x1111111111111111111111111111111111111111",
          "tokenId": "tether",
          "amount": "12.5",
          "clientId": "string",
          "sessionId": "string"
        }
    """.trimIndent()
}