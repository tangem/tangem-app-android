package com.tangem.features.walletconnect.transaction.ui.blockaid

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.getActiveIconRes
import com.tangem.domain.walletconnect.model.WcApprovedAmount
import com.tangem.features.walletconnect.impl.R
import com.tangem.features.walletconnect.transaction.entity.approve.WcSpendAllowanceUM
import com.tangem.utils.converter.Converter
import javax.inject.Inject

internal class WcSpendAllowanceUMConverter @Inject constructor() : Converter<WcApprovedAmount, WcSpendAllowanceUM> {

    override fun convert(value: WcApprovedAmount): WcSpendAllowanceUM {
        val amount = value.amount?.value ?: 0.0.toBigDecimal()
        val isUnlimited = value.amount?.value == null
        return WcSpendAllowanceUM(
            amountValue = amount,
            amountText = if (value.amount?.value == null) {
                TextReference.Res(R.string.wc_common_unlimited)
            } else {
                TextReference.Str(amount.amountText())
            },
            isUnlimited = isUnlimited,
            tokenSymbol = value.amount?.currencySymbol ?: "",
            tokenImageUrl = value.logoUrl,
            networkIconRes = value.chainId?.toString()?.let { getActiveIconRes(it) },
        )
    }
}