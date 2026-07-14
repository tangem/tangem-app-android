package com.tangem.data.pay.util

import com.google.common.truth.Truth.assertThat
import com.tangem.datasource.api.pay.models.response.CashbackHistoryResponse
import com.tangem.domain.pay.model.CashbackHistory
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
                currency = "USD",
                items = listOf(
                    createItem(year = 2026, month = 2, confirmedAmount = BigDecimal("5.00")),
                    createItem(year = 2026, month = 6, confirmedAmount = BigDecimal("22.54")),
                ),
            ),
            expected = CashbackHistory(
                currency = "USD",
                months = listOf(
                    CashbackHistory.MonthlyCashback(year = 2026, month = 2, confirmedAmount = BigDecimal("5.00")),
                    CashbackHistory.MonthlyCashback(year = 2026, month = 6, confirmedAmount = BigDecimal("22.54")),
                ),
            ),
        ),
        ConvertModel(
            name = "negative amount preserved for current-month refund",
            response = createResponse(
                items = listOf(createItem(year = 2026, month = 6, confirmedAmount = BigDecimal("-2.15"))),
            ),
            expected = CashbackHistory(
                currency = "USD",
                months = listOf(
                    CashbackHistory.MonthlyCashback(year = 2026, month = 6, confirmedAmount = BigDecimal("-2.15")),
                ),
            ),
        ),
        ConvertModel(
            name = "null confirmed_amount -> ZERO",
            response = createResponse(
                items = listOf(createItem(year = 2026, month = 6, confirmedAmount = null)),
            ),
            expected = CashbackHistory(
                currency = "USD",
                months = listOf(
                    CashbackHistory.MonthlyCashback(year = 2026, month = 6, confirmedAmount = BigDecimal.ZERO),
                ),
            ),
        ),
        ConvertModel(
            name = "null currency -> empty string",
            response = createResponse(currency = null, items = emptyList()),
            expected = CashbackHistory(currency = "", months = emptyList()),
        ),
        ConvertModel(
            name = "null items -> empty months",
            response = createResponse(items = null),
            expected = CashbackHistory(currency = "USD", months = emptyList()),
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
            currency: String? = "USD",
            items: List<CashbackHistoryResponse.Item>? = listOf(
                createItem(year = 2026, month = 6, confirmedAmount = BigDecimal("22.54")),
            ),
        ) = CashbackHistoryResponse(
            currency = currency,
            items = items,
        )

        fun createItem(year: Int, month: Int, confirmedAmount: BigDecimal?) = CashbackHistoryResponse.Item(
            year = year,
            month = month,
            confirmedAmount = confirmedAmount,
        )
    }
}