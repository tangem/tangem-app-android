package com.tangem.data.visa.utils

import com.tangem.spend.datasource.pay.models.response.TransactionCashbackResponse
import com.tangem.domain.visa.model.TangemPayTxHistoryItem.Cashback
import com.tangem.domain.visa.model.TangemPayTxHistoryItem.Cashback.ExclusionReason
import com.tangem.domain.visa.model.TangemPayTxHistoryItem.Cashback.Status
import com.tangem.utils.converter.Converter
import java.math.BigDecimal
import java.util.Currency

internal object PayTransactionCashbackConverter : Converter<TransactionCashbackResponse?, Cashback?> {

    override fun convert(value: TransactionCashbackResponse?): Cashback? {
        value ?: return null
        return Cashback(
            status = convertStatus(value.status),
            amount = value.amount,
            currency = value.currency?.toCurrencyOrNull(),
            isCapTrimmed = value.isCapTrimmed == true,
            exclusionReason = value.exclusionReason?.let(::convertExclusionReason),
        )
    }

    /**
     * Converts the flat cashback fields of the transaction endpoints (`cashback`, `cashback_status`,
     * `cashback_currency_code`). Unlike the cashback-details endpoint, they carry no cap or
     * exclusion data.
     */
    fun convertSpendCashback(status: String?, amount: BigDecimal?, currencyCode: String?): Cashback? {
        status ?: return null
        return Cashback(
            status = convertStatus(status),
            amount = amount,
            currency = currencyCode?.toCurrencyOrNull(),
            isCapTrimmed = false,
            exclusionReason = null,
        )
    }

    private fun String.toCurrencyOrNull(): Currency? = runCatching { Currency.getInstance(this) }.getOrNull()

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
        "merchant_country_excluded" -> ExclusionReason.MERCHANT_COUNTRY_EXCLUDED
        "below-min" -> ExclusionReason.BELOW_MIN
        else -> ExclusionReason.UNKNOWN
    }
}