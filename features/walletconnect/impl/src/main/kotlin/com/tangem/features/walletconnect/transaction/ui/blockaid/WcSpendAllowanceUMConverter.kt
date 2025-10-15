package com.tangem.features.walletconnect.transaction.ui.blockaid

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.getActiveIconRes
import com.tangem.domain.walletconnect.model.WcApprovedAmount
import com.tangem.features.walletconnect.impl.R
import com.tangem.features.walletconnect.transaction.entity.approve.WcSpendAllowanceUM
import com.tangem.utils.converter.Converter

internal class WcSpendAllowanceUMConverter : Converter<WcSpendAllowanceUMConverter.Input, WcSpendAllowanceUM> {

    override fun convert(value: Input): WcSpendAllowanceUM {
        val amount = value.approvedAmount.amount?.value ?: 0.0.toBigDecimal()
        val isUnlimited = value.approvedAmount.amount?.value == null
        return WcSpendAllowanceUM(
            amountValue = amount,
            amountText = if (value.approvedAmount.amount?.value == null) {
                TextReference.Res(R.string.wc_common_unlimited)
            } else {
                TextReference.Str(amount.amountText())
            },
            isUnlimited = isUnlimited,
            tokenSymbol = value.approvedAmount.amount?.currencySymbol ?: "",
            tokenImageUrl = value.approvedAmount.logoUrl,
            networkIconRes = value.approvedAmount.chainId?.toString()?.let { getActiveIconRes(it) },
            onLearnMoreClicked = value.onLearnMoreClick,
        )
    }

    data class Input(val approvedAmount: WcApprovedAmount, val onLearnMoreClick: () -> Unit)
}