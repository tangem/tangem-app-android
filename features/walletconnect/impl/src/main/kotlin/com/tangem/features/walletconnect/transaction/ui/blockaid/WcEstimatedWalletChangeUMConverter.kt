package com.tangem.features.walletconnect.transaction.ui.blockaid

import com.domain.blockaid.models.transaction.simultation.AmountInfo
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.features.walletconnect.transaction.entity.blockaid.WcEstimatedWalletChangeUM
import com.tangem.utils.converter.Converter
import javax.inject.Inject

internal class WcEstimatedWalletChangeUMConverter @Inject constructor() :
    Converter<WcEstimatedWalletChangeUMConverter.Input, WcEstimatedWalletChangeUM> {

    override fun convert(value: Input): WcEstimatedWalletChangeUM {
        return when (val amountInfo = value.amountInfo) {
            is AmountInfo.FungibleTokens -> WcEstimatedWalletChangeUM(
                iconRes = value.iconRes,
                title = resourceReference(value.titleRes),
                description = "${value.sign} ${amountInfo.amountText()} ${amountInfo.token.symbol}",
                tokenIconUrl = amountInfo.token.logoUrl,
            )
            is AmountInfo.NonFungibleTokens -> WcEstimatedWalletChangeUM(
                iconRes = value.iconRes,
                title = resourceReference(value.titleRes),
                description = amountInfo.name,
                tokenIconUrl = amountInfo.logoUrl,
            )
        }
    }

    private fun AmountInfo.FungibleTokens.amountText() = amount.format { crypto("", token.decimals) }

    data class Input(
        val amountInfo: AmountInfo,
        val iconRes: Int,
        val titleRes: Int,
        val sign: String? = null,
    )
}