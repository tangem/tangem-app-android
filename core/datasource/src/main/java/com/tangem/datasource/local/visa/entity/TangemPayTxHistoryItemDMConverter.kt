package com.tangem.datasource.local.visa.entity

import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import com.tangem.utils.converter.Converter
import javax.inject.Inject

internal class TangemPayTxHistoryItemToDMConverter @Inject constructor() :
    Converter<TangemPayTxHistoryItem, TangemPayTxHistoryItemDM> {

    override fun convert(value: TangemPayTxHistoryItem): TangemPayTxHistoryItemDM = when (value) {
        is TangemPayTxHistoryItem.Spend -> TangemPayTxHistoryItemDM.Spend(
            id = value.id,
            jsonRepresentation = value.jsonRepresentation,
            date = value.date,
            amount = value.amount,
            currency = value.currency,
            authorizedAmount = value.authorizedAmount,
            localAmount = value.localAmount,
            localCurrency = value.localCurrency,
            enrichedMerchantName = value.enrichedMerchantName,
            merchantName = value.merchantName,
            enrichedMerchantCategory = value.enrichedMerchantCategory,
            merchantCategoryCode = value.merchantCategoryCode,
            merchantCategory = value.merchantCategory,
            status = value.status.toDM(),
            enrichedMerchantIconUrl = value.enrichedMerchantIconUrl,
            declinedReason = value.declinedReason,
        )
        is TangemPayTxHistoryItem.Payment -> TangemPayTxHistoryItemDM.Payment(
            id = value.id,
            jsonRepresentation = value.jsonRepresentation,
            date = value.date,
            amount = value.amount,
            currency = value.currency,
            transactionHash = value.transactionHash,
        )
        is TangemPayTxHistoryItem.Fee -> TangemPayTxHistoryItemDM.Fee(
            id = value.id,
            jsonRepresentation = value.jsonRepresentation,
            date = value.date,
            amount = value.amount,
            currency = value.currency,
            description = value.description,
        )
        is TangemPayTxHistoryItem.Collateral -> TangemPayTxHistoryItemDM.Collateral(
            id = value.id,
            jsonRepresentation = value.jsonRepresentation,
            date = value.date,
            amount = value.amount,
            currency = value.currency,
            transactionHash = value.transactionHash,
            type = value.type.toDM(),
        )
    }
}

internal class TangemPayTxHistoryItemToDomainConverter @Inject constructor() :
    Converter<TangemPayTxHistoryItemDM, TangemPayTxHistoryItem> {

    override fun convert(value: TangemPayTxHistoryItemDM): TangemPayTxHistoryItem = when (value) {
        is TangemPayTxHistoryItemDM.Spend -> TangemPayTxHistoryItem.Spend(
            id = value.id,
            jsonRepresentation = value.jsonRepresentation,
            date = value.date,
            amount = value.amount,
            currency = value.currency,
            authorizedAmount = value.authorizedAmount,
            localAmount = value.localAmount,
            localCurrency = value.localCurrency,
            enrichedMerchantName = value.enrichedMerchantName,
            merchantName = value.merchantName,
            enrichedMerchantCategory = value.enrichedMerchantCategory,
            merchantCategoryCode = value.merchantCategoryCode,
            merchantCategory = value.merchantCategory,
            status = value.status.toDomain(),
            enrichedMerchantIconUrl = value.enrichedMerchantIconUrl,
            declinedReason = value.declinedReason,
        )
        is TangemPayTxHistoryItemDM.Payment -> TangemPayTxHistoryItem.Payment(
            id = value.id,
            jsonRepresentation = value.jsonRepresentation,
            date = value.date,
            amount = value.amount,
            currency = value.currency,
            transactionHash = value.transactionHash,
        )
        is TangemPayTxHistoryItemDM.Fee -> TangemPayTxHistoryItem.Fee(
            id = value.id,
            jsonRepresentation = value.jsonRepresentation,
            date = value.date,
            amount = value.amount,
            currency = value.currency,
            description = value.description,
        )
        is TangemPayTxHistoryItemDM.Collateral -> TangemPayTxHistoryItem.Collateral(
            id = value.id,
            jsonRepresentation = value.jsonRepresentation,
            date = value.date,
            amount = value.amount,
            currency = value.currency,
            transactionHash = value.transactionHash,
            type = value.type.toDomain(),
        )
    }
}

private fun TangemPayTxHistoryItem.Status.toDM(): TangemPayTxHistoryItemDM.Status = when (this) {
    TangemPayTxHistoryItem.Status.PENDING -> TangemPayTxHistoryItemDM.Status.PENDING
    TangemPayTxHistoryItem.Status.RESERVED -> TangemPayTxHistoryItemDM.Status.RESERVED
    TangemPayTxHistoryItem.Status.COMPLETED -> TangemPayTxHistoryItemDM.Status.COMPLETED
    TangemPayTxHistoryItem.Status.DECLINED -> TangemPayTxHistoryItemDM.Status.DECLINED
    TangemPayTxHistoryItem.Status.REVERSED -> TangemPayTxHistoryItemDM.Status.REVERSED
    TangemPayTxHistoryItem.Status.UNKNOWN -> TangemPayTxHistoryItemDM.Status.UNKNOWN
}

private fun TangemPayTxHistoryItemDM.Status.toDomain(): TangemPayTxHistoryItem.Status = when (this) {
    TangemPayTxHistoryItemDM.Status.PENDING -> TangemPayTxHistoryItem.Status.PENDING
    TangemPayTxHistoryItemDM.Status.RESERVED -> TangemPayTxHistoryItem.Status.RESERVED
    TangemPayTxHistoryItemDM.Status.COMPLETED -> TangemPayTxHistoryItem.Status.COMPLETED
    TangemPayTxHistoryItemDM.Status.DECLINED -> TangemPayTxHistoryItem.Status.DECLINED
    TangemPayTxHistoryItemDM.Status.REVERSED -> TangemPayTxHistoryItem.Status.REVERSED
    TangemPayTxHistoryItemDM.Status.UNKNOWN -> TangemPayTxHistoryItem.Status.UNKNOWN
}

private fun TangemPayTxHistoryItem.Type.toDM(): TangemPayTxHistoryItemDM.Type = when (this) {
    TangemPayTxHistoryItem.Type.Deposit -> TangemPayTxHistoryItemDM.Type.Deposit
    TangemPayTxHistoryItem.Type.Withdrawal -> TangemPayTxHistoryItemDM.Type.Withdrawal
}

private fun TangemPayTxHistoryItemDM.Type.toDomain(): TangemPayTxHistoryItem.Type = when (this) {
    TangemPayTxHistoryItemDM.Type.Deposit -> TangemPayTxHistoryItem.Type.Deposit
    TangemPayTxHistoryItemDM.Type.Withdrawal -> TangemPayTxHistoryItem.Type.Withdrawal
}