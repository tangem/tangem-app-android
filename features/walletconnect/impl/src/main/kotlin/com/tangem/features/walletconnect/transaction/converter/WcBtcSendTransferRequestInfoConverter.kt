package com.tangem.features.walletconnect.transaction.converter

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.walletconnect.model.WcBitcoinMethod
import com.tangem.features.walletconnect.impl.R
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionRequestBlockUM
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionRequestInfoItemUM
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toImmutableList
import javax.inject.Inject

/**
 * Builds a transaction request details block (recipient and amount) for a Bitcoin `sendTransfer` WalletConnect request.
 *
 */
internal class WcBtcSendTransferRequestInfoConverter @Inject constructor() :
    Converter<WcBtcSendTransferRequestInfoConverter.Input, WcTransactionRequestBlockUM> {

    override fun convert(value: Input): WcTransactionRequestBlockUM {
        val method = value.method
        return WcTransactionRequestBlockUM(
            info = buildList {
                add(WcTransactionRequestInfoItemUM(resourceReference(R.string.common_from), method.account))
                add(WcTransactionRequestInfoItemUM(resourceReference(R.string.common_to), method.recipientAddress))
                add(
                    WcTransactionRequestInfoItemUM(
                        title = resourceReference(R.string.common_amount),
                        description = formatAmount(method.amount, value.decimals, value.symbol),
                    ),
                )
                val changeAddress = method.changeAddress
                if (!changeAddress.isNullOrEmpty()) {
                    add(WcTransactionRequestInfoItemUM(resourceReference(R.string.wc_change_address), changeAddress))
                }
            }.toImmutableList(),
        )
    }

    private fun formatAmount(amount: String, decimals: Int, symbol: String): String {
        val value = amount.toBigDecimalOrNull()
            ?.movePointLeft(decimals)
            ?.stripTrailingZeros()
            ?: return amount
        return "${value.toPlainString()} $symbol"
    }

    data class Input(
        val method: WcBitcoinMethod.SendTransfer,
        val decimals: Int,
        val symbol: String,
    )
}