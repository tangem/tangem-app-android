package com.tangem.features.onramp.main.entity.factory.amount

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.onramp.model.OnrampCurrency
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
                        currencySymbol = currency.code,
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
        )
    }

    fun getAmountSecondaryUpdatedState(quote: OnrampQuote): OnrampMainComponentUM {
        val currentState = currentStateProvider()
        if (currentState !is OnrampMainComponentUM.Content) return currentState

        val amountState = currentState.amountBlockState
        return currentState.copy(
            amountBlockState = amountState.copy(
                secondaryFieldModel = quote.toSecondaryFieldUiModel(amountState),
            ),
            providerBlockState = quote.toProviderBlockState(),
        )
    }

    private fun OnrampQuote.toProviderBlockState(): OnrampProviderBlockUM {
        return OnrampProviderBlockUM.Content(
            paymentMethodIconUrl = paymentMethod.imageUrl,
            paymentMethodName = paymentMethod.name,
            providerName = provider.info.name,
            isBestRate = true,
            onClick = onrampIntents::openProviders,
        )
    }

    private fun OnrampQuote.toSecondaryFieldUiModel(amountState: OnrampAmountBlockUM): OnrampAmountSecondaryFieldUM {
        return when (this) {
            is OnrampQuote.Data -> {
                val amount = toAmount.format {
                    crypto(
                        symbol = amountState.amountFieldModel.cryptoAmount.currencySymbol,
                        decimals = amountState.amountFieldModel.cryptoAmount.decimals,
                    )
                }
                OnrampAmountSecondaryFieldUM.Content(stringReference(amount))
            }
            is OnrampQuote.Error.AmountTooBigError -> {
                val amount = this.amount.value.format {
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
                val amount = this.amount.value.format {
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
