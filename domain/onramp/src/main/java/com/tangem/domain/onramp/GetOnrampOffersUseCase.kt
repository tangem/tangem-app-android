package com.tangem.domain.onramp

import arrow.core.left
import arrow.core.right
import com.tangem.domain.core.utils.EitherFlow
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.onramp.model.*
import com.tangem.domain.onramp.model.cache.OnrampTransaction
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.domain.onramp.repositories.OnrampErrorResolver
import com.tangem.domain.onramp.repositories.OnrampRepository
import com.tangem.domain.onramp.repositories.OnrampTransactionRepository
import com.tangem.domain.onramp.utils.calculateRateDif
import com.tangem.domain.onramp.utils.compareOffersByRateSpeedAndPriority
import com.tangem.domain.settings.repositories.SettingsRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class GetOnrampOffersUseCase(
    private val onrampRepository: OnrampRepository,
    private val onrampTransactionRepository: OnrampTransactionRepository,
    private val errorResolver: OnrampErrorResolver,
    private val settingsRepository: SettingsRepository,
) {

    operator fun invoke(
        userWalletId: UserWalletId,
        cryptoCurrencyId: CryptoCurrency.ID,
    ): EitherFlow<OnrampError, List<OnrampOffersBlock>> {
        return combine(
            onrampRepository.getQuotes(),
            onrampTransactionRepository.getTransactions(userWalletId, cryptoCurrencyId),
        ) { quotes, transactions ->
            processOffers(quotes, transactions)
        }
            .map { offers -> offers.right() }
            .catch { throwable -> errorResolver.resolve(throwable).left() }
    }

    private suspend fun processOffers(
        quotes: List<OnrampQuote>,
        transactions: List<OnrampTransaction>,
    ): List<OnrampOffersBlock> {
        val validQuotes = quotes.filterIsInstance<OnrampQuote.Data>()
        if (validQuotes.isEmpty()) return emptyList()

        val isGooglePayAvailable = settingsRepository.isGooglePayAvailability()
        val bestRateQuote = validQuotes.maxWithOrNull(compareOffersByRateSpeedAndPriority(isGooglePayAvailable))
        val bestRate = bestRateQuote?.toAmount?.value

        val offers = validQuotes.map { quote ->
            val rateDif = calculateRateDif(quote.toAmount.value, bestRate)
            OnrampOffer(quote = quote, rateDif = rateDif)
        }

        val recentOffer = findRecentOffer(offers, transactions)
        val bestRateOffer = findBestRateOffer(offers, isGooglePayAvailable)
        val fastestOffer = findFastestOffer(offers, isGooglePayAvailable)

        return buildOffersBlocks(
            recentOffer = recentOffer,
            bestRateOffer = bestRateOffer,
            fastestOffer = fastestOffer,
            allOffers = offers,
        )
    }

    private fun findRecentOffer(offers: List<OnrampOffer>, transactions: List<OnrampTransaction>): OnrampOffer? {
        val lastTransaction = transactions.maxByOrNull { it.timestamp } ?: return null

        return offers.find { offer ->
            offer.quote.provider.id == lastTransaction.providerType &&
                offer.quote.paymentMethod.id == lastTransaction.paymentMethod
        }
    }

    private fun findBestRateOffer(offers: List<OnrampOffer>, isGooglePayAvailable: Boolean): OnrampOffer? {
        return offers.maxWithOrNull(offerComparator(isGooglePayAvailable))
    }

    private fun findFastestOffer(offers: List<OnrampOffer>, isGooglePayAvailable: Boolean): OnrampOffer? {
        val instantOffers = offers.filter { it.quote.paymentMethod.type.isInstant() }
        return if (instantOffers.isNotEmpty()) {
            instantOffers.maxWithOrNull(offerComparator(isGooglePayAvailable))
        } else {
            val offersBySpeed = offers.groupBy { offer ->
                offer.quote.paymentMethod.type.getProcessingSpeed().speed
            }
            val fastestSpeed = offersBySpeed.keys.minOrNull() ?: return null
            val fastestOffers = offersBySpeed[fastestSpeed] ?: return null
            fastestOffers.maxWithOrNull(offerComparator(isGooglePayAvailable))
        }
    }

    private fun offerComparator(isGooglePayAvailable: Boolean): Comparator<OnrampOffer> = Comparator { offer1, offer2 ->
        when (val quote1 = offer1.quote) {
            is OnrampQuote.Data -> {
                when (val quote2 = offer2.quote) {
                    is OnrampQuote.Data -> {
                        compareOffersByRateSpeedAndPriority(isGooglePayAvailable).compare(quote1, quote2)
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
        allOffers: List<OnrampOffer>,
    ): List<OnrampOffersBlock> {
        val recommendedOffers = buildRecommendedOffers(
            recentOffer = recentOffer,
            bestRateOffer = bestRateOffer,
            fastestOffer = fastestOffer,
        )

        val shownOffersCount = (if (recentOffer != null) 1 else 0) + recommendedOffers.size
        val hasMoreOffers = allOffers.size > shownOffersCount

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
                                ),
                                rateDif = if (bestRateOffer != null) recentOffer.rateDif else null,
                            ),
                        ),
                        hasMoreOffers = false,
                    ),
                )
            }

            if (recommendedOffers.isNotEmpty() && hasOnlyOneMethodAndProvider(allOffers).not()) {
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
    ): OnrampOfferAdvantages {
        if (isSameOffer(recentOffer, bestRateOffer) && isSameOffer(recentOffer, fastestOffer)) {
            return OnrampOfferAdvantages.BestRate
        }
        if (isSameOffer(recentOffer, bestRateOffer)) {
            return OnrampOfferAdvantages.BestRate
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
    ): List<OnrampOffer> {
        return buildList {
            if (isSameOffer(bestRateOffer, fastestOffer)) {
                bestRateOffer?.let { offer ->
                    add(
                        offer.copy(
                            advantages = OnrampOfferAdvantages.BestRate,
                            rateDif = null,
                        ),
                    )
                }
            } else {
                if (bestRateOffer != null && !isSameOffer(bestRateOffer, recentOffer)) {
                    add(
                        bestRateOffer.copy(
                            advantages = OnrampOfferAdvantages.BestRate,
                            rateDif = null,
                        ),
                    )
                }

                if (fastestOffer != null && !isSameOffer(fastestOffer, recentOffer) &&
                    !isSameOffer(fastestOffer, bestRateOffer)
                ) {
                    add(
                        fastestOffer.copy(
                            advantages = OnrampOfferAdvantages.Fastest,
                            rateDif = if (bestRateOffer != null) fastestOffer.rateDif else null,
                        ),
                    )
                }
            }
        }
    }

    private fun hasOnlyOneMethodAndProvider(offers: List<OnrampOffer>): Boolean {
        val uniquePaymentMethods = offers.map { it.quote.paymentMethod.id }.distinct()
        val uniqueProviders = offers.map { it.quote.provider.id }.distinct()
        return uniquePaymentMethods.size == 1 && uniqueProviders.size == 1
    }

    private fun isSameOffer(offer1: OnrampOffer?, offer2: OnrampOffer?): Boolean {
        if (offer1 == null || offer2 == null) return false
        return offer1.quote.provider.id == offer2.quote.provider.id &&
            offer1.quote.paymentMethod.id == offer2.quote.paymentMethod.id
    }
}