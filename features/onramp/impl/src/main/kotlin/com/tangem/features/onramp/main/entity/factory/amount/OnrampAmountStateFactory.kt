package com.tangem.features.onramp.main.entity.factory.amount

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.onramp.model.OnrampCurrency
import com.tangem.domain.onramp.model.OnrampProviderWithQuote
import com.tangem.domain.onramp.model.OnrampQuote
import com.tangem.domain.tokens.model.AmountType
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.main.entity.*
import com.tangem.utils.Provider

internal class OnrampAmountStateFactory(
    private val currentStateProvider: Provider<OnrampMainComponentUM>,
    private val onrampIntents: OnrampIntents,
) {

    private val onrampAmountFieldChangeConverter = OnrampAmountFieldChangeConverter(
        currentStateProvider = currentStateProvider,
    )

    fun getOnAmountValueChange(value: String) = onrampAmountFieldChangeConverter.convert(value)

    fun getUpdatedCurrencyState(currency: OnrampCurrency): OnrampMainComponentUM {
        val currentState = currentStateProvider()
        if (currentState !is OnrampMainComponentUM.Content) return currentState

        val amountState = currentState.amountBlockState
        return currentState.copy(
            amountBlockState = amountState.copy(
                currencyUM = amountState.currencyUM.copy(
                    code = currency.code,
                    iconUrl = currency.image,
                    precision = currency.precision,
                ),
                amountFieldModel = amountState.amountFieldModel.copy(
                    fiatAmount = amountState.amountFieldModel.fiatAmount.copy(
                        currencySymbol = currency.unit,
                        decimals = currency.precision,
                        type = AmountType.FiatType(currency.code),
                    ),
                ),
            ),
        )
    }

    fun getAmountSecondaryLoadingState(): OnrampMainComponentUM {
        val currentState = currentStateProvider()
        if (currentState !is OnrampMainComponentUM.Content) return currentState

        val amountState = currentState.amountBlockState

        return currentState.copy(
            amountBlockState = amountState.copy(secondaryFieldModel = OnrampAmountSecondaryFieldUM.Loading),
            buyButtonConfig = currentState.buyButtonConfig.copy(enabled = false),
        )
    }

    fun getAmountSecondaryUpdatedState(quote: OnrampQuote, isBestRate: Boolean): OnrampMainComponentUM {
        val currentState = currentStateProvider()
        if (currentState !is OnrampMainComponentUM.Content) return currentState

        val amountState = currentState.amountBlockState
        if (amountState.amountFieldModel.fiatValue.isEmpty()) return currentState

        return currentState.copy(
            amountBlockState = amountState.copy(
                secondaryFieldModel = quote.toSecondaryFieldUiModel(amountState),
            ),
            providerBlockState = quote.toProviderBlockState(isBestRate),
            buyButtonConfig = currentState.buyButtonConfig.copy(
                enabled = quote is OnrampQuote.Data,
                onClick = {
                    if (quote is OnrampQuote.Data) {
                        onrampIntents.onBuyClick(
                            OnrampProviderWithQuote.Data(
                                provider = quote.provider,
                                paymentMethod = quote.paymentMethod,
                                toAmount = quote.toAmount,
                                fromAmount = quote.fromAmount,
                            ),
                        )
                    }
                },
            ),
        )
    }

    fun getAmountSecondaryUpdatedState(
        quoteWithProvider: OnrampProviderWithQuote.Data,
        isBestRate: Boolean,
    ): OnrampMainComponentUM {
        val currentState = currentStateProvider()
        if (currentState !is OnrampMainComponentUM.Content) return currentState

        val amountState = currentState.amountBlockState
        val amount = quoteWithProvider.toAmount.value.format {
            crypto(symbol = quoteWithProvider.toAmount.symbol, decimals = quoteWithProvider.toAmount.decimals)
        }
        return currentState.copy(
            amountBlockState = amountState.copy(
                secondaryFieldModel = OnrampAmountSecondaryFieldUM.Content(stringReference(amount)),
            ),
            providerBlockState = OnrampProviderBlockUM.Content(
                paymentMethod = quoteWithProvider.paymentMethod,
                providerName = quoteWithProvider.provider.info.name,
                isBestRate = isBestRate,
                onClick = onrampIntents::openProviders,
            ),
            buyButtonConfig = currentState.buyButtonConfig.copy(
                enabled = true,
                onClick = { onrampIntents.onBuyClick(quoteWithProvider) },
            ),
        )
    }

    private fun OnrampQuote.toProviderBlockState(isBestRate: Boolean): OnrampProviderBlockUM {
        return OnrampProviderBlockUM.Content(
            paymentMethod = paymentMethod,
            providerName = provider.info.name,
            isBestRate = isBestRate,
            onClick = onrampIntents::openProviders,
        )
    }

    private fun OnrampQuote.toSecondaryFieldUiModel(amountState: OnrampAmountBlockUM): OnrampAmountSecondaryFieldUM {
        return when (this) {
            is OnrampQuote.Data -> {
                val amount = toAmount.value.format {
                    crypto(symbol = toAmount.symbol, decimals = toAmount.decimals)
                }
                OnrampAmountSecondaryFieldUM.Content(stringReference(amount))
            }
            is OnrampQuote.Error.AmountTooBigError -> {
                val amount = requiredAmount.value.format {
                    fiat(
                        fiatCurrencyCode = amountState.amountFieldModel.fiatAmount.currencySymbol,
                        fiatCurrencySymbol = amountState.amountFieldModel.fiatAmount.currencySymbol,
                    )
                }
                OnrampAmountSecondaryFieldUM.Error(
                    resourceReference(
                        R.string.onramp_max_amount_restriction,
                        wrappedList(amount),
                    ),
                )
            }
            is OnrampQuote.Error.AmountTooSmallError -> {
                val amount = requiredAmount.value.format {
                    fiat(
                        fiatCurrencyCode = amountState.amountFieldModel.fiatAmount.currencySymbol,
                        fiatCurrencySymbol = amountState.amountFieldModel.fiatAmount.currencySymbol,
                    )
                }
                OnrampAmountSecondaryFieldUM.Error(
                    resourceReference(
                        R.string.onramp_min_amount_restriction,
                        wrappedList(amount),
                    ),
                )
            }
        }
    }
}