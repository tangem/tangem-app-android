package com.tangem.features.onramp.mainv2.entity.factory

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.onramp.analytics.OnrampAnalyticsEvent
import com.tangem.domain.onramp.model.OnrampCurrency
import com.tangem.domain.onramp.model.OnrampQuote
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.domain.tokens.model.AmountType
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.mainv2.entity.*
import com.tangem.features.onramp.mainv2.entity.converter.OnrampV2AmountFieldChangeConverter
import com.tangem.utils.Provider
import java.math.BigDecimal

internal class OnrampV2AmountStateFactory(
    private val currentStateProvider: Provider<OnrampV2MainComponentUM>,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val onrampIntents: OnrampV2Intents,
    private val cryptoCurrency: CryptoCurrency,
    private val onrampAmountButtonUMStateFactory: OnrampAmountButtonUMStateFactory,
) {

    private val onrampAmountFieldChangeConverter: OnrampV2AmountFieldChangeConverter by lazy(
        mode = LazyThreadSafetyMode.NONE,
    ) {
        OnrampV2AmountFieldChangeConverter(
            currentStateProvider = currentStateProvider,
            onrampAmountButtonUMStateFactory = onrampAmountButtonUMStateFactory,
            onrampIntents = onrampIntents,
            cryptoCurrency = cryptoCurrency,
        )
    }

    fun getOnAmountValueChange(value: String): OnrampV2MainComponentUM {
        return onrampAmountFieldChangeConverter.convert(value)
    }

    fun getUpdatedCurrencyState(currency: OnrampCurrency): OnrampV2MainComponentUM {
        val currentState = currentStateProvider()
        if (currentState !is OnrampV2MainComponentUM.Content) return currentState

        val amountState = currentState.amountBlockState

        return currentState.copy(
            amountBlockState = amountState.copy(
                currencyUM = amountState.currencyUM.copy(
                    unit = currency.unit,
                    code = currency.code,
                    iconUrl = currency.image,
                    precision = currency.precision,
                ),
                amountFieldModel = amountState.amountFieldModel.copy(
                    isError = false,
                    fiatAmount = amountState.amountFieldModel.fiatAmount.copy(
                        currencySymbol = currency.unit,
                        decimals = currency.precision,
                        type = AmountType.FiatType(currency.code),
                    ),
                ),
            ),
            onrampAmountButtonUMState = onrampAmountButtonUMStateFactory.createOnrampAmountActionButton(
                currencyCode = currency.code,
                currencySymbol = currency.unit,
                onAmountValueChanged = onrampIntents::onAmountValueChanged,
            ),
        )
    }

    fun getAmountSecondaryLoadingState(): OnrampV2MainComponentUM {
        val currentState = currentStateProvider()
        if (currentState !is OnrampV2MainComponentUM.Content) return currentState

        val amountState = currentState.amountBlockState

        return currentState.copy(
            amountBlockState = amountState.copy(
                secondaryFieldModel = OnrampNewAmountSecondaryFieldUM.Loading,
            ),
            offersBlockState = OnrampOffersBlockUM.Loading(isBlockVisible = false),
            continueButtonConfig = currentState.continueButtonConfig.copy(enabled = false),
            errorNotification = null,
            onrampAmountButtonUMState = OnrampV2AmountButtonUMState.None,
            onrampProviderState = OnrampV2ProvidersUM.Loading,
        )
    }

    fun getAmountSecondaryUpdatedState(quote: OnrampQuote): OnrampV2MainComponentUM {
        val currentState = currentStateProvider()
        if (currentState !is OnrampV2MainComponentUM.Content) return currentState

        val amountState = currentState.amountBlockState
        if (amountState.amountFieldModel.fiatValue.isEmpty()) return currentState

        return currentState.copy(
            amountBlockState = amountState.copy(
                amountFieldModel = amountState.amountFieldModel.copy(isError = false),
                secondaryFieldModel = quote.toSecondaryFieldUiModel(amountState) ?: amountState.secondaryFieldModel,
            ),
            continueButtonConfig = currentState.continueButtonConfig.copy(
                enabled = quote is OnrampQuote.Data,
                onClick = onrampIntents::onContinueClick,
            ),
            errorNotification = null,
        )
    }

    fun getUpdatedProviderState(selectedQuote: OnrampQuote): OnrampV2MainComponentUM {
        val currentState = currentStateProvider()
        if (currentState !is OnrampV2MainComponentUM.Content) return currentState

        analyticsEventHandler.send(
            OnrampAnalyticsEvent.ProviderCalculated(
                providerName = selectedQuote.provider.info.name,
                tokenSymbol = cryptoCurrency.symbol,
                paymentMethod = selectedQuote.paymentMethod.name,
            ),
        )
        return currentState.copy(
            onrampProviderState = selectedQuote.toProviderBlockState(),
        )
    }

    fun getAmountSecondaryResetState(): OnrampV2MainComponentUM {
        val currentState = currentStateProvider()
        if (currentState !is OnrampV2MainComponentUM.Content) return currentState

        val amountState = currentState.amountBlockState

        if (amountState.secondaryFieldModel is OnrampNewAmountSecondaryFieldUM.Content) return currentState

        return currentState.copy(
            amountBlockState = amountState.copy(
                secondaryFieldModel = OnrampNewAmountSecondaryFieldUM.Content(
                    amount = stringReference(
                        BigDecimal.ZERO.format {
                            crypto(cryptoCurrency = cryptoCurrency, ignoreSymbolPosition = true)
                        },
                    ),
                ),
            ),
            onrampAmountButtonUMState = OnrampV2AmountButtonUMState.None,
            errorNotification = null,
        )
    }

    fun getShowProvidersState(): OnrampV2MainComponentUM {
        val currentState = currentStateProvider()
        if (currentState !is OnrampV2MainComponentUM.Content) return currentState

        return when (currentState.offersBlockState) {
            is OnrampOffersBlockUM.Content -> {
                currentState.copy(
                    offersBlockState = currentState.offersBlockState.copy(isBlockVisible = true),
                )
            }
            OnrampOffersBlockUM.Empty,
            is OnrampOffersBlockUM.Loading,
            -> currentState
        }
    }

    private fun OnrampQuote.toProviderBlockState(): OnrampV2ProvidersUM {
        return OnrampV2ProvidersUM.Content(
            paymentMethod = paymentMethod,
            providerId = provider.id,
        )
    }

    private fun OnrampQuote.toSecondaryFieldUiModel(
        amountState: OnrampNewAmountBlockUM,
    ): OnrampNewAmountSecondaryFieldUM? {
        return when (this) {
            is OnrampQuote.Error -> null
            is OnrampQuote.Data -> {
                val amount = toAmount.value.format {
                    crypto(cryptoCurrency = cryptoCurrency, ignoreSymbolPosition = true)
                }
                val contentAmount = combinedReference(stringReference("\u007E"), stringReference(amount))
                OnrampNewAmountSecondaryFieldUM.Content(contentAmount)
            }
            is OnrampQuote.AmountError -> this.toSecondaryFieldUiModel(amountState)
        }
    }

    private fun OnrampQuote.AmountError.toSecondaryFieldUiModel(
        amountState: OnrampNewAmountBlockUM,
    ): OnrampNewAmountSecondaryFieldUM.Error {
        val amount = error.requiredAmount.format {
            fiat(
                fiatCurrencyCode = amountState.amountFieldModel.fiatAmount.currencySymbol,
                fiatCurrencySymbol = amountState.amountFieldModel.fiatAmount.currencySymbol,
            )
        }

        val errorTextRes = when (error) {
            is OnrampError.AmountError.TooBigError -> {
                analyticsEventHandler.send(OnrampAnalyticsEvent.MaxAmountError)
                R.string.onramp_max_amount_restriction
            }
            is OnrampError.AmountError.TooSmallError -> {
                analyticsEventHandler.send(OnrampAnalyticsEvent.MinAmountError)
                R.string.onramp_min_amount_restriction
            }
        }

        return OnrampNewAmountSecondaryFieldUM.Error(
            resourceReference(
                errorTextRes,
                wrappedList(amount),
            ),
        )
    }
}