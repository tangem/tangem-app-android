package com.tangem.domain.walletmanager.utils

import com.tangem.domain.transaction.models.AssetRequirementsCondition
import com.tangem.utils.converter.Converter
import com.tangem.blockchain.common.trustlines.AssetRequirementsCondition as SdkRequirementsCondition

internal class SdkRequirementsConditionConverter : Converter<SdkRequirementsCondition, AssetRequirementsCondition> {
    override fun convert(value: SdkRequirementsCondition): AssetRequirementsCondition {
        return when (value) {
            SdkRequirementsCondition.PaidTransaction -> AssetRequirementsCondition.PaidTransaction
            is SdkRequirementsCondition.PaidTransactionWithFee -> AssetRequirementsCondition.PaidTransactionWithFee(
                feeAmount = requireNotNull(value.feeAmount.value),
                feeCurrencySymbol = value.feeAmount.currencySymbol,
                decimals = value.feeAmount.decimals,
            )
            is SdkRequirementsCondition.IncompleteTransaction -> TODO()
        }
    }
}
