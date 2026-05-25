package com.tangem.features.tangempay.deeplink

import com.tangem.domain.pay.utils.TangemPayTxHistoryItemStatusConverter
import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import com.tangem.utils.extensions.orZero
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import java.math.BigDecimal
import java.util.Currency

object TangemPayPushPayloadToTxHistoryItemConverter {

    private const val KEY_ID = "transaction_id"
    private const val KEY_AMOUNT = "amount"
    private const val KEY_CURRENCY = "currency"
    private const val KEY_LOCAL_AMOUNT = "local_amount"
    private const val KEY_LOCAL_CURRENCY = "local_currency"
    private const val KEY_AUTHORIZED_AMOUNT = "authorized_amount"
    private const val KEY_MERCHANT_NAME = "merchant_name"
    private const val KEY_ENRICHED_MERCHANT_NAME = "enriched_merchant_name"
    private const val KEY_ENRICHED_MERCHANT_ICON = "enriched_merchant_icon"
    private const val KEY_ENRICHED_MERCHANT_CATEGORY = "enriched_merchant_category"
    private const val KEY_MERCHANT_CATEGORY = "merchant_category"
    private const val KEY_MERCHANT_CATEGORY_CODE = "merchant_category_code"
    private const val KEY_STATUS = "status"
    private const val KEY_DECLINED_REASON = "declined_reason"
    private const val KEY_AUTHORIZED_AT = "authorized_at"
    private const val KEY_POSTED_AT = "posted_at"
    private const val KEY_TRANSACTION_HASH = "transaction_hash"

    @Suppress("ComplexCondition")
    fun convertSpend(payload: Map<String, String>): TangemPayTxHistoryItem.Spend? {
        val id = payload[KEY_ID]?.ifEmpty { null } ?: return null
        val amount = payload[KEY_AMOUNT]?.toBigDecimalOrNull() ?: return null
        val currency = payload[KEY_CURRENCY]?.let(::parseCurrency) ?: return null
        val merchantName = payload[KEY_MERCHANT_NAME] ?: payload[KEY_ENRICHED_MERCHANT_NAME] ?: return null
        val status = payload[KEY_STATUS]?.ifEmpty { null } ?: return null
        val authorizedAt = payload[KEY_AUTHORIZED_AT]?.let(::parseDateTime) ?: return null

        return TangemPayTxHistoryItem.Spend(
            id = id,
            jsonRepresentation = payload.toString(),
            date = authorizedAt.withZone(DateTimeZone.getDefault()),
            amount = amount,
            currency = currency,
            authorizedAmount = payload[KEY_AUTHORIZED_AMOUNT]?.toBigDecimalOrNull().orZero(),
            localAmount = payload[KEY_LOCAL_AMOUNT]?.toBigDecimalOrNull(),
            localCurrency = payload[KEY_LOCAL_CURRENCY]?.let(::parseCurrency),
            enrichedMerchantName = payload[KEY_ENRICHED_MERCHANT_NAME],
            merchantName = merchantName,
            enrichedMerchantCategory = payload[KEY_ENRICHED_MERCHANT_CATEGORY],
            merchantCategoryCode = payload[KEY_MERCHANT_CATEGORY_CODE],
            merchantCategory = payload[KEY_MERCHANT_CATEGORY],
            status = TangemPayTxHistoryItemStatusConverter.convert(status),
            enrichedMerchantIconUrl = payload[KEY_ENRICHED_MERCHANT_ICON],
            declinedReason = payload[KEY_DECLINED_REASON],
        )
    }

    @Suppress("ComplexCondition")
    fun convertCollateral(payload: Map<String, String>): TangemPayTxHistoryItem.Collateral? {
        val id = payload[KEY_ID]?.ifEmpty { null } ?: return null
        val amount = payload[KEY_AMOUNT]?.toBigDecimalOrNull() ?: return null
        val transactionHash = payload[KEY_TRANSACTION_HASH]?.ifEmpty { null } ?: return null
        val postedAt = payload[KEY_POSTED_AT]?.let(::parseDateTime) ?: return null
        val currency = payload[KEY_CURRENCY]?.let(::parseCurrency) ?: return null

        return TangemPayTxHistoryItem.Collateral(
            id = id,
            jsonRepresentation = payload.toString(),
            date = postedAt.withZone(DateTimeZone.getDefault()),
            currency = currency,
            amount = amount,
            transactionHash = transactionHash,
            type = if (amount >= BigDecimal.ZERO) {
                TangemPayTxHistoryItem.Type.Deposit
            } else {
                TangemPayTxHistoryItem.Type.Withdrawal
            },
        )
    }

    private fun parseCurrency(code: String): Currency? = runCatching {
        return Currency.getInstance(code.uppercase())
    }.getOrNull()

    private fun parseDateTime(value: String): DateTime? = runCatching {
        return DateTime.parse(value).withZone(DateTimeZone.getDefault())
    }.getOrNull()
}