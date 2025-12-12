package com.tangem.domain.onramp

import arrow.core.left
import arrow.core.right
import com.tangem.domain.core.utils.EitherFlow
import com.tangem.domain.onramp.model.*
import com.tangem.domain.onramp.model.cache.OnrampTransaction
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.domain.onramp.repositories.OnrampErrorResolver
import com.tangem.domain.onramp.repositories.OnrampRepository
import com.tangem.domain.onramp.repositories.OnrampTransactionRepository
import com.tangem.domain.onramp.utils.calculateRateDif
import com.tangem.domain.onramp.utils.compareOffersByRateSpeedAndPriority
import com.tangem.domain.promo.PromoRepository
import com.tangem.domain.settings.repositories.SettingsRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class GetOnrampOffersUseCase(
    private val onrampRepository: OnrampRepository,
    private val onrampTransactionRepository: OnrampTransactionRepository,
    private val errorResolver: OnrampErrorResolver,
    private val settingsRepository: SettingsRepository,
    private val promoRepository: PromoRepository,
) {

    operator fun invoke(): EitherFlow<OnrampError, List<OnrampOffersBlock>> {
        return combine(
            onrampRepository.getQuotes(),
            onrampTransactionRepository.getAllTransactions(),
            flow { emit(promoRepository.isMoonpayPromoActive()) },
        ) { quotes, transactions, isMoonpayPromoActive ->
            processOffers(quotes, transactions, isMoonpayPromoActive)
        }
            .map { offers -> offers.right() }
            .catch { throwable -> errorResolver.resolve(throwable).left() }
    }

    private suspend fun processOffers(
        quotes: List<OnrampQuote>,
        transactions: List<OnrampTransaction>,
        isMoonpayPromoActive: Boolean,
    ): List<OnrampOffersBlock> {
        val validQuotes = quotes.filterIsInstance<OnrampQuote.Data>()
        if (validQuotes.isEmpty()) return emptyList()

        val isGooglePayAvailable = settingsRepository.isGooglePayAvailability()
        val bestRateQuote = validQuotes.maxWithOrNull(
            compareOffersByRateSpeedAndPriority(
                isGooglePayAvailable = isGooglePayAvailable,
                isSepaPrioritized = true,
            ),
        )
        val bestRate = bestRateQuote?.toAmount?.value

        val offers = validQuotes.map { quote ->
            val rateDif = calculateRateDif(quote.toAmount.value, bestRate)
            OnrampOffer(quote = quote, rateDif = rateDif)
        }

        val recentOffer = findRecentOffer(offers, transactions)
        val bestRateOffer = findBestRateOffer(offers, isGooglePayAvailable)
        val fastestOffer = findFastestOffer(offers, isGooglePayAvailable, isMoonpayPromoActive)

        return buildOffersBlocks(
            recentOffer = recentOffer,
            bestRateOffer = bestRateOffer,
            fastestOffer = fastestOffer,
            allQuotes = quotes,
            isSingleOffer = offers.size == 1,
        )
    }

    private fun findRecentOffer(offers: List<OnrampOffer>, transactions: List<OnrampTransaction>): OnrampOffer? {
        if (transactions.isEmpty()) return null
        val matchingPairs = transactions.mapNotNull { transaction ->
            if (!isRecentUsed(transaction.status)) return@mapNotNull null
            val matchingOffer = offers.find { offer ->
                offer.quote.provider.info.name == transaction.providerName &&
                    offer.quote.paymentMethod.name == transaction.paymentMethod
            }
            matchingOffer?.let { it to transaction }
        }
        return matchingPairs.maxByOrNull { (_, transaction) -> transaction.timestamp }?.first
    }

    private fun findBestRateOffer(offers: List<OnrampOffer>, isGooglePayAvailable: Boolean): OnrampOffer? {
        return offers.maxWithOrNull(offerComparator(isGooglePayAvailable))
    }

    private fun findFastestOffer(
        offers: List<OnrampOffer>,
        isGooglePayAvailable: Boolean,
        isMoonpayPromoActive: Boolean,
    ): OnrampOffer? {
        val moonpayPromoOffers = if (isMoonpayPromoActive) {
            offers.filter { offer ->
                offer.quote.provider.id == MOONPAY_PROMO_PROVIDER_ID &&
                    offer.quote.paymentMethod.type == PaymentMethodType.GOOGLE_PAY
            }
        } else {
            emptyList()
        }

        val instantOffers = moonpayPromoOffers.ifEmpty {
            offers.filter { it.quote.paymentMethod.type.isInstant() }
        }

        return if (instantOffers.isNotEmpty()) {
            instantOffers.maxWithOrNull(fastestOfferComparator(isGooglePayAvailable))
        } else {
            val offersBySpeed = offers.groupBy { offer ->
                offer.quote.paymentMethod.type.getProcessingSpeed().speed
            }
            val fastestSpeed = offersBySpeed.keys.minOrNull() ?: return null
            val fastestOffers = offersBySpeed[fastestSpeed] ?: return null
            fastestOffers.maxWithOrNull(fastestOfferComparator(isGooglePayAvailable))
        }
    }

    private fun offerComparator(isGooglePayAvailable: Boolean): Comparator<OnrampOffer> = Comparator { offer1, offer2 ->
        when (val quote1 = offer1.quote) {
            is OnrampQuote.Data -> {
                when (val quote2 = offer2.quote) {
                    is OnrampQuote.Data -> {
                        compareOffersByRateSpeedAndPriority(
                            isGooglePayAvailable = isGooglePayAvailable,
                            isSepaPrioritized = true,
                        ).compare(quote1, quote2)
                    }
                    else -> 1
                }
            }
            else -> -1
        }
    }

    private fun fastestOfferComparator(isGooglePayAvailable: Boolean): Comparator<OnrampOffer> =
        Comparator { offer1, offer2 ->
            when (val quote1 = offer1.quote) {
                is OnrampQuote.Data -> {
                    when (val quote2 = offer2.quote) {
                        is OnrampQuote.Data -> {
                            // For fastest offer, first compare by priority of speed
                            val priorityComparison = quote2.paymentMethod.type.getPriorityBySpeed(isGooglePayAvailable)
                                .compareTo(quote1.paymentMethod.type.getPriorityBySpeed(isGooglePayAvailable))

                            // If priorities are equal, compare by rate
                            if (priorityComparison != 0) {
                                priorityComparison
                            } else {
                                quote1.toAmount.value.compareTo(quote2.toAmount.value)
                            }
                        }
                        else -> 1
                    }
                }
                else -> -1
            }
        }

    private fun buildOffersBlocks(
        recentOffer: OnrampOffer?,
        bestRateOffer: OnrampOffer?,
        fastestOffer: OnrampOffer?,
        allQuotes: List<OnrampQuote>,
        isSingleOffer: Boolean,
    ): List<OnrampOffersBlock> {
        val recommendedOffers = buildRecommendedOffers(
            recentOffer = recentOffer,
            bestRateOffer = bestRateOffer,
            fastestOffer = fastestOffer,
            isSingleOffer = isSingleOffer,
        )

        val shownOffersCount = (if (recentOffer != null) 1 else 0) + recommendedOffers.size
        val hasMoreOffers = allQuotes.size > shownOffersCount

        return buildList {
            if (recentOffer != null) {
                add(
                    OnrampOffersBlock(
                        category = OnrampOfferCategory.Recent,
                        offers = listOf(
                            recentOffer.copy(
                                advantages = determineAdvantages(
                                    recentOffer,
                                    bestRateOffer,
                                    fastestOffer,
                                    isSingleOffer,
                                ),
                                rateDif = if (bestRateOffer != null) recentOffer.rateDif else null,
                            ),
                        ),
                        hasMoreOffers = hasMoreOffers,
                    ),
                )
            }

            if (recommendedOffers.isNotEmpty()) {
                add(
                    OnrampOffersBlock(
                        category = OnrampOfferCategory.Recommended,
                        offers = recommendedOffers,
                        hasMoreOffers = hasMoreOffers,
                    ),
                )
            }
        }
    }

    private fun determineAdvantages(
        recentOffer: OnrampOffer,
        bestRateOffer: OnrampOffer?,
        fastestOffer: OnrampOffer?,
        isSingleOffer: Boolean,
    ): OnrampOfferAdvantages {
        if (isSingleOffer) {
            return OnrampOfferAdvantages.Default
        }
        if (isSameOffer(recentOffer, bestRateOffer) && isSameOffer(recentOffer, fastestOffer)) {
            return OnrampOfferAdvantages.GreatRate
        }
        if (isSameOffer(recentOffer, bestRateOffer)) {
            return OnrampOfferAdvantages.GreatRate
        }
        if (isSameOffer(recentOffer, fastestOffer)) {
            return OnrampOfferAdvantages.Fastest
        }
        return OnrampOfferAdvantages.Default
    }

    private fun buildRecommendedOffers(
        recentOffer: OnrampOffer?,
        bestRateOffer: OnrampOffer?,
        fastestOffer: OnrampOffer?,
        isSingleOffer: Boolean,
    ): List<OnrampOffer> {
        return buildList {
            if (isSameOffer(bestRateOffer, fastestOffer)) {
                if (bestRateOffer != null && !isSameOffer(bestRateOffer, recentOffer)) {
                    add(
                        bestRateOffer.copy(
                            advantages = if (isSingleOffer) {
                                OnrampOfferAdvantages.Default
                            } else {
                                OnrampOfferAdvantages.GreatRate
                            },
                            rateDif = null,
                        ),
                    )
                }
            } else {
                if (bestRateOffer != null && !isSameOffer(bestRateOffer, recentOffer)) {
                    add(
                        bestRateOffer.copy(
                            advantages = if (isSingleOffer) {
                                OnrampOfferAdvantages.Default
                            } else {
                                OnrampOfferAdvantages.GreatRate
                            },
                            rateDif = null,
                        ),
                    )
                }

                if (fastestOffer != null && !isSameOffer(fastestOffer, recentOffer) &&
                    !isSameOffer(fastestOffer, bestRateOffer)
                ) {
                    add(
                        fastestOffer.copy(
                            advantages = if (isSingleOffer) {
                                OnrampOfferAdvantages.Default
                            } else {
                                OnrampOfferAdvantages.Fastest
                            },
                            rateDif = if (bestRateOffer != null) fastestOffer.rateDif else null,
                        ),
                    )
                }
            }
        }
    }

    private fun isSameOffer(offer1: OnrampOffer?, offer2: OnrampOffer?): Boolean {
        if (offer1 == null || offer2 == null) return false
        return offer1.quote.provider.id == offer2.quote.provider.id &&
            offer1.quote.paymentMethod.id == offer2.quote.paymentMethod.id
    }

    private fun isRecentUsed(onrampStatus: OnrampStatus.Status): Boolean {
        return when (onrampStatus) {
            OnrampStatus.Status.Created,
            OnrampStatus.Status.Expired,
            OnrampStatus.Status.Paused,
            OnrampStatus.Status.WaitingForPayment,
            OnrampStatus.Status.PaymentProcessing,
            OnrampStatus.Status.Verifying,
            OnrampStatus.Status.Paid,
            OnrampStatus.Status.Sending,
            OnrampStatus.Status.RefundInProgress,
            -> false
            OnrampStatus.Status.Failed,
            OnrampStatus.Status.Finished,
            OnrampStatus.Status.Refunded,
            -> true
        }
    }

    private companion object {
        const val MOONPAY_PROMO_PROVIDER_ID = "moonpay"
    }
}