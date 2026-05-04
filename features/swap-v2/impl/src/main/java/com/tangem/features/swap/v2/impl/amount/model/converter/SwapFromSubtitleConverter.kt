package com.tangem.features.swap.v2.impl.amount.model.converter

import com.tangem.core.ui.components.atoms.text.TextEllipsis
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.swap.v2.impl.R
import com.tangem.utils.StringsSigns.DOT
import java.math.BigDecimal

/**
 * Computes all subtitle fields for the **From** (primary) field.
 *
 * | State                          | subtitleLeft                      | subtitleRight                     | ellipsisLeft           |
 * |--------------------------------|-----------------------------------|-----------------------------------|------------------------|
 * | Entering (float), any          | "Balance: " (empty param)         | "{balance}" masked                | OffsetEnd(symbol)      |
 * | Viewing (fixed), empty         | "Balance: " (empty param)         | "{balance}" masked (crypto only)  | End                    |
 * | Viewing (fixed), not empty     | "{balance}" masked               | "• Send {displayStr}" masked       | OffsetEnd(symbol)      |
 */
internal object SwapFromSubtitleConverter {

    fun convert(
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        isBalanceHidden: Boolean,
        isEntering: Boolean,
        isAmountEmpty: Boolean,
        displayAmount: BigDecimal?,
    ): SwapSubtitleResult {
        val symbol = cryptoCurrencyStatus.currency.symbol
        val balance = cryptoCurrencyStatus.value.amount.format {
            crypto(cryptoCurrency = cryptoCurrencyStatus.currency)
        }
        val displayStr = displayAmount?.format {
            crypto(cryptoCurrency = cryptoCurrencyStatus.currency)
        } ?: balance

        val subtitleLeft: TextReference
        val subtitleRight: TextReference
        val ellipsisLeft: TextEllipsis

        when {
            isEntering -> {
                subtitleLeft = resourceReference(R.string.common_balance, wrappedList(""))
                subtitleRight = combinedReference(stringReference(balance))
                    .orMaskWithStars(isBalanceHidden)
                ellipsisLeft = TextEllipsis.OffsetEnd(symbol.length)
            }
            !isEntering && isAmountEmpty -> {
                subtitleLeft = resourceReference(R.string.common_balance, wrappedList(""))
                subtitleRight = combinedReference(stringReference(balance))
                    .orMaskWithStars(isBalanceHidden)
                ellipsisLeft = TextEllipsis.End
            }
            else -> {
                subtitleLeft = stringReference(balance)
                    .orMaskWithStars(isBalanceHidden)
                subtitleRight = combinedReference(
                    stringReference("$DOT "),
                    resourceReference(R.string.common_send),
                    stringReference(" $displayStr"),
                ).orMaskWithStars(isBalanceHidden)
                ellipsisLeft = TextEllipsis.OffsetEnd(symbol.length)
            }
        }

        return SwapSubtitleResult(
            subtitleLeft = subtitleLeft,
            subtitleRight = subtitleRight,
            subtitleEllipsisLeft = ellipsisLeft,
            subtitleEllipsisRight = TextEllipsis.OffsetEnd(symbol.length),
        )
    }
}