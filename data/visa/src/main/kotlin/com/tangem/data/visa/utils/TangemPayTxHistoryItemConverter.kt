package com.tangem.data.visa.utils

import com.squareup.moshi.Moshi
import com.tangem.spend.datasource.pay.models.response.TangemPayTxHistoryResponse
import com.tangem.domain.pay.utils.TangemPayTxHistoryItemStatusConverter
import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import com.tangem.utils.converter.Converter
import com.tangem.utils.extensions.isPositive
import com.tangem.utils.extensions.isZero
import com.tangem.utils.extensions.orZero
import com.tangem.utils.logging.TangemLogger
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import java.math.BigDecimal
import java.util.Currency

internal class TangemPayTxHistoryItemConverter(moshi: Moshi) :
    Converter<TangemPayTxHistoryResponse.Transaction, TangemPayTxHistoryItem?> {

    private val spendAdapter by lazy { moshi.adapter(TangemPayTxHistoryResponse.Spend::class.java) }
    private val refundAdapter by lazy { moshi.adapter(TangemPayTxHistoryResponse.Refund::class.java) }
    private val paymentAdapter by lazy { moshi.adapter(TangemPayTxHistoryResponse.Payment::class.java) }
    private val feeAdapter by lazy { moshi.adapter(TangemPayTxHistoryResponse.Fee::class.java) }
    private val collateralAdapter by lazy { moshi.adapter(TangemPayTxHistoryResponse.Collateral::class.java) }

    override fun convert(value: TangemPayTxHistoryResponse.Transaction): TangemPayTxHistoryItem? {
        return value.spend?.let { convertSpend(id = value.id, spend = it) }
            ?: value.refund?.let { convertRefund(id = value.id, refund = it) }
            ?: value.payment?.let { convertPayment(id = value.id, payment = it) }
            ?: value.fee?.let { convertFee(id = value.id, fee = it) }
            ?: value.collateral?.let { convertCollateral(id = value.id, collateral = it) }
            ?: run {
                TangemLogger.e("unknown type of transaction: $value")
                null
            }
    }

    private fun convertSpend(id: String, spend: TangemPayTxHistoryResponse.Spend): TangemPayTxHistoryItem.Spend {
        val rawDate = if (spend.amount.signum() < 0) {
            spend.postedAt ?: spend.authorizedAt
        } else {
            spend.authorizedAt
        }
        return TangemPayTxHistoryItem.Spend(
            id = id,
            jsonRepresentation = spendAdapter.toJson(spend),
            date = rawDate.withLocalZone(),
            amount = spend.amount,
            currency = Currency.getInstance(spend.currency),
            authorizedAmount = spend.authorizedAmount.orZero(),
            localAmount = spend.localAmount,
            localCurrency = spend.localCurrency?.let(Currency::getInstance),
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
    private fun convertRefund(id: String, refund: TangemPayTxHistoryResponse.Refund): TangemPayTxHistoryItem.Spend {
        return TangemPayTxHistoryItem.Spend(
            id = id,
            jsonRepresentation = refundAdapter.toJson(refund),
            date = (refund.postedAt ?: refund.authorizedAt).withLocalZone(),
            amount = refund.amount.abs().negate(),
            currency = Currency.getInstance(refund.currency),
            authorizedAmount = BigDecimal.ZERO,
            localAmount = refund.localAmount?.abs()?.negate(),
            localCurrency = refund.localCurrency?.let(Currency::getInstance),
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
        payment: TangemPayTxHistoryResponse.Payment,
    ): TangemPayTxHistoryItem.Payment {
        return TangemPayTxHistoryItem.Payment(
            id = id,
            jsonRepresentation = paymentAdapter.toJson(payment),
            date = payment.postedAt.withLocalZone(),
            currency = Currency.getInstance(payment.currency),
            amount = payment.amount,
            transactionHash = payment.transactionHash,
        )
    }

    private fun convertFee(id: String, fee: TangemPayTxHistoryResponse.Fee): TangemPayTxHistoryItem.Fee {
        return TangemPayTxHistoryItem.Fee(
            id = id,
            jsonRepresentation = feeAdapter.toJson(fee),
            date = fee.postedAt.withLocalZone(),
            currency = Currency.getInstance(fee.currency),
            amount = fee.amount,
            description = fee.description,
        )
    }

    private fun convertCollateral(
        id: String,
        collateral: TangemPayTxHistoryResponse.Collateral,
    ): TangemPayTxHistoryItem.Collateral? {
        val date = collateral.postedAt ?: return run {
            TangemLogger.e("Collateral transaction postedAt is null: $collateral")
            return@run null
        }
        return TangemPayTxHistoryItem.Collateral(
            id = id,
            jsonRepresentation = collateralAdapter.toJson(collateral),
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