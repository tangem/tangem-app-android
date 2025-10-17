package com.tangem.data.visa.utils

import com.squareup.moshi.Moshi
import com.tangem.datasource.api.pay.models.response.TangemPayTxHistoryResponse
import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import com.tangem.utils.converter.Converter
import timber.log.Timber
import java.util.Currency

internal class TangemPayTxHistoryItemConverter(moshi: Moshi) :
    Converter<TangemPayTxHistoryResponse.Transaction, TangemPayTxHistoryItem?> {

    private val spendAdapter = moshi.adapter(TangemPayTxHistoryResponse.Spend::class.java)
    private val paymentAdapter = moshi.adapter(TangemPayTxHistoryResponse.Payment::class.java)
    private val feeAdapter = moshi.adapter(TangemPayTxHistoryResponse.Fee::class.java)

    override fun convert(value: TangemPayTxHistoryResponse.Transaction): TangemPayTxHistoryItem? {
        return value.spend?.let { convertSpend(id = value.id, spend = it) }
            ?: value.payment?.let { convertPayment(id = value.id, payment = it) }
            ?: value.fee?.let { convertFee(id = value.id, fee = it) }
            ?: run {
                Timber.wtf("unknown type of transaction: $value")
                null
            }
    }

    private fun convertSpend(id: String, spend: TangemPayTxHistoryResponse.Spend): TangemPayTxHistoryItem.Spend {
        return TangemPayTxHistoryItem.Spend(
            id = id,
            jsonRepresentation = spendAdapter.toJson(spend),
            // If postedAt is null, it means transaction wasn't posted and was likely declined. Use authorizedAt
            date = spend.postedAt ?: spend.authorizedAt,
            amount = spend.amount,
            currency = Currency.getInstance(spend.currency),
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
            date = payment.postedAt,
            currency = Currency.getInstance(payment.currency),
            amount = payment.amount,
            transactionHash = payment.transactionHash,
        )
    }

    private fun convertFee(id: String, fee: TangemPayTxHistoryResponse.Fee): TangemPayTxHistoryItem.Fee {
        return TangemPayTxHistoryItem.Fee(
            id = id,
            jsonRepresentation = feeAdapter.toJson(fee),
            date = fee.postedAt,
            currency = Currency.getInstance(fee.currency),
            amount = fee.amount,
        )
    }
}