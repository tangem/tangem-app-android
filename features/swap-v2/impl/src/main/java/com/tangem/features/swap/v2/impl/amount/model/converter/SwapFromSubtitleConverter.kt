package com.tangem.features.swap.v2.impl.amount.model.converter

import com.tangem.core.ui.components.atoms.text.TextEllipsis
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.swap.v2.impl.R
import com.tangem.features.swap.v2.impl.amount.entity.SendSubtitleUM
import java.math.BigDecimal

/**
 * Computes all subtitle fields for the **From** (primary) field.
 *
 * | State                          | subtitleLeft                      | subtitleRight | sendSubtitle            |
 * |--------------------------------|-----------------------------------|---------------|-------------------------|
 * | Entering (float), any          | "Balance: {balance}" masked       | EMPTY         | null                    |
 * | Viewing (fixed), empty         | "Balance: {balance}" masked       | EMPTY         | null                    |
 * | Viewing (fixed), not empty     | "Balance: {balance}" masked       | EMPTY         | SendSubtitleUM(...)     |
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

        val subtitleLeft = combinedReference(
            resourceReference(R.string.common_balance, wrappedList("")),
            stringReference(balance).orMaskWithStars(isBalanceHidden),
        )

        val sendSubtitle = if (!isEntering && !isAmountEmpty) {
            SendSubtitleUM(
                label = resourceReference(R.string.common_send_colon),
                value = stringReference(displayStr).orMaskWithStars(isBalanceHidden),
                valueEllipsis = TextEllipsis.OffsetEnd(symbol.length),
            )
        } else {
            null
        }

        return SwapSubtitleResult(
            subtitleLeft = subtitleLeft,
            subtitleRight = TextReference.EMPTY,
            subtitleEllipsisLeft = TextEllipsis.OffsetEnd(symbol.length),
            subtitleEllipsisRight = TextEllipsis.OffsetEnd(symbol.length),
            sendSubtitle = sendSubtitle,
        )
    }
}