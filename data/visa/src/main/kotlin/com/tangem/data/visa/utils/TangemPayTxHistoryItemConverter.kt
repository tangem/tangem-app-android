package com.tangem.data.visa.utils

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.remote.json.NetworkJson
import com.tangem.domain.pay.utils.TangemPayTxHistoryItemStatusConverter
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import com.tangem.spend.datasource.pay.models.response.TangemPayTxHistoryResponse
import com.tangem.spend.datasource.pay.models.response.TangemPayTxHistoryResponse.Transaction
import com.tangem.utils.converter.Converter
import com.tangem.utils.extensions.isPositive
import com.tangem.utils.extensions.isZero
import com.tangem.utils.extensions.orZero
import com.tangem.utils.logging.TangemLogger
import kotlinx.serialization.json.Json
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import java.math.BigDecimal
import java.util.Currency
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

internal class TangemPayTxHistoryItemConverter @Inject constructor(
    @NetworkJson private val json: Json,
    private val analytics: AnalyticsEventHandler,
) : Converter<Transaction, TangemPayTxHistoryItem?> {

    private val reportedUnknownTypes = ConcurrentHashMap.newKeySet<String>()

    override fun convert(value: Transaction): TangemPayTxHistoryItem? {
        val item = when (value) {
            is Transaction.Spend -> value.spend?.let { convertSpend(id = value.id, spend = it) }
            is Transaction.Refund -> value.refund?.let { convertRefund(id = value.id, refund = it) }
            is Transaction.Payment -> value.payment?.let { convertPayment(id = value.id, payment = it) }
            is Transaction.Fee -> value.fee?.let { convertFee(id = value.id, fee = it) }
            is Transaction.Collateral -> value.collateral?.let { convertCollateral(id = value.id, collateral = it) }
            is Transaction.Unknown -> {
                reportUnknownType(value)
                null
            }
        }

        if (item == null) {
            TangemLogger.e("Transaction ${value.id} is skipped: ${value.skipReason()}")
        }

        return item
    }

    private fun reportUnknownType(transaction: Transaction.Unknown) {
        if (reportedUnknownTypes.add(transaction.type.orEmpty())) {
            analytics.send(TangemPayAnalyticsEvents.UnknownTransactionType(type = transaction.type))
        }
    }

    private fun Transaction.skipReason(): String = when (this) {
        is Transaction.Unknown -> "unknown type '$type'"
        else -> "no payload"
    }

    private fun convertSpend(id: String, spend: TangemPayTxHistoryResponse.SpendData): TangemPayTxHistoryItem.Spend {
        val rawDate = if (spend.amount.signum() < 0) {
            spend.postedAt ?: spend.authorizedAt
        } else {
            spend.authorizedAt
        }
        return TangemPayTxHistoryItem.Spend(
            id = id,
            jsonRepresentation = json.encodeToString(TangemPayTxHistoryResponse.SpendData.serializer(), spend),
            date = rawDate.withLocalZone(),
            amount = spend.amount,
            currency = getJavaCurrencyByCode(spend.currency),
            authorizedAmount = spend.authorizedAmount.orZero(),
            localAmount = spend.localAmount,
            localCurrency = spend.localCurrency?.let(::getJavaCurrencyOrNull),
            enrichedMerchantName = spend.enrichedMerchantName,
            merchantName = spend.merchantName,
            enrichedMerchantCategory = spend.enrichedMerchantCategory,
            merchantCategoryCode = spend.merchantCategoryCode,
            merchantCategory = spend.merchantCategory,
            status = TangemPayTxHistoryItemStatusConverter.convert(spend.status),
            enrichedMerchantIconUrl = spend.enrichedMerchantIcon,
            declinedReason = spend.declinedReason,
            cardName = spend.cardDisplayName,
            cardNumberLast4 = spend.cardNumberEnd,
            cashback = PayTransactionCashbackConverter.convertSpendCashback(
                status = spend.cashbackStatus,
                amount = spend.cashback,
                currencyCode = spend.cashbackCurrencyCode,
            ),
        )
    }

    /**
     * A refund goes through the spend pipeline, where incoming money and clawed-back cashback carry
     * a negative sign (the convention of refund push notifications and the spend UI), so the signs
     * are forced to negative regardless of what the backend sends.
     */
    private fun convertRefund(
        id: String,
        refund: TangemPayTxHistoryResponse.RefundData,
    ): TangemPayTxHistoryItem.Spend {
        return TangemPayTxHistoryItem.Spend(
            id = id,
            jsonRepresentation = json.encodeToString(TangemPayTxHistoryResponse.RefundData.serializer(), refund),
            date = (refund.postedAt ?: refund.authorizedAt).withLocalZone(),
            amount = refund.amount.abs().negate(),
            currency = getJavaCurrencyByCode(refund.currency),
            authorizedAmount = BigDecimal.ZERO,
            localAmount = refund.localAmount?.abs()?.negate(),
            localCurrency = refund.localCurrency?.let(::getJavaCurrencyOrNull),
            enrichedMerchantName = refund.enrichedMerchantName,
            merchantName = refund.merchantName,
            enrichedMerchantCategory = refund.enrichedMerchantCategory,
            merchantCategoryCode = refund.merchantCategoryCode,
            merchantCategory = refund.merchantCategory,
            status = TangemPayTxHistoryItemStatusConverter.convert(refund.status),
            enrichedMerchantIconUrl = refund.enrichedMerchantIcon,
            declinedReason = null,
            cardName = refund.cardDisplayName,
            cardNumberLast4 = refund.cardNumberEnd,
            cashback = PayTransactionCashbackConverter.convertSpendCashback(
                status = refund.cashbackStatus,
                amount = refund.cashback?.abs()?.negate(),
                currencyCode = refund.cashbackCurrencyCode,
            ),
        )
    }

    private fun convertPayment(
        id: String,
        payment: TangemPayTxHistoryResponse.PaymentData,
    ): TangemPayTxHistoryItem.Payment {
        return TangemPayTxHistoryItem.Payment(
            id = id,
            jsonRepresentation = json.encodeToString(TangemPayTxHistoryResponse.PaymentData.serializer(), payment),
            date = payment.postedAt.withLocalZone(),
            currency = getJavaCurrencyByCode(payment.currency),
            amount = payment.amount,
            transactionHash = payment.transactionHash,
        )
    }

    private fun convertFee(id: String, fee: TangemPayTxHistoryResponse.FeeData): TangemPayTxHistoryItem.Fee {
        return TangemPayTxHistoryItem.Fee(
            id = id,
            jsonRepresentation = json.encodeToString(TangemPayTxHistoryResponse.FeeData.serializer(), fee),
            date = fee.postedAt.withLocalZone(),
            currency = getJavaCurrencyByCode(fee.currency),
            amount = fee.amount,
            description = fee.description,
        )
    }

    private fun convertCollateral(
        id: String,
        collateral: TangemPayTxHistoryResponse.CollateralData,
    ): TangemPayTxHistoryItem.Collateral? {
        val date = collateral.postedAt ?: return run {
            TangemLogger.e("Collateral transaction postedAt is null: $collateral")
            return@run null
        }
        return TangemPayTxHistoryItem.Collateral(
            id = id,
            jsonRepresentation = json.encodeToString(
                TangemPayTxHistoryResponse.CollateralData.serializer(),
                collateral,
            ),
            date = date.withLocalZone(),
            currency = Currency.getInstance("usd"),
            amount = collateral.amount,
            transactionHash = collateral.transactionHash,
            type = if (collateral.amount.isPositive() || collateral.amount.isZero()) {
                TangemPayTxHistoryItem.Type.Deposit
            } else {
                TangemPayTxHistoryItem.Type.Withdrawal
            },
        )
    }

    private fun DateTime.withLocalZone(): DateTime {
        return withZone(DateTimeZone.getDefault())
    }
}