package com.tangem.features.swap.v2.impl.amount.model.converter

import com.tangem.core.ui.components.atoms.text.TextEllipsis
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.swap.v2.impl.R
import com.tangem.utils.StringsSigns.TILDE_SIGN
import java.math.BigDecimal

/**
 * Computes all subtitle fields for the **To** (secondary) field.
 *
 * | State                          | subtitleLeft                       | subtitleRight              | ellipsisLeft           |
 * |--------------------------------|------------------------------------|----------------------------|------------------------|
 * | Viewing (float), empty         | "Will be sent to recipient"        | EMPTY                      | End                    |
 * | Viewing (float), not empty     | "Recipient gets" (empty param)     | "~{displayStr}" masked     | End                    |
 * | Entering (fixed), any          | "Will be sent to recipient"        | EMPTY                      | End                    |
 */
internal object SwapToSubtitleConverter {

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
                subtitleLeft = resourceReference(R.string.send_amount_receive_token_subtitle)
                subtitleRight = TextReference.EMPTY
                ellipsisLeft = TextEllipsis.End
            }
            !isEntering && isAmountEmpty -> {
                subtitleLeft = resourceReference(R.string.send_amount_receive_token_subtitle)
                subtitleRight = TextReference.EMPTY
                ellipsisLeft = TextEllipsis.End
            }
            else -> {
                subtitleLeft = resourceReference(
                    R.string.send_with_swap_recipient_get_amount,
                    wrappedList(""),
                )
                subtitleRight = combinedReference(
                    stringReference(TILDE_SIGN),
                    stringReference(displayStr),
                ).orMaskWithStars(isBalanceHidden)
                ellipsisLeft = TextEllipsis.End
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