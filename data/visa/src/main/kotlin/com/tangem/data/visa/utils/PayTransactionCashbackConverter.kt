package com.tangem.data.visa.utils

import com.tangem.spend.datasource.pay.models.response.TransactionCashbackResponse
import com.tangem.domain.visa.model.TangemPayTxHistoryItem.Cashback
import com.tangem.domain.visa.model.TangemPayTxHistoryItem.Cashback.ExclusionReason
import com.tangem.domain.visa.model.TangemPayTxHistoryItem.Cashback.Status
import com.tangem.utils.converter.Converter
import java.util.Currency

/**
 * Maps the per-transaction cashback DTO to its domain model. Returns `null` when the BFF sends no
 * cashback object (feature disabled, program deactivated, or a non-spend transaction).
 */
internal object PayTransactionCashbackConverter : Converter<TransactionCashbackResponse?, Cashback?> {

    override fun convert(value: TransactionCashbackResponse?): Cashback? {
        value ?: return null
        return Cashback(
            status = convertStatus(value.status),
            amount = value.amount,
            // Degrade to no currency (badge hidden) on an unrecognized ISO code rather than failing the whole page.
            currency = value.currency?.let { runCatching { Currency.getInstance(it) }.getOrNull() },
            isCapTrimmed = value.isCapTrimmed == true,
            exclusionReason = value.exclusionReason?.let(::convertExclusionReason),
            promotionIds = value.promotionIds.orEmpty(),
        )
    }

    private fun convertStatus(status: String): Status = when (status.lowercase()) {
        "estimated" -> Status.ESTIMATED
        "confirmed" -> Status.CONFIRMED
        "excluded" -> Status.EXCLUDED
        "awaiting_calculation" -> Status.AWAITING_CALCULATION
        else -> Status.UNKNOWN
    }

    private fun convertExclusionReason(reason: String): ExclusionReason = when (reason.lowercase()) {
        "mcc_excluded" -> ExclusionReason.MCC_EXCLUDED
        "monthly_cap_reached" -> ExclusionReason.MONTHLY_CAP_REACHED
        "customer_blocklisted" -> ExclusionReason.CUSTOMER_BLOCKLISTED
        "merchant_country_excluded" -> ExclusionReason.MERCHANT_COUNTRY_EXCLUDED
        "below-min" -> ExclusionReason.BELOW_MIN
        else -> ExclusionReason.UNKNOWN
    }
}