package com.tangem.features.onramp.alloffers.entity

import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.crypto
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
import kotlinx.collections.immutable.toPersistentList
import java.math.BigDecimal

internal class AllOffersStateFactory(
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val currentStateProvider: Provider<AllOffersStateUM>,
    private val allOffersIntents: AllOffersIntents,
) {

    fun getLoadedPaymentsState(methodGroups: List<OnrampPaymentMethodGroup>): AllOffersStateUM {
        return AllOffersStateUM.Content(
            methods = methodGroups.map { methodGroup ->
                AllOffersPaymentMethodUM(
                    offers = mapOffersToUM(methodGroup.offers).toPersistentList(),
                    methodConfig = OnrampPaymentMethodConfig(
                        method = methodGroup.paymentMethod,
                        onClick = { allOffersIntents.onPaymentMethodClicked(methodGroup.paymentMethod.id) },
                    ),
                    diff = methodGroup
                        .bestRateOffer
                        ?.rateDif
                        ?.takeIf { it > BigDecimal.ZERO }
                        ?.let { diff ->
                            stringReference("$MINUS${diff.format { percent() }}")
                        },
                    rate = methodGroup.bestRateOffer?.let { offer ->
                        when (val quote = offer.quote) {
                            is OnrampQuote.Data -> quote.toAmount.value.format {
                                crypto(
                                    symbol = quote.toAmount.symbol,
                                    decimals = quote.toAmount.decimals,
                                )
                            }
                            else -> ""
                        }
                    } ?: "",
                    providersCount = methodGroup.providerCount,
                    isBestRate = methodGroup.isBestPaymentMethod,
                )
            }.toPersistentList(),
            currentMethod = null,
            onBackClicked = { allOffersIntents.onBackClicked() },
        )
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
        return when (onrampError) {
            is OnrampError.DataError -> getErrorState(
                errorCode = onrampError.code,
                onRefresh = allOffersIntents::onRefresh,
            )
            OnrampError.PairsNotFound,
            is OnrampError.DomainError,
            -> getErrorState(onRefresh = allOffersIntents::onRefresh)
            is OnrampError.AmountError.TooBigError,
            is OnrampError.AmountError.TooSmallError,
            OnrampError.RedirectError.VerificationFailed,
            OnrampError.RedirectError.WrongRequestId,
            -> currentStateProvider()
        }
    }

    private fun getErrorState(errorCode: String? = null, onRefresh: () -> Unit): AllOffersStateUM {
        val state = currentStateProvider()
        return when (state) {
            is AllOffersStateUM.Content,
            AllOffersStateUM.Loading,
            -> AllOffersStateUM.Error(
                errorNotification = NotificationUM.Warning.OnrampErrorNotification(
                    errorCode = errorCode,
                    onRefresh = onRefresh,
                ),
            )
            is AllOffersStateUM.Error -> state
        }
    }

    private fun mapOfferAdvantagesDTOtoUM(advantages: OnrampOfferAdvantages): OnrampOfferAdvantagesUM {
        return when (advantages) {
            OnrampOfferAdvantages.Default -> OnrampOfferAdvantagesUM.Default
            OnrampOfferAdvantages.BestRate -> OnrampOfferAdvantagesUM.BestRate
            OnrampOfferAdvantages.Fastest -> OnrampOfferAdvantagesUM.Fastest
        }
    }

    private fun mapOffersToUM(offers: List<OnrampOffer>): List<OnrampOfferUM> {
        return buildList {
            offers.forEach { offer ->
                when (val quote = offer.quote) {
                    is OnrampQuote.Data -> {
                        add(
                            OnrampOfferUM(
                                category = OnrampOfferCategoryUM.Recommended,
                                advantages = mapOfferAdvantagesDTOtoUM(offer.advantages),
                                paymentMethod = quote.paymentMethod,
                                providerId = quote.provider.id,
                                providerName = quote.provider.info.name,
                                rate = quote.toAmount.value.format {
                                    crypto(
                                        symbol = quote.toAmount.symbol,
                                        decimals = quote.toAmount.decimals,
                                    )
                                },
                                diff = offer
                                    .rateDif
                                    ?.takeIf { it > BigDecimal.ZERO }
                                    ?.let { diff ->
                                        stringReference("$MINUS${diff.format { percent() }}")
                                    },
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
                            ),
                        )
                    }
                    is OnrampQuote.AmountError,
                    is OnrampQuote.Error,
                    -> Unit
                }
            }
        }
    }
}