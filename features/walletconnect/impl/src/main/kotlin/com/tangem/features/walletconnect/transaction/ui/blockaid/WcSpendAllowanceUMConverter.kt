package com.tangem.features.walletconnect.transaction.ui.blockaid

import com.domain.blockaid.models.transaction.simultation.ApprovedAmount
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.walletconnect.impl.R
import com.tangem.features.walletconnect.transaction.entity.approve.WcSpendAllowanceUM
import com.tangem.utils.converter.Converter
import javax.inject.Inject

internal class WcSpendAllowanceUMConverter @Inject constructor() : Converter<ApprovedAmount, WcSpendAllowanceUM> {

    override fun convert(value: ApprovedAmount) = WcSpendAllowanceUM(
        amountText = if (value.isUnlimited) {
            TextReference.Res(R.string.wc_common_unlimited)
        } else {
            TextReference.Str(value.approvedAmount.amountText())
        },
        tokenSymbol = value.tokenInfo.symbol,
        tokenImageUrl = value.tokenInfo.logoUrl,
    )
}