package com.tangem.data.blockaid.converters

import com.domain.blockaid.models.transaction.GasEstimationResult
import com.google.common.truth.Truth.assertThat
import com.tangem.store.datasource.blockaid.models.response.GasEstimationItem
import com.tangem.store.datasource.blockaid.models.response.GasEstimationResponse
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GasEstimationResponseConverterTest {

    @ParameterizedTest
    @MethodSource("provideTestModels")
    fun `GIVEN simulation response WHEN convert THEN maps it to the gas estimation result`(model: ConvertModel) {
        // Act
        val actual = GasEstimationResponseConverter.convert(model.response)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        ConvertModel(
            name = "every simulation succeeded",
            response = listOf(
                item(status = "Success", estimate = "0x5eb4e"),
                item(status = "Success", estimate = "0x186a0"),
            ),
            expected = GasEstimationResult.Estimated(
                gasLimits = listOf(BigInteger.valueOf(387918), BigInteger.valueOf(100000)),
            ),
        ),
        ConvertModel(
            name = "estimates without a status",
            response = listOf(item(status = null, estimate = "0x5eb4e")),
            expected = GasEstimationResult.Estimated(gasLimits = listOf(BigInteger.valueOf(387918))),
        ),
        ConvertModel(
            name = "one simulation reverted",
            response = listOf(
                item(status = "Success", estimate = "0x5eb4e"),
                item(status = "Error", error = "Simulation Error: Reverted: 'ZeroAmount'"),
            ),
            expected = GasEstimationResult.Reverted,
        ),
        ConvertModel(
            name = "successful simulation without an estimate",
            response = listOf(item(status = "Success"), item(status = "Success", estimate = "0x5eb4e")),
            expected = GasEstimationResult.Unavailable,
        ),
        ConvertModel(
            name = "unknown status without an estimate",
            response = listOf(item(status = "Pending")),
            expected = GasEstimationResult.Unavailable,
        ),
        ConvertModel(
            name = "no status and no estimate",
            response = listOf(item(status = null)),
            expected = GasEstimationResult.Unavailable,
        ),
        ConvertModel(
            name = "empty response",
            response = emptyList(),
            expected = GasEstimationResult.Unavailable,
        ),
    )

    private fun item(status: String?, estimate: String? = null, error: String? = null) =
        GasEstimationResponse(GasEstimationItem(status = status, estimate = estimate, error = error))

    data class ConvertModel(
        val name: String,
        val response: List<GasEstimationResponse>,
        val expected: GasEstimationResult,
    ) {
        override fun toString(): String = name
    }
}