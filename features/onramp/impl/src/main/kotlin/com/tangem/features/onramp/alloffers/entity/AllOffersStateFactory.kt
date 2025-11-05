package com.tangem.features.onramp.alloffers.entity

import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.domain.onramp.analytics.OnrampAnalyticsEvent
import com.tangem.domain.onramp.model.*
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.features.onramp.mainv2.entity.OnrampOfferAdvantagesUM
import com.tangem.features.onramp.mainv2.entity.OnrampOfferCategoryUM
import com.tangem.features.onramp.mainv2.entity.OnrampOfferUM
import com.tangem.utils.Provider
import com.tangem.utils.StringsSigns.MINUS
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import java.math.BigDecimal

internal class AllOffersStateFactory(
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val currentStateProvider: Provider<AllOffersStateUM>,
    private val allOffersIntents: AllOffersIntents,
) {

    fun getLoadedPaymentsState(methodGroups: List<OnrampPaymentMethodGroup>, currencyCode: String): AllOffersStateUM {
        return AllOffersStateUM.Content(
            methods = methodGroups.map { group ->
                createPaymentMethodUM(group, currencyCode)
            }.toPersistentList(),
            currentMethod = replaceOffersForCurrentMethod(methodGroups, currencyCode),
            onBackClicked = { allOffersIntents.onBackClicked() },
        )
    }

    private fun createPaymentMethodUM(
        methodGroup: OnrampPaymentMethodGroup,
        currencyCode: String,
    ): AllOffersPaymentMethodUM {
        return AllOffersPaymentMethodUM(
            offers = mapOffersToUM(methodGroup.offers, currencyCode).toPersistentList(),
            methodConfig = createMethodConfig(methodGroup.paymentMethod),
            diff = formatRateDiff(methodGroup.bestRateOffer?.rateDif),
            rate = formatBestRate(methodGroup.bestRateOffer, currencyCode),
            providersCount = methodGroup.providerCount,
            isBestRate = methodGroup.isBestPaymentMethod,
            paymentMethodStatus = methodGroup.methodStatus,
        )
    }

    private fun createMethodConfig(paymentMethod: OnrampPaymentMethod): OnrampPaymentMethodConfig {
        return OnrampPaymentMethodConfig(
            method = paymentMethod,
            onClick = { allOffersIntents.onPaymentMethodClicked(paymentMethod.id) },
        )
    }

    private fun formatRateDiff(rateDif: BigDecimal?) = rateDif
        ?.takeIf { it > BigDecimal.ZERO }
        ?.let { diff -> stringReference("$MINUS${diff.format { percent() }}") }

    private fun formatBestRate(bestRateOffer: OnrampOffer?, currencyCode: String): String {
        return when (val quote = bestRateOffer?.quote) {
            is OnrampQuote.Data -> formatCryptoAmount(quote.toAmount)
            is OnrampQuote.AmountError -> formatRequiredAmount(quote, currencyCode)
            is OnrampQuote.Error,
            null,
            -> ""
        }
    }

    private fun formatCryptoAmount(amount: OnrampAmount): String {
        return amount.value.format {
            crypto(symbol = amount.symbol, decimals = amount.decimals)
        }
    }

    private fun formatRequiredAmount(quote: OnrampQuote.AmountError, currencyCode: String): String {
        return quote.error.requiredAmount.format {
            fiat(fiatCurrencySymbol = quote.fromAmount.symbol, fiatCurrencyCode = currencyCode)
        }
    }

    fun getPaymentsState(): AllOffersStateUM {
        return when (val currentState = currentStateProvider.invoke()) {
            is AllOffersStateUM.Content -> {
                analyticsEventHandler.send(OnrampAnalyticsEvent.PaymentMethodsScreenOpened)
                currentState.copy(currentMethod = null)
            }
            AllOffersStateUM.Loading,
            is AllOffersStateUM.Error,
            -> currentState
        }
    }

    fun getOnrampErrorState(onrampError: OnrampError): AllOffersStateUM {
        return if (shouldShowErrorState(onrampError)) {
            createErrorState(errorCode = (onrampError as? OnrampError.DataError)?.code)
        } else {
            currentStateProvider()
        }
    }

    private fun shouldShowErrorState(error: OnrampError): Boolean {
        return when (error) {
            is OnrampError.DataError,
            OnrampError.PairsNotFound,
            is OnrampError.DomainError,
            -> true
            is OnrampError.AmountError,
            is OnrampError.RedirectError,
            -> false
        }
    }

    private fun createErrorState(errorCode: String? = null): AllOffersStateUM {
        return when (currentStateProvider()) {
            is AllOffersStateUM.Content,
            AllOffersStateUM.Loading,
            -> AllOffersStateUM.Error(
                errorNotification = NotificationUM.Warning.OnrampErrorNotification(
                    errorCode = errorCode,
                    onRefresh = allOffersIntents::onRefresh,
                ),
            )
            is AllOffersStateUM.Error -> currentStateProvider()
        }
    }

    private fun mapOfferAdvantagesDTOtoUM(advantages: OnrampOfferAdvantages): OnrampOfferAdvantagesUM {
        return when (advantages) {
            OnrampOfferAdvantages.Default -> OnrampOfferAdvantagesUM.Default
            OnrampOfferAdvantages.BestRate -> OnrampOfferAdvantagesUM.BestRate
            OnrampOfferAdvantages.Fastest -> OnrampOfferAdvantagesUM.Fastest
            OnrampOfferAdvantages.GreatRate -> OnrampOfferAdvantagesUM.GreatRate
        }
    }

    private fun mapOffersToUM(offers: List<OnrampOffer>, currencyCode: String): List<OnrampOfferUM> {
        return offers.mapNotNull { offer ->
            when (val quote = offer.quote) {
                is OnrampQuote.Data -> createDataOfferUM(quote, offer)
                is OnrampQuote.AmountError -> createAmountErrorOfferUM(quote, offer, currencyCode)
                is OnrampQuote.Error -> null
            }
        }
    }

    private fun createDataOfferUM(quote: OnrampQuote.Data, offer: OnrampOffer): OnrampOfferUM {
        return OnrampOfferUM(
            category = OnrampOfferCategoryUM.Recommended,
            advantages = mapOfferAdvantagesDTOtoUM(offer.advantages),
            paymentMethod = quote.paymentMethod,
            providerName = quote.provider.info.name,
            rate = formatCryptoAmount(quote.toAmount),
            diff = formatRateDiff(offer.rateDif),
            onBuyClicked = {
                allOffersIntents.onBuyClick(
                    quote = OnrampProviderWithQuote.Data(
                        provider = quote.provider,
                        paymentMethod = quote.paymentMethod,
                        toAmount = quote.toAmount,
                        fromAmount = quote.fromAmount,
                    ),
                    onrampOfferAdvantagesUM = mapOfferAdvantagesDTOtoUM(offer.advantages),
                )
            },
        )
    }

    private fun createAmountErrorOfferUM(
        quote: OnrampQuote.AmountError,
        offer: OnrampOffer,
        currencyCode: String,
    ): OnrampOfferUM {
        val advantages = when (quote.error) {
            is OnrampError.AmountError.TooSmallError -> OnrampOfferAdvantagesUM.Unavailable.MinAmount
            is OnrampError.AmountError.TooBigError -> OnrampOfferAdvantagesUM.Unavailable.MaxAmount
        }

        return OnrampOfferUM(
            category = OnrampOfferCategoryUM.Recommended,
            advantages = advantages,
            paymentMethod = quote.paymentMethod,
            providerName = quote.provider.info.name,
            rate = formatRequiredAmount(quote, currencyCode),
            diff = formatRateDiff(offer.rateDif),
            onBuyClicked = {},
        )
    }

    private fun replaceOffersForCurrentMethod(
        methodGroups: List<OnrampPaymentMethodGroup>,
        currencyCode: String,
    ): AllOffersPaymentMethodUM? {
        val currentMethod = (currentStateProvider() as? AllOffersStateUM.Content)?.currentMethod ?: return null

        val updateForCurrentMethod = methodGroups.find { it.paymentMethod.id == currentMethod.methodConfig.method.id }

        return updateForCurrentMethod?.let { updatedCurrentMethod ->
            currentMethod.copy(
                offers = mapOffersToUM(updatedCurrentMethod.offers, currencyCode).toImmutableList(),
            )
        }
    }
}