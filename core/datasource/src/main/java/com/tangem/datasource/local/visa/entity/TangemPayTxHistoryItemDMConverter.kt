package com.tangem.datasource.local.visa.entity

import com.tangem.datasource.local.visa.entity.TangemPayTxHistoryItemDM.Cashback as CashbackDM
import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import com.tangem.domain.visa.model.TangemPayTxHistoryItem.Cashback as CashbackDomain
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
            cashback = value.cashback?.toDM(),
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

    private fun TangemPayTxHistoryItem.Status.toDM(): TangemPayTxHistoryItemDM.Status = when (this) {
        TangemPayTxHistoryItem.Status.PENDING -> TangemPayTxHistoryItemDM.Status.PENDING
        TangemPayTxHistoryItem.Status.RESERVED -> TangemPayTxHistoryItemDM.Status.RESERVED
        TangemPayTxHistoryItem.Status.COMPLETED -> TangemPayTxHistoryItemDM.Status.COMPLETED
        TangemPayTxHistoryItem.Status.DECLINED -> TangemPayTxHistoryItemDM.Status.DECLINED
        TangemPayTxHistoryItem.Status.REVERSED -> TangemPayTxHistoryItemDM.Status.REVERSED
        TangemPayTxHistoryItem.Status.UNKNOWN -> TangemPayTxHistoryItemDM.Status.UNKNOWN
    }

    private fun TangemPayTxHistoryItem.Type.toDM(): TangemPayTxHistoryItemDM.Type = when (this) {
        TangemPayTxHistoryItem.Type.Deposit -> TangemPayTxHistoryItemDM.Type.Deposit
        TangemPayTxHistoryItem.Type.Withdrawal -> TangemPayTxHistoryItemDM.Type.Withdrawal
    }

    private fun CashbackDomain.toDM(): CashbackDM = CashbackDM(
        status = status.toDM(),
        amount = amount,
        currency = currency,
        isCapTrimmed = isCapTrimmed,
        exclusionReason = exclusionReason?.toDM(),
        promotionIds = promotionIds,
    )

    private fun CashbackDomain.Status.toDM(): CashbackDM.Status = when (this) {
        CashbackDomain.Status.ESTIMATED -> CashbackDM.Status.ESTIMATED
        CashbackDomain.Status.CONFIRMED -> CashbackDM.Status.CONFIRMED
        CashbackDomain.Status.EXCLUDED -> CashbackDM.Status.EXCLUDED
        CashbackDomain.Status.AWAITING_CALCULATION -> CashbackDM.Status.AWAITING_CALCULATION
        CashbackDomain.Status.UNKNOWN -> CashbackDM.Status.UNKNOWN
    }

    private fun CashbackDomain.ExclusionReason.toDM(): CashbackDM.ExclusionReason = when (this) {
        CashbackDomain.ExclusionReason.MCC_EXCLUDED -> CashbackDM.ExclusionReason.MCC_EXCLUDED
        CashbackDomain.ExclusionReason.MONTHLY_CAP_REACHED -> CashbackDM.ExclusionReason.MONTHLY_CAP_REACHED
        CashbackDomain.ExclusionReason.CUSTOMER_BLOCKLISTED -> CashbackDM.ExclusionReason.CUSTOMER_BLOCKLISTED
        CashbackDomain.ExclusionReason.MERCHANT_COUNTRY_EXCLUDED ->
            CashbackDM.ExclusionReason.MERCHANT_COUNTRY_EXCLUDED
        CashbackDomain.ExclusionReason.BELOW_MIN -> CashbackDM.ExclusionReason.BELOW_MIN
        CashbackDomain.ExclusionReason.UNKNOWN -> CashbackDM.ExclusionReason.UNKNOWN
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
            cashback = value.cashback?.toDomain(),
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

    private fun TangemPayTxHistoryItemDM.Status.toDomain(): TangemPayTxHistoryItem.Status = when (this) {
        TangemPayTxHistoryItemDM.Status.PENDING -> TangemPayTxHistoryItem.Status.PENDING
        TangemPayTxHistoryItemDM.Status.RESERVED -> TangemPayTxHistoryItem.Status.RESERVED
        TangemPayTxHistoryItemDM.Status.COMPLETED -> TangemPayTxHistoryItem.Status.COMPLETED
        TangemPayTxHistoryItemDM.Status.DECLINED -> TangemPayTxHistoryItem.Status.DECLINED
        TangemPayTxHistoryItemDM.Status.REVERSED -> TangemPayTxHistoryItem.Status.REVERSED
        TangemPayTxHistoryItemDM.Status.UNKNOWN -> TangemPayTxHistoryItem.Status.UNKNOWN
    }

    private fun TangemPayTxHistoryItemDM.Type.toDomain(): TangemPayTxHistoryItem.Type = when (this) {
        TangemPayTxHistoryItemDM.Type.Deposit -> TangemPayTxHistoryItem.Type.Deposit
        TangemPayTxHistoryItemDM.Type.Withdrawal -> TangemPayTxHistoryItem.Type.Withdrawal
    }

    private fun CashbackDM.toDomain(): CashbackDomain = CashbackDomain(
        status = status.toDomain(),
        amount = amount,
        currency = currency,
        isCapTrimmed = isCapTrimmed,
        exclusionReason = exclusionReason?.toDomain(),
        promotionIds = promotionIds,
    )

    private fun CashbackDM.Status.toDomain(): CashbackDomain.Status = when (this) {
        CashbackDM.Status.ESTIMATED -> CashbackDomain.Status.ESTIMATED
        CashbackDM.Status.CONFIRMED -> CashbackDomain.Status.CONFIRMED
        CashbackDM.Status.EXCLUDED -> CashbackDomain.Status.EXCLUDED
        CashbackDM.Status.AWAITING_CALCULATION -> CashbackDomain.Status.AWAITING_CALCULATION
        CashbackDM.Status.UNKNOWN -> CashbackDomain.Status.UNKNOWN
    }

    private fun CashbackDM.ExclusionReason.toDomain(): CashbackDomain.ExclusionReason = when (this) {
        CashbackDM.ExclusionReason.MCC_EXCLUDED -> CashbackDomain.ExclusionReason.MCC_EXCLUDED
        CashbackDM.ExclusionReason.MONTHLY_CAP_REACHED -> CashbackDomain.ExclusionReason.MONTHLY_CAP_REACHED
        CashbackDM.ExclusionReason.CUSTOMER_BLOCKLISTED -> CashbackDomain.ExclusionReason.CUSTOMER_BLOCKLISTED
        CashbackDM.ExclusionReason.MERCHANT_COUNTRY_EXCLUDED ->
            CashbackDomain.ExclusionReason.MERCHANT_COUNTRY_EXCLUDED
        CashbackDM.ExclusionReason.BELOW_MIN -> CashbackDomain.ExclusionReason.BELOW_MIN
        CashbackDM.ExclusionReason.UNKNOWN -> CashbackDomain.ExclusionReason.UNKNOWN
    }
}