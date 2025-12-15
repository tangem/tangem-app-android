package com.tangem.features.onramp.mainv2.entity.factory

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.onramp.analytics.OnrampAnalyticsEvent
import com.tangem.domain.onramp.model.OnrampCurrency
import com.tangem.domain.onramp.model.OnrampQuote
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.domain.tokens.model.AmountType
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.mainv2.entity.*
import com.tangem.features.onramp.mainv2.entity.converter.OnrampV2AmountFieldChangeConverter
import com.tangem.utils.Provider

internal class OnrampV2AmountStateFactory(
    private val currentStateProvider: Provider<OnrampV2MainComponentUM>,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val onrampIntents: OnrampV2Intents,
    private val onrampAmountButtonUMStateFactory: OnrampAmountButtonUMStateFactory,
) {

    private val onrampAmountFieldChangeConverter: OnrampV2AmountFieldChangeConverter by lazy(
        mode = LazyThreadSafetyMode.NONE,
    ) {
        OnrampV2AmountFieldChangeConverter(
            currentStateProvider = currentStateProvider,
            onrampAmountButtonUMStateFactory = onrampAmountButtonUMStateFactory,
            onrampIntents = onrampIntents,
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
            offersBlockState = if (amountState.amountFieldModel.fiatValue.isEmpty()) {
                OnrampOffersBlockUM.Empty
            } else {
                OnrampOffersBlockUM.Loading
            },
        )
    }

    fun getSecondaryFieldAmountErrorState(quotes: List<OnrampQuote>): OnrampV2MainComponentUM {
        val currentState = currentStateProvider()
        if (currentState !is OnrampV2MainComponentUM.Content) return currentState

        val amountState = currentState.amountBlockState
        if (amountState.amountFieldModel.fiatValue.isEmpty()) return currentState

        val limitedQuote = getLimitFromAmountErrors(quotes)
        return currentState.copy(
            amountBlockState = amountState.copy(
                amountFieldModel = amountState.amountFieldModel.copy(isError = false),
                secondaryFieldModel = limitedQuote?.toSecondaryFieldUiModel(amountState)
                    ?: OnrampSecondaryFieldErrorUM.Empty,
            ),
            errorNotification = null,
            offersBlockState = OnrampOffersBlockUM.Empty,
        )
    }

    fun getAmountSecondaryFieldResetState(): OnrampV2MainComponentUM {
        val currentState = currentStateProvider()
        if (currentState !is OnrampV2MainComponentUM.Content) return currentState

        val amountState = currentState.amountBlockState
        if (amountState.secondaryFieldModel is OnrampSecondaryFieldErrorUM.Empty) return currentState

        return currentState.copy(
            amountBlockState = amountState.copy(secondaryFieldModel = OnrampSecondaryFieldErrorUM.Empty),
            onrampAmountButtonUMState = OnrampV2AmountButtonUMState.None,
            errorNotification = null,
            offersBlockState = currentState.offersBlockState,
        )
    }

    private fun OnrampQuote.AmountError.toSecondaryFieldUiModel(
        amountState: OnrampNewAmountBlockUM,
    ): OnrampSecondaryFieldErrorUM.Error {
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

        return OnrampSecondaryFieldErrorUM.Error(
            resourceReference(
                errorTextRes,
                wrappedList(amount),
            ),
        )
    }

    private fun getLimitFromAmountErrors(quotes: List<OnrampQuote>): OnrampQuote.AmountError? {
        val amountErrorQuotes = quotes.filterIsInstance<OnrampQuote.AmountError>()
        if (amountErrorQuotes.isEmpty()) {
            return null
        }
        val tooSmallErrors = amountErrorQuotes.filter { it.error is OnrampError.AmountError.TooSmallError }
        val tooBigErrors = amountErrorQuotes.filter { it.error is OnrampError.AmountError.TooBigError }
        if (tooSmallErrors.isNotEmpty()) {
            return tooSmallErrors.minByOrNull { it.error.requiredAmount }
        }
        if (tooBigErrors.isNotEmpty()) {
            return tooBigErrors.maxByOrNull { it.error.requiredAmount }
        }
        return null
    }
}