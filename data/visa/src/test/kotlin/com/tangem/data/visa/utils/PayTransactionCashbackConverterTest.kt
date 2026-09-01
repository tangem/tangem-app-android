package com.tangem.data.visa.utils

import com.google.common.truth.Truth.assertThat
import com.tangem.spend.datasource.pay.models.response.TransactionCashbackResponse
import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import com.tangem.domain.visa.model.TangemPayTxHistoryItem.Cashback.ExclusionReason
import com.tangem.domain.visa.model.TangemPayTxHistoryItem.Cashback.Status
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.util.Currency

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PayTransactionCashbackConverterTest {

    @ParameterizedTest
    @MethodSource("provideTestModels")
    fun convert(model: ConvertModel) {
        // Act
        val actual = PayTransactionCashbackConverter.convert(model.dto)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ConvertSpendCashback {

        @ParameterizedTest
        @MethodSource("provideTestModels")
        fun convertSpendCashback(model: SpendCashbackModel) {
            // Act
            val actual = PayTransactionCashbackConverter.convertSpendCashback(
                status = model.status,
                amount = model.amount,
                currencyCode = model.currencyCode,
            )

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            // No status — cashback is absent for the transaction.
            SpendCashbackModel(status = null, expected = null),
            SpendCashbackModel(
                status = "confirmed",
                amount = BigDecimal("3.00"),
                currencyCode = "EUR",
                expected = cashback(
                    status = Status.CONFIRMED,
                    amount = BigDecimal("3.00"),
                    currency = Currency.getInstance("EUR"),
                ),
            ),
            SpendCashbackModel(
                status = "estimated",
                expected = cashback(status = Status.ESTIMATED),
            ),
            // Awaiting calculation — amount and currency are null (not "0.00").
            SpendCashbackModel(
                status = "awaiting_calculation",
                amount = null,
                currencyCode = null,
                expected = cashback(status = Status.AWAITING_CALCULATION, amount = null, currency = null),
            ),
            // Unknown status string maps to UNKNOWN rather than throwing.
            SpendCashbackModel(
                status = "brand_new_status",
                expected = cashback(status = Status.UNKNOWN),
            ),
            // Invalid ISO code degrades to null currency instead of throwing.
            SpendCashbackModel(
                status = "confirmed",
                currencyCode = "NOT_A_CODE",
                expected = cashback(status = Status.CONFIRMED, currency = null),
            ),
        )
    }

    data class SpendCashbackModel(
        val status: String?,
        val amount: BigDecimal? = BigDecimal("1.00"),
        val currencyCode: String? = "USD",
        val expected: TangemPayTxHistoryItem.Cashback?,
    )

    data class ConvertModel(
        val dto: TransactionCashbackResponse?,
        val expected: TangemPayTxHistoryItem.Cashback?,
    )

    private fun provideTestModels() = listOf(
        ConvertModel(dto = null, expected = null),
        ConvertModel(
            dto = dto(
                status = "confirmed",
                amount = BigDecimal("3.00"),
                isCapTrimmed = true,
            ),
            expected = cashback(
                status = Status.CONFIRMED,
                amount = BigDecimal("3.00"),
                isCapTrimmed = true,
            ),
        ),
        ConvertModel(
            dto = dto(status = "estimated"),
            expected = cashback(status = Status.ESTIMATED),
        ),
        // Awaiting calculation — amount and currency are null (not "0.00").
        ConvertModel(
            dto = dto(status = "awaiting_calculation", amount = null, currency = null),
            expected = cashback(status = Status.AWAITING_CALCULATION, amount = null, currency = null),
        ),
        // Absent cap_trimmed defaults to false.
        ConvertModel(
            dto = dto(status = "confirmed", isCapTrimmed = null),
            expected = cashback(status = Status.CONFIRMED, isCapTrimmed = false),
        ),
        // Unknown status string maps to UNKNOWN rather than throwing.
        ConvertModel(
            dto = dto(status = "brand_new_status"),
            expected = cashback(status = Status.UNKNOWN),
        ),
        ConvertModel(
            dto = dto(status = "excluded", exclusionReason = "mcc_excluded"),
            expected = cashback(status = Status.EXCLUDED, exclusionReason = ExclusionReason.MCC_EXCLUDED),
        ),
        ConvertModel(
            dto = dto(status = "excluded", exclusionReason = "monthly_cap_reached"),
            expected = cashback(status = Status.EXCLUDED, exclusionReason = ExclusionReason.MONTHLY_CAP_REACHED),
        ),
        ConvertModel(
            dto = dto(status = "excluded", exclusionReason = "merchant_country_excluded"),
            expected = cashback(status = Status.EXCLUDED, exclusionReason = ExclusionReason.MERCHANT_COUNTRY_EXCLUDED),
        ),
        ConvertModel(
            dto = dto(status = "excluded", exclusionReason = "below-min"),
            expected = cashback(status = Status.EXCLUDED, exclusionReason = ExclusionReason.BELOW_MIN),
        ),
        // Deprecated / unknown reason maps to UNKNOWN.
        ConvertModel(
            dto = dto(status = "excluded", exclusionReason = "currency_excluded"),
            expected = cashback(status = Status.EXCLUDED, exclusionReason = ExclusionReason.UNKNOWN),
        ),
    )

    private fun dto(
        status: String = "confirmed",
        amount: BigDecimal? = BigDecimal("1.00"),
        currency: String? = "USD",
        isCapTrimmed: Boolean? = null,
        exclusionReason: String? = null,
    ) = TransactionCashbackResponse(
        status = status,
        amount = amount,
        currency = currency,
        isCapTrimmed = isCapTrimmed,
        exclusionReason = exclusionReason,
    )

    private fun cashback(
        status: Status,
        amount: BigDecimal? = BigDecimal("1.00"),
        currency: Currency? = Currency.getInstance("USD"),
        isCapTrimmed: Boolean = false,
        exclusionReason: ExclusionReason? = null,
    ) = TangemPayTxHistoryItem.Cashback(
        status = status,
        amount = amount,
        currency = currency,
        isCapTrimmed = isCapTrimmed,
        exclusionReason = exclusionReason,
    )
}