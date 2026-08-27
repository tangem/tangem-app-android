package com.tangem.data.visa.utils

import com.google.common.truth.Truth.assertThat
import com.tangem.spend.datasource.pay.models.response.TransactionCashbackResponse
import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import com.tangem.domain.visa.model.TangemPayTxHistoryItem.Cashback.ExclusionReason
import com.tangem.domain.visa.model.TangemPayTxHistoryItem.Cashback.Status
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
                promotionIds = listOf("promo-1", "promo-2"),
            ),
            expected = cashback(
                status = Status.CONFIRMED,
                amount = BigDecimal("3.00"),
                isCapTrimmed = true,
                promotionIds = listOf("promo-1", "promo-2"),
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
        // Absent cap_trimmed defaults to false; absent promotion_ids defaults to empty.
        ConvertModel(
            dto = dto(status = "confirmed", isCapTrimmed = null, promotionIds = null),
            expected = cashback(status = Status.CONFIRMED, isCapTrimmed = false, promotionIds = emptyList()),
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
            dto = dto(status = "excluded", exclusionReason = "customer_blocklisted"),
            expected = cashback(status = Status.EXCLUDED, exclusionReason = ExclusionReason.CUSTOMER_BLOCKLISTED),
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
        promotionIds: List<String>? = null,
    ) = TransactionCashbackResponse(
        status = status,
        amount = amount,
        currency = currency,
        isCapTrimmed = isCapTrimmed,
        exclusionReason = exclusionReason,
        promotionIds = promotionIds,
    )

    private fun cashback(
        status: Status,
        amount: BigDecimal? = BigDecimal("1.00"),
        currency: Currency? = Currency.getInstance("USD"),
        isCapTrimmed: Boolean = false,
        exclusionReason: ExclusionReason? = null,
        promotionIds: List<String> = emptyList(),
    ) = TangemPayTxHistoryItem.Cashback(
        status = status,
        amount = amount,
        currency = currency,
        isCapTrimmed = isCapTrimmed,
        exclusionReason = exclusionReason,
        promotionIds = promotionIds,
    )
}