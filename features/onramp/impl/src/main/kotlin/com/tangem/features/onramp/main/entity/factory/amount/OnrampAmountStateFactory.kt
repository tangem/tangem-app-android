package com.tangem.features.onramp.main.entity.factory.amount

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.onramp.analytics.OnrampAnalyticsEvent
import com.tangem.domain.onramp.model.OnrampCurrency
import com.tangem.domain.onramp.model.OnrampProviderWithQuote
import com.tangem.domain.onramp.model.OnrampQuote
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.domain.tokens.model.AmountType
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.main.entity.*
import com.tangem.features.onramp.providers.entity.SelectProviderResult
import com.tangem.utils.Provider
import com.tangem.utils.extensions.isSingleItem

internal class OnrampAmountStateFactory(
    private val currentStateProvider: Provider<OnrampMainComponentUM>,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val onrampIntents: OnrampIntents,
    private val cryptoCurrency: CryptoCurrency,
    private val needApplyFCARestrictions: Provider<Boolean>,
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
                    isError = false,
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
            providerBlockState = OnrampProviderBlockUM.Loading,
            buyButtonConfig = currentState.buyButtonConfig.copy(enabled = false),
            errorNotification = null,
        )
    }

    fun getAmountSecondaryUpdatedState(quote: OnrampQuote): OnrampMainComponentUM {
        val currentState = currentStateProvider()
        if (currentState !is OnrampMainComponentUM.Content) return currentState

        val amountState = currentState.amountBlockState
        if (amountState.amountFieldModel.fiatValue.isEmpty()) return currentState

        return currentState.copy(
            amountBlockState = amountState.copy(
                amountFieldModel = amountState.amountFieldModel.copy(isError = false),
                secondaryFieldModel = quote.toSecondaryFieldUiModel(amountState) ?: amountState.secondaryFieldModel,
            ),
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
            errorNotification = null,
        )
    }

    fun getUpdatedProviderState(selectedQuote: OnrampQuote, quotes: List<OnrampQuote>): OnrampMainComponentUM {
        val currentState = currentStateProvider()
        if (currentState !is OnrampMainComponentUM.Content) return currentState

        analyticsEventHandler.send(
            OnrampAnalyticsEvent.ProviderCalculated(
                providerName = selectedQuote.provider.info.name,
                tokenSymbol = cryptoCurrency.symbol,
                paymentMethod = selectedQuote.paymentMethod.name,
            ),
        )

        val bestProvider = selectedQuote as? OnrampQuote.Data
        val isMultipleQuotes = !quotes.isSingleItem()
        val isOtherQuotesHasData = quotes
            .filter { it.paymentMethod == selectedQuote.paymentMethod }
            .filterNot { it == bestProvider }
            .any { it is OnrampQuote.Data }

        val isBestProvider = selectedQuote == bestProvider &&
            isMultipleQuotes &&
            isOtherQuotesHasData &&
            !needApplyFCARestrictions()

        return currentState.copy(
            providerBlockState = selectedQuote.toProviderBlockState(isBestProvider),
        )
    }

    fun getAmountSecondaryUpdatedState(
        providerResult: SelectProviderResult,
        isBestRate: Boolean,
    ): OnrampMainComponentUM {
        val currentState = currentStateProvider()
        if (currentState !is OnrampMainComponentUM.Content) return currentState

        val amountState = currentState.amountBlockState
        val secondaryField = when (providerResult) {
            is SelectProviderResult.ProviderWithError -> {
                providerResult.quoteError.toSecondaryFieldUiModel(amountState)
            }
            is SelectProviderResult.ProviderWithQuote -> {
                val amount = providerResult.toAmount.value.format {
                    crypto(symbol = providerResult.toAmount.symbol, decimals = providerResult.toAmount.decimals)
                }
                OnrampAmountSecondaryFieldUM.Content(stringReference(amount))
            }
        }
        return currentState.copy(
            amountBlockState = amountState.copy(secondaryFieldModel = secondaryField),
            providerBlockState = OnrampProviderBlockUM.Content(
                paymentMethod = providerResult.paymentMethod,
                providerId = providerResult.provider.id,
                providerName = providerResult.provider.info.name,
                isBestRate = isBestRate && !needApplyFCARestrictions(),
                onClick = onrampIntents::openProviders,
                termsOfUseLink = providerResult.provider.info.termsOfUseLink,
                privacyPolicyLink = providerResult.provider.info.privacyPolicyLink,
                onLinkClick = onrampIntents::onLinkClick,
            ),
            buyButtonConfig = currentState.buyButtonConfig.copy(
                enabled = providerResult is SelectProviderResult.ProviderWithQuote,
                onClick = {
                    if (providerResult is SelectProviderResult.ProviderWithQuote) {
                        onrampIntents.onBuyClick(
                            OnrampProviderWithQuote.Data(
                                provider = providerResult.provider,
                                paymentMethod = providerResult.paymentMethod,
                                toAmount = providerResult.toAmount,
                                fromAmount = providerResult.fromAmount,
                            ),
                        )
                    }
                },
            ),
            errorNotification = null,
        )
    }

    fun getAmountSecondaryResetState(): OnrampMainComponentUM {
        val currentState = currentStateProvider()
        if (currentState !is OnrampMainComponentUM.Content) return currentState

        val amountState = currentState.amountBlockState

        if (amountState.secondaryFieldModel is OnrampAmountSecondaryFieldUM.Content) return currentState

        return currentState.copy(
            amountBlockState = amountState.copy(
                secondaryFieldModel = OnrampAmountSecondaryFieldUM.Content(
                    amount = TextReference.EMPTY,
                ),
            ),
            errorNotification = null,
        )
    }

    private fun OnrampQuote.toProviderBlockState(isBestRate: Boolean): OnrampProviderBlockUM {
        return OnrampProviderBlockUM.Content(
            paymentMethod = paymentMethod,
            providerId = provider.id,
            providerName = provider.info.name,
            isBestRate = isBestRate,
            onClick = onrampIntents::openProviders,
            termsOfUseLink = provider.info.termsOfUseLink,
            privacyPolicyLink = provider.info.privacyPolicyLink,
            onLinkClick = onrampIntents::onLinkClick,
        )
    }

    private fun OnrampQuote.toSecondaryFieldUiModel(amountState: OnrampAmountBlockUM): OnrampAmountSecondaryFieldUM? {
        return when (this) {
            is OnrampQuote.Error -> null
            is OnrampQuote.Data -> {
                val amount = toAmount.value.format {
                    crypto(symbol = toAmount.symbol, decimals = toAmount.decimals)
                }
                OnrampAmountSecondaryFieldUM.Content(stringReference(amount))
            }
            is OnrampQuote.AmountError -> this.toSecondaryFieldUiModel(amountState)
        }
    }

    private fun OnrampQuote.AmountError.toSecondaryFieldUiModel(
        amountState: OnrampAmountBlockUM,
    ): OnrampAmountSecondaryFieldUM.Error {
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

        return OnrampAmountSecondaryFieldUM.Error(
            resourceReference(
                errorTextRes,
                wrappedList(amount),
            ),
        )
    }
}