package com.tangem.spend.datasource.pay.models.response

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.json.NetworkJsonFactory
import com.tangem.spend.datasource.pay.models.response.OrderResponse.Result.Status
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CardOrderStatusFallbackParsingTest {

    private val json = NetworkJsonFactory.create()

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN freeze response WHEN status is unknown or missing THEN it is UNKNOWN`(model: StatusModel) {
        // Act
        val actual = json.decodeFromString(FreezeUnfreezeCardResponse.serializer(), model.raw).result?.status

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN close response WHEN status is unknown or missing THEN it is UNKNOWN`(model: StatusModel) {
        // Act
        val actual = json.decodeFromString(CloseCardResponse.serializer(), model.raw).result.status

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN reissue response WHEN status is unknown or missing THEN it is UNKNOWN`(model: StatusModel) {
        // Act
        val actual = json.decodeFromString(ReissueCardResponse.serializer(), model.raw).result.status

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        StatusModel(raw = orderJson(status = ""","status":"PROCESSING""""), expected = Status.PROCESSING),
        StatusModel(raw = orderJson(status = ""","status":"AWAITING_SOMETHING_NEW""""), expected = Status.UNKNOWN),
        StatusModel(raw = orderJson(status = ""), expected = Status.UNKNOWN),
    )

    private fun orderJson(status: String) = """{"result":{"order_id":"order-1"$status}}"""

    internal data class StatusModel(val raw: String, val expected: Status)
}