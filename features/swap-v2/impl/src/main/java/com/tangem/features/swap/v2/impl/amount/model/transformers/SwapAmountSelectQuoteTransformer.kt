package com.tangem.features.swap.v2.impl.amount.model.transformers

import com.tangem.common.ui.amountScreen.converters.field.AmountFieldChangeTransformer
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.amountScreen.models.EnterAmountBoundary
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.domain.swap.models.SwapAmountType
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountFieldUM
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountUM
import com.tangem.features.swap.v2.impl.amount.model.SwapAmountQuoteUtils.calculatePriceImpact
import com.tangem.features.swap.v2.impl.amount.model.converter.SwapAmountErrorConverter
import com.tangem.features.swap.v2.impl.amount.model.converter.SwapAmountUpdateSubtitleConverter
import com.tangem.features.swap.v2.impl.R
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM
import com.tangem.features.swap.v2.impl.common.isRestrictedByFCA
import com.tangem.utils.extensions.orZero
import com.tangem.utils.transformer.Transformer

internal class SwapAmountSelectQuoteTransformer(
    private val quoteUM: SwapQuoteUM,
    private val secondaryMaximumAmountBoundary: EnterAmountBoundary?,
    private val secondaryMinimumAmountBoundary: EnterAmountBoundary?,
    private val isNeedApplyFCARestrictions: Boolean,
    private val isBalanceHidden: Boolean,
    private val primaryMaximumAmountBoundary: EnterAmountBoundary? = null,
    private val primaryMinimumAmountBoundary: EnterAmountBoundary? = null,
) : Transformer<SwapAmountUM> {

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    override fun transform(prevState: SwapAmountUM): SwapAmountUM {
        if (prevState !is SwapAmountUM.Content) return prevState

        val isPrimarySelected = prevState.selectedAmountType == SwapAmountType.From
        val isSecondarySelected = prevState.selectedAmountType == SwapAmountType.To

        val primaryProviderErrorConverter = SwapAmountErrorConverter(
            cryptoCurrency = prevState.primaryCryptoCurrencyStatus.currency,
        )
        val secondaryProviderErrorConverter = prevState.secondaryCryptoCurrencyStatus?.let {
            SwapAmountErrorConverter(cryptoCurrency = it.currency)
        }

        val quoteContent = quoteUM as? SwapQuoteUM.Content
        val fromAmount = quoteContent?.fromAmount
        val toAmount = quoteContent?.toAmount

        val primarySwapAmountField = prevState.primaryAmount as? SwapAmountFieldUM.Content
        val secondarySwapAmountField = prevState.secondaryAmount as? SwapAmountFieldUM.Content

        val subtitleConverter = SwapAmountUpdateSubtitleConverter(
            selectedAmountType = prevState.selectedAmountType,
            isBalanceHidden = isBalanceHidden,
        )

        val newPrimaryAmount = when {
            fromAmount != null && primaryMaximumAmountBoundary != null -> {
                primarySwapAmountField?.let { fromField ->
                    subtitleConverter.updateSubtitles(
                        field = fromField,
                        cryptoCurrencyStatus = prevState.primaryCryptoCurrencyStatus,
                        isAmountEmpty = false,
                        displayAmount = fromAmount,
                    ).copy(
                        amountField = AmountFieldChangeTransformer(
                            cryptoCurrencyStatus = prevState.primaryCryptoCurrencyStatus,
                            maxEnterAmount = primaryMaximumAmountBoundary,
                            minimumTransactionAmount = primaryMinimumAmountBoundary,
                            value = fromAmount.parseBigDecimal(
                                prevState.primaryCryptoCurrencyStatus.currency.decimals,
                            ),
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
                    amountField = AmountFieldChangeTransformer(
                        cryptoCurrencyStatus = prevState.primaryCryptoCurrencyStatus,
                        maxEnterAmount = primaryMaximumAmountBoundary,
                        minimumTransactionAmount = primaryMinimumAmountBoundary,
                        value = "",
                    ).transform(primarySwapAmountField.amountField),
                )
            }
            else -> prevState.primaryAmount
        }

        val newSecondaryAmount = if (
            prevState.secondaryCryptoCurrencyStatus != null &&
            secondaryMaximumAmountBoundary != null &&
            secondarySwapAmountField != null
        ) {
            val fromAmountForPriceImpact = fromAmount
                ?: (prevState.primaryAmount.amountField as? AmountState.Data)
                    ?.amountTextField?.cryptoAmount?.value.orZero()
            val priceImpact = calculatePriceImpact(
                swapDirection = prevState.swapDirection,
                fromTokenAmount = fromAmountForPriceImpact,
                toTokenAmount = toAmount.orZero(),
                primaryCryptoCurrencyStatus = prevState.primaryCryptoCurrencyStatus,
                secondaryCryptoCurrencyStatus = prevState.secondaryCryptoCurrencyStatus,
            )
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
                AmountFieldChangeTransformer(
                    cryptoCurrencyStatus = prevState.secondaryCryptoCurrencyStatus,
                    maxEnterAmount = secondaryMaximumAmountBoundary,
                    minimumTransactionAmount = secondaryMinimumAmountBoundary,
                    value = toAmount?.parseBigDecimal(prevState.secondaryCryptoCurrencyStatus.currency.decimals)
                        .orEmpty(),
                ).transform(secondarySwapAmountField.amountField)
            }
            val insufficientFundsError = if (isSecondarySelected && fromAmount != null) {
                val primaryBalance = prevState.primaryCryptoCurrencyStatus.value.amount
                if (primaryBalance != null && fromAmount > primaryBalance) {
                    resourceReference(R.string.swapping_insufficient_funds)
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
                priceImpact = priceImpact,
                amountField = secondaryAmountFieldWithError,
            )
        } else {
            prevState.secondaryAmount
        }

        return prevState.copy(
            isPrimaryButtonEnabled = quoteUM is SwapQuoteUM.Content,
            selectedQuote = quoteUM,
            isShowFCAWarning = isNeedApplyFCARestrictions && quoteUM.provider?.isRestrictedByFCA() == true,
            primaryAmount = newPrimaryAmount,
            secondaryAmount = newSecondaryAmount,
        )
    }
}