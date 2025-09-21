package com.tangem.data.visa.utils

import com.tangem.datasource.api.pay.models.response.TangemPayTxHistoryResponse
import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import com.tangem.utils.converter.Converter

internal object TangemPayTxHistoryItemConverter :
    Converter<TangemPayTxHistoryResponse.Transaction, TangemPayTxHistoryItem> {

    @Suppress("CyclomaticComplexMethod")
    override fun convert(value: TangemPayTxHistoryResponse.Transaction): TangemPayTxHistoryItem {
        val spend = value.spend
        val collateral = value.collateral
        val payment = value.payment
        val fee = value.fee

        return TangemPayTxHistoryItem(
            id = value.id,
            date = when {
                spend != null -> spend.postedAt
                collateral != null -> collateral.postedAt
                payment != null -> payment.postedAt
                fee != null -> fee.postedAt
                else -> null
            },
            amount = when {
                spend != null -> spend.amount
                collateral != null -> collateral.amount
                payment != null -> payment.amount
                fee != null -> fee.amount
                else -> null
            },
            merchantName = when {
                spend != null -> spend.merchantName
                else -> null
            },
            status = when {
                spend != null -> spend.status
                payment != null -> payment.status
                else -> null
            },
            currency = when {
                spend != null -> spend.currency
                collateral != null -> collateral.currency
                payment != null -> payment.currency
                fee != null -> fee.currency
                else -> null
            },
        )
    }
}