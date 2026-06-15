package com.tangem.features.walletconnect.transaction.converter

import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.walletconnect.model.WcPsbtOutput
import com.tangem.features.walletconnect.impl.R
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionRequestBlockUM
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionRequestInfoItemUM
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal
import javax.inject.Inject

/**
 * Builds transaction request details blocks (recipient + amount per output) for a Bitcoin `signPsbt` request.
 *
 */
internal class WcSignPsbtRequestInfoConverter @Inject constructor() :
    Converter<WcSignPsbtRequestInfoConverter.Input, List<WcTransactionRequestBlockUM>> {

    override fun convert(value: Input): List<WcTransactionRequestBlockUM> {
        val outputs = value.outputs
        if (outputs.isEmpty()) return emptyList()
        val shouldShowIndex = outputs.size > 1
        return outputs.mapIndexed { index, output ->
            val recipientTitle = when {
                output.address == null -> resourceReference(R.string.common_no_address)
                shouldShowIndex -> combinedReference(
                    resourceReference(R.string.common_to),
                    stringReference(" ${index + 1}"),
                )
                else -> resourceReference(R.string.common_to)
            }
            WcTransactionRequestBlockUM(
                info = buildList {
                    add(
                        WcTransactionRequestInfoItemUM(
                            title = recipientTitle,
                            description = output.address.orEmpty(),
                        ),
                    )
                    add(
                        WcTransactionRequestInfoItemUM(
                            title = resourceReference(R.string.common_amount),
                            description = formatAmount(output.amountSatoshi, value.decimals, value.symbol),
                        ),
                    )
                }.toImmutableList(),
            )
        }
    }

    private fun formatAmount(amountSatoshi: Long, decimals: Int, symbol: String): String {
        val value = BigDecimal.valueOf(amountSatoshi)
            .movePointLeft(decimals)
            .stripTrailingZeros()
        return "${value.toPlainString()} $symbol"
    }

    data class Input(
        val outputs: List<WcPsbtOutput>,
        val decimals: Int,
        val symbol: String,
    )
}