package com.tangem.features.swap.v2.impl.amount.model.converter

import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.swap.models.SwapAmountType
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountFieldUM
import java.math.BigDecimal

/**
 * Updates all subtitle fields on an existing [SwapAmountFieldUM.Content] field.
 *
 * Delegates to [SwapFromSubtitleConverter] / [SwapToSubtitleConverter] based on the field's
 * [SwapAmountFieldUM.Content.amountType]. All other field properties are preserved.
 *
 * `isEntering` is derived internally: the field is "entering" when its amountType matches
 * [selectedAmountType], so callers cannot accidentally pass a wrong value.
 *
 * @param selectedAmountType the currently selected amount type from [SwapAmountUM.Content]
 * @param isBalanceHidden whether balance values should be masked
 */
internal class SwapAmountUpdateSubtitleConverter(
    private val selectedAmountType: SwapAmountType,
    private val isBalanceHidden: Boolean,
) {

    /**
     * Updates all subtitle fields after a quote arrives.
     *
     * @param field the current field content to update
     * @param cryptoCurrencyStatus status used for balance formatting
     * @param isAmountEmpty whether the amount in this field is empty/null
     * @param displayAmount optional override for the displayed amount (e.g. quote toAmount/fromAmount)
     */
    fun updateSubtitles(
        field: SwapAmountFieldUM.Content,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        isAmountEmpty: Boolean,
        displayAmount: BigDecimal? = null,
    ): SwapAmountFieldUM.Content {
        val isEntering = field.amountType == selectedAmountType
        val subtitles = when (field.amountType) {
            SwapAmountType.From -> SwapFromSubtitleConverter.convert(
                cryptoCurrencyStatus = cryptoCurrencyStatus,
                isBalanceHidden = isBalanceHidden,
                isEntering = isEntering,
                isAmountEmpty = isAmountEmpty,
                displayAmount = displayAmount,
            )
            SwapAmountType.To -> SwapToSubtitleConverter.convert(
                cryptoCurrencyStatus = cryptoCurrencyStatus,
                isBalanceHidden = isBalanceHidden,
                isEntering = isEntering,
                isAmountEmpty = isAmountEmpty,
                displayAmount = displayAmount,
            )
        }
        return field.copy(
            subtitleLeft = subtitles.subtitleLeft,
            subtitleEllipsisLeft = subtitles.subtitleEllipsisLeft,
            subtitleRight = subtitles.subtitleRight,
            subtitleEllipsisRight = subtitles.subtitleEllipsisRight,
        )
    }
}