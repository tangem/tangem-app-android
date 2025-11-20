package com.tangem.data.visa.utils

import com.squareup.moshi.Moshi
import com.tangem.datasource.api.pay.models.response.TangemPayTxHistoryResponse
import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import com.tangem.utils.converter.Converter
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import timber.log.Timber
import java.util.Currency

internal class TangemPayTxHistoryItemConverter(moshi: Moshi) :
    Converter<TangemPayTxHistoryResponse.Transaction, TangemPayTxHistoryItem?> {

    private val spendAdapter by lazy { moshi.adapter(TangemPayTxHistoryResponse.Spend::class.java) }
    private val paymentAdapter by lazy { moshi.adapter(TangemPayTxHistoryResponse.Payment::class.java) }
    private val feeAdapter by lazy { moshi.adapter(TangemPayTxHistoryResponse.Fee::class.java) }
    private val collateralAdapter by lazy { moshi.adapter(TangemPayTxHistoryResponse.Collateral::class.java) }

    override fun convert(value: TangemPayTxHistoryResponse.Transaction): TangemPayTxHistoryItem? {
        return value.spend?.let { convertSpend(id = value.id, spend = it) }
            ?: value.payment?.let { convertPayment(id = value.id, payment = it) }
            ?: value.fee?.let { convertFee(id = value.id, fee = it) }
            ?: value.collateral?.let { convertCollateral(id = value.id, collateral = it) }
            ?: run {
                Timber.wtf("unknown type of transaction: $value")
                null
            }
    }

    private fun convertSpend(id: String, spend: TangemPayTxHistoryResponse.Spend): TangemPayTxHistoryItem.Spend {
        return TangemPayTxHistoryItem.Spend(
            id = id,
            jsonRepresentation = spendAdapter.toJson(spend),
            date = spend.authorizedAt.withLocalZone(),
            amount = spend.amount,
            currency = Currency.getInstance(spend.currency),
            localAmount = spend.localAmount,
            localCurrency = spend.localCurrency?.let(Currency::getInstance),
            enrichedMerchantName = spend.enrichedMerchantName,
            merchantName = spend.merchantName,
            enrichedMerchantCategory = spend.enrichedMerchantCategory,
            merchantCategory = spend.merchantCategory,
            status = TangemPayTxHistoryItemStatusConverter.convert(spend.status),
            enrichedMerchantIconUrl = spend.enrichedMerchantIcon,
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
            Timber.e("Collateral transaction postedAt is null: $collateral")
            return@run null
        }
        return TangemPayTxHistoryItem.Collateral(
            id = id,
            jsonRepresentation = collateralAdapter.toJson(collateral),
            date = date.withLocalZone(),
            currency = Currency.getInstance("usd"),
            amount = collateral.amount,
            transactionHash = collateral.transactionHash,
        )
    }

    private fun DateTime.withLocalZone(): DateTime {
        return withZone(DateTimeZone.getDefault())
    }
}