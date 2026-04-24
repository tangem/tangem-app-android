package com.tangem.features.swap.v2.impl.amount.model.transformers

import com.tangem.common.ui.amountScreen.converters.field.AmountFieldQuoteTransformer
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.amountScreen.models.EnterAmountBoundary
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.swap.models.SwapAmountType
import com.tangem.features.swap.v2.impl.R
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountFieldUM
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountUM
import com.tangem.features.swap.v2.impl.amount.model.SwapAmountQuoteUtils.calculatePriceImpact
import com.tangem.features.swap.v2.impl.amount.model.converter.SwapAmountErrorConverter
import com.tangem.features.swap.v2.impl.amount.model.converter.SwapAmountUpdateSubtitleConverter
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM
import com.tangem.features.swap.v2.impl.common.isRestrictedByFCA
import com.tangem.utils.transformer.Transformer
import java.math.BigDecimal

@Suppress("LongParameterList")
internal class SwapAmountSelectQuoteTransformer(
    private val quoteUM: SwapQuoteUM,
    private val secondaryMaximumAmountBoundary: EnterAmountBoundary?,
    private val secondaryMinimumAmountBoundary: EnterAmountBoundary?,
    private val isNeedApplyFCARestrictions: Boolean,
    private val isBalanceHidden: Boolean,
    private val primaryMaximumAmountBoundary: EnterAmountBoundary? = null,
    private val primaryMinimumAmountBoundary: EnterAmountBoundary? = null,
    private val primaryFiatRateUSD: BigDecimal?,
    private val secondaryFiatRateUSD: BigDecimal?,
) : Transformer<SwapAmountUM> {

    override fun transform(prevState: SwapAmountUM): SwapAmountUM {
        if (prevState !is SwapAmountUM.Content) return prevState

        val quoteContent = quoteUM as? SwapQuoteUM.Content

        val subtitleConverter = SwapAmountUpdateSubtitleConverter(
            selectedAmountType = prevState.selectedAmountType,
            isBalanceHidden = isBalanceHidden,
        )

        val newPrimaryAmount = getPrimaryAmount(
            prevState = prevState,
            quoteContent = quoteContent,
            subtitleConverter = subtitleConverter,
        )

        val newSecondaryAmount = getSecondaryAmount(
            prevState = prevState,
            quoteContent = quoteContent,
            subtitleConverter = subtitleConverter,
        )

        val priceImpact = calculatePriceImpact(
            quoteContent = quoteContent,
            swapDirection = prevState.swapDirection,
            primaryFiatRateUSD = primaryFiatRateUSD,
            secondaryFiatRateUSD = secondaryFiatRateUSD,
            primaryCryptoCurrencyStatus = prevState.primaryCryptoCurrencyStatus,
            secondaryCryptoCurrencyStatus = prevState.secondaryCryptoCurrencyStatus,
        )

        val hasInsufficientFundsForFixed = hasInsufficientFundsForFixed(prevState, quoteContent)

        return prevState.copy(
            isPrimaryButtonEnabled = quoteUM is SwapQuoteUM.Content && !hasInsufficientFundsForFixed,
            selectedQuote = quoteUM,
            isShowFCAWarning = isNeedApplyFCARestrictions && quoteUM.provider?.isRestrictedByFCA() == true,
            primaryAmount = newPrimaryAmount,
            priceImpact = priceImpact,
            secondaryAmount = newSecondaryAmount,
        )
    }

    private fun hasInsufficientFundsForFixed(
        prevState: SwapAmountUM.Content,
        quoteContent: SwapQuoteUM.Content?,
    ): Boolean {
        if (prevState.selectedAmountType != SwapAmountType.To) return false
        val fromAmount = quoteContent?.fromAmount ?: return false
        val primaryBalance = prevState.primaryCryptoCurrencyStatus.value.amount ?: return false
        return fromAmount > primaryBalance
    }

    private fun getPrimaryAmount(
        prevState: SwapAmountUM.Content,
        quoteContent: SwapQuoteUM.Content?,
        subtitleConverter: SwapAmountUpdateSubtitleConverter,
    ): SwapAmountFieldUM {
        val isPrimarySelected = prevState.selectedAmountType == SwapAmountType.From
        val primarySwapAmountField = prevState.primaryAmount as? SwapAmountFieldUM.Content

        val primaryProviderErrorConverter = SwapAmountErrorConverter(
            cryptoCurrency = prevState.primaryCryptoCurrencyStatus.currency,
        )
        val fromAmount = quoteContent?.fromAmount
        return when {
            fromAmount != null && primaryMaximumAmountBoundary != null -> {
                primarySwapAmountField?.let { fromField ->
                    subtitleConverter.updateSubtitles(
                        field = fromField,
                        cryptoCurrencyStatus = prevState.primaryCryptoCurrencyStatus,
                        isAmountEmpty = false,
                        displayAmount = fromAmount,
                    ).copy(
                        amountField = AmountFieldQuoteTransformer(
                            cryptoCurrencyStatus = prevState.primaryCryptoCurrencyStatus,
                            cryptoAmount = fromAmount,
                            maxEnterAmount = primaryMaximumAmountBoundary,
                            minimumTransactionAmount = primaryMinimumAmountBoundary,
                        ).transform(fromField.amountField),
                    )
                } ?: prevState.primaryAmount
            }
            isPrimarySelected -> {
                val amountField = primarySwapAmountField?.amountField as? AmountState.Data
                val amountError = (quoteUM as? SwapQuoteUM.Error)?.expressError
                    ?.let(primaryProviderErrorConverter::convert)
                primarySwapAmountField?.copy(
                    amountField = amountField?.copy(
                        amountTextField = amountField.amountTextField.copy(
                            error = amountError ?: TextReference.EMPTY,
                            isError = amountError != null,
                        ),
                    ) ?: primarySwapAmountField.amountField,
                ) ?: prevState.primaryAmount
            }
            !isPrimarySelected && primarySwapAmountField != null && primaryMaximumAmountBoundary != null -> {
                subtitleConverter.updateSubtitles(
                    field = primarySwapAmountField,
                    cryptoCurrencyStatus = prevState.primaryCryptoCurrencyStatus,
                    isAmountEmpty = true,
                ).copy(
                    amountField = AmountFieldQuoteTransformer(
                        cryptoCurrencyStatus = prevState.primaryCryptoCurrencyStatus,
                        cryptoAmount = null,
                        maxEnterAmount = primaryMaximumAmountBoundary,
                        minimumTransactionAmount = primaryMinimumAmountBoundary,
                    ).transform(primarySwapAmountField.amountField),
                )
            }
            else -> prevState.primaryAmount
        }
    }

    @Suppress("CyclomaticComplexMethod")
    private fun getSecondaryAmount(
        prevState: SwapAmountUM.Content,
        quoteContent: SwapQuoteUM.Content?,
        subtitleConverter: SwapAmountUpdateSubtitleConverter,
    ): SwapAmountFieldUM {
        val isSecondarySelected = prevState.selectedAmountType == SwapAmountType.To
        val secondaryProviderErrorConverter = prevState.secondaryCryptoCurrencyStatus?.let {
            SwapAmountErrorConverter(cryptoCurrency = it.currency)
        }
        val fromAmount = quoteContent?.fromAmount
        val toAmount = quoteContent?.toAmount
        val secondarySwapAmountField = prevState.secondaryAmount as? SwapAmountFieldUM.Content
        return if (
            prevState.secondaryCryptoCurrencyStatus != null &&
            secondaryMaximumAmountBoundary != null &&
            secondarySwapAmountField != null
        ) {
            val isAmountEmpty = toAmount == null
            val secondaryAmountError = if (isSecondarySelected) {
                (quoteUM as? SwapQuoteUM.Error)?.expressError
                    ?.let { secondaryProviderErrorConverter?.convert(it) }
            } else {
                null
            }
            val transformedSecondaryAmountField = if (isSecondarySelected && toAmount == null) {
                secondarySwapAmountField.amountField
            } else {
                AmountFieldQuoteTransformer(
                    cryptoCurrencyStatus = prevState.secondaryCryptoCurrencyStatus,
                    cryptoAmount = toAmount,
                    maxEnterAmount = secondaryMaximumAmountBoundary,
                    minimumTransactionAmount = secondaryMinimumAmountBoundary,
                ).transform(secondarySwapAmountField.amountField)
            }
            val insufficientFundsError = if (isSecondarySelected && fromAmount != null) {
                val primaryBalance = prevState.primaryCryptoCurrencyStatus.value.amount
                if (primaryBalance != null && fromAmount > primaryBalance) {
                    resourceReference(R.string.common_insufficient_balance)
                } else {
                    null
                }
            } else {
                null
            }
            val effectiveSecondaryError = secondaryAmountError ?: insufficientFundsError
            val secondaryAmountFieldWithError = if (isSecondarySelected) {
                (transformedSecondaryAmountField as? AmountState.Data)?.let { data ->
                    data.copy(
                        amountTextField = data.amountTextField.copy(
                            error = effectiveSecondaryError ?: TextReference.EMPTY,
                            isError = effectiveSecondaryError != null,
                        ),
                    )
                } ?: transformedSecondaryAmountField
            } else {
                transformedSecondaryAmountField
            }
            subtitleConverter.updateSubtitles(
                field = secondarySwapAmountField,
                cryptoCurrencyStatus = prevState.secondaryCryptoCurrencyStatus,
                isAmountEmpty = isAmountEmpty,
                displayAmount = toAmount,
            ).copy(
                amountField = secondaryAmountFieldWithError,
            )
        } else {
            prevState.secondaryAmount
        }
    }
}