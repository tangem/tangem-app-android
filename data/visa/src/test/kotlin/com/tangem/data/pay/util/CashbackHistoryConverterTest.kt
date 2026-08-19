package com.tangem.data.pay.util

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.pay.model.CashbackHistory
import com.tangem.spend.datasource.pay.models.response.CashbackHistoryResponse
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CashbackHistoryConverterTest {

    @ParameterizedTest
    @MethodSource("provideTestModels")
    fun convert(model: ConvertModel) {
        // Act
        val actual = CashbackHistoryConverter.convert(model.response)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        ConvertModel(
            name = "full response -> mapped history ordered oldest to newest",
            response = createResponse(
                items = listOf(
                    createItem(year = 2026, month = 2, confirmedAmount = BigDecimal("5.00")),
                    createItem(year = 2026, month = 6, confirmedAmount = BigDecimal("22.54")),
                ),
            ),
            expected = CashbackHistory(
                months = listOf(
                    expectedMonth(month = 2, confirmedAmount = BigDecimal("5.00")),
                    expectedMonth(month = 6, confirmedAmount = BigDecimal("22.54")),
                ),
            ),
        ),
        ConvertModel(
            name = "negative amount preserved for current-month refund",
            response = createResponse(
                items = listOf(createItem(year = 2026, month = 6, confirmedAmount = BigDecimal("-2.15"))),
            ),
            expected = CashbackHistory(
                months = listOf(expectedMonth(month = 6, confirmedAmount = BigDecimal("-2.15"))),
            ),
        ),
        ConvertModel(
            name = "null items -> empty months",
            response = createResponse(items = null),
            expected = CashbackHistory(months = emptyList()),
        ),
    )

    internal data class ConvertModel(
        val name: String,
        val response: CashbackHistoryResponse,
        val expected: CashbackHistory,
    ) {
        override fun toString(): String = name
    }

    private companion object {

        fun createResponse(
            items: List<CashbackHistoryResponse.Item>? = listOf(
                createItem(year = 2026, month = 6, confirmedAmount = BigDecimal("22.54")),
            ),
        ) = CashbackHistoryResponse(result = CashbackHistoryResponse.Result(items = items))

        fun createItem(year: Int, month: Int, confirmedAmount: BigDecimal, currency: String = "USD") =
            CashbackHistoryResponse.Item(
                year = year,
                month = month,
                confirmedAmount = confirmedAmount,
                currency = currency,
            )

        fun expectedMonth(month: Int, confirmedAmount: BigDecimal) = CashbackHistory.MonthlyCashback(
            year = 2026,
            month = month,
            confirmedAmount = confirmedAmount,
            currency = "USD",
        )
    }
}