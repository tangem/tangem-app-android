package com.tangem.data.walletmanager.utils

import com.tangem.domain.transaction.models.AssetRequirementsCondition
import com.tangem.utils.converter.Converter
import com.tangem.blockchain.common.trustlines.AssetRequirementsCondition as SdkRequirementsCondition

internal class SdkRequirementsConditionConverter : Converter<SdkRequirementsCondition, AssetRequirementsCondition> {
    override fun convert(value: SdkRequirementsCondition): AssetRequirementsCondition {
        return when (value) {
            is SdkRequirementsCondition.PaidTransaction -> AssetRequirementsCondition.PaidTransaction
            is SdkRequirementsCondition.RequiredTrustline -> AssetRequirementsCondition.RequiredTrustline(
                requiredAmount = requireNotNull(value.amount.value),
                currencySymbol = value.amount.currencySymbol,
                decimals = value.amount.decimals,
            )
            is SdkRequirementsCondition.PaidTransactionWithFee -> AssetRequirementsCondition.PaidTransactionWithFee(
                feeAmount = requireNotNull(value.feeAmount.value),
                feeCurrencySymbol = value.feeAmount.currencySymbol,
                decimals = value.feeAmount.decimals,
            )
            is SdkRequirementsCondition.IncompleteTransaction -> AssetRequirementsCondition.IncompleteTransaction(
                amount = requireNotNull(value.amount.value),
                currencySymbol = value.amount.currencySymbol,
                currencyDecimals = value.amount.decimals,
                feeAmount = requireNotNull(value.feeAmount.value),
                feeCurrencySymbol = value.feeAmount.currencySymbol,
                feeCurrencyDecimals = value.feeAmount.decimals,
            )
        }
    }
}