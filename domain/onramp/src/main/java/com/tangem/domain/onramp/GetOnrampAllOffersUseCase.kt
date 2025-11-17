package com.tangem.domain.onramp

import arrow.core.left
import arrow.core.right
import com.tangem.domain.core.utils.EitherFlow
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.onramp.model.*
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.domain.onramp.repositories.OnrampErrorResolver
import com.tangem.domain.onramp.repositories.OnrampRepository
import com.tangem.domain.onramp.utils.calculateRateDif
import com.tangem.domain.onramp.utils.compareOffersByRateSpeedAndPriority
import com.tangem.domain.settings.repositories.SettingsRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.math.BigDecimal

class GetOnrampAllOffersUseCase(
    private val onrampRepository: OnrampRepository,
    private val errorResolver: OnrampErrorResolver,
    private val settingsRepository: SettingsRepository,
) {

    operator fun invoke(
        userWalletId: UserWalletId,
        cryptoCurrencyId: CryptoCurrency.ID,
    ): EitherFlow<OnrampError, List<OnrampPaymentMethodGroup>> {
        return onrampRepository.getQuotes()
            .map { quotes -> processAllOffers(quotes).right() }
            .catch { throwable -> errorResolver.resolve(throwable).left() }
    }

    private suspend fun processAllOffers(quotes: List<OnrampQuote>): List<OnrampPaymentMethodGroup> {
        val relevantQuotes = quotes.filter { it !is OnrampQuote.Error }
        if (relevantQuotes.isEmpty()) return emptyList()

        val isGooglePayAvailable = settingsRepository.isGooglePayAvailability()

        val validQuotes = relevantQuotes.filterIsInstance<OnrampQuote.Data>()
        val overallBestRateQuote = findOverallBestRateQuote(validQuotes, isGooglePayAvailable)
        val bestRate = overallBestRateQuote?.toAmount?.value

        val offersByPaymentMethod = relevantQuotes.groupBy { it.paymentMethod }

        return offersByPaymentMethod
            .map { (paymentMethod, methodQuotes) ->
                createPaymentMethodGroup(
                    paymentMethod = paymentMethod,
                    methodQuotes = methodQuotes,
                    overallBestRateQuote = overallBestRateQuote,
                    bestRate = bestRate,
                    isGooglePayAvailable = isGooglePayAvailable,
                )
            }
            .sortedBy { it.paymentMethod.type.getPriorityForMethod(isGooglePayAvailable) }
    }

    private fun findOverallBestRateQuote(
        validQuotes: List<OnrampQuote.Data>,
        isGooglePayAvailable: Boolean,
    ): OnrampQuote.Data? {
        return validQuotes.maxWithOrNull(
            compareOffersByRateSpeedAndPriority(
                isGooglePayAvailable = isGooglePayAvailable,
                isSepaPrioritized = false,
            ),
        )
    }

    private fun createPaymentMethodGroup(
        paymentMethod: OnrampPaymentMethod,
        methodQuotes: List<OnrampQuote>,
        overallBestRateQuote: OnrampQuote.Data?,
        bestRate: BigDecimal?,
        isGooglePayAvailable: Boolean,
    ): OnrampPaymentMethodGroup {
        val methodOffers = methodQuotes.mapNotNull { quote ->
            createOffer(quote, overallBestRateQuote, bestRate)
        }

        val groupBestRateQuote = findGroupBestRateQuote(methodQuotes, isGooglePayAvailable)
        val groupBestRateOffer = findBestRateOffer(methodOffers, groupBestRateQuote)

        return OnrampPaymentMethodGroup(
            paymentMethod = paymentMethod,
            offers = sortOffers(methodOffers),
            providerCount = countUniqueProviders(methodOffers),
            bestRateOffer = groupBestRateOffer,
            isBestPaymentMethod = overallBestRateQuote?.paymentMethod == paymentMethod,
            methodStatus = determineMethodStatus(methodQuotes),
        )
    }

    private fun createOffer(
        quote: OnrampQuote,
        overallBestRateQuote: OnrampQuote.Data?,
        bestRate: BigDecimal?,
    ): OnrampOffer? {
        return when (quote) {
            is OnrampQuote.Data -> {
                val advantages = if (quote == overallBestRateQuote) {
                    OnrampOfferAdvantages.BestRate
                } else {
                    OnrampOfferAdvantages.Default
                }
                val rateDif = calculateRateDif(quote.toAmount.value, bestRate)
                OnrampOffer(quote = quote, rateDif = rateDif, advantages = advantages)
            }
            is OnrampQuote.AmountError -> {
                OnrampOffer(quote = quote, rateDif = null, advantages = OnrampOfferAdvantages.Default)
            }
            is OnrampQuote.Error -> null
        }
    }

    private fun findGroupBestRateQuote(
        validMethodQuotes: List<OnrampQuote>,
        isGooglePayAvailable: Boolean,
    ): OnrampQuote? {
        val dataQuotes = validMethodQuotes.filterIsInstance<OnrampQuote.Data>()
        val amountErrorQuotes = validMethodQuotes.filterIsInstance<OnrampQuote.AmountError>()

        return when {
            dataQuotes.isNotEmpty() -> {
                dataQuotes.maxWithOrNull(
                    compareOffersByRateSpeedAndPriority(
                        isGooglePayAvailable = isGooglePayAvailable,
                        isSepaPrioritized = false,
                    ),
                )
            }
            amountErrorQuotes.isNotEmpty() -> {
                amountErrorQuotes.minByOrNull { it.error.requiredAmount }
            }
            else -> validMethodQuotes.firstOrNull()
        }
    }

    private fun findBestRateOffer(methodOffers: List<OnrampOffer>, groupBestRateQuote: OnrampQuote?): OnrampOffer? {
        if (groupBestRateQuote == null) return null

        return methodOffers.find { offer ->
            when (val quote = offer.quote) {
                is OnrampQuote.Data -> quote == groupBestRateQuote
                is OnrampQuote.AmountError -> quote == groupBestRateQuote
                is OnrampQuote.Error -> false
            }
        }
    }

    private fun sortOffers(methodOffers: List<OnrampOffer>): List<OnrampOffer> {
        val (availableOffers, unavailableOffers) = methodOffers.partition { it.quote is OnrampQuote.Data }

        val sortedAvailableOffers = sortAvailableOffers(availableOffers)
        val sortedUnavailableOffers = sortUnavailableOffers(unavailableOffers)

        return sortedAvailableOffers + sortedUnavailableOffers
    }

    private fun sortAvailableOffers(availableOffers: List<OnrampOffer>): List<OnrampOffer> {
        return availableOffers.sortedByDescending { offer ->
            when (val quote = offer.quote) {
                is OnrampQuote.Data -> quote.toAmount.value
                else -> BigDecimal.ZERO
            }
        }
    }

    private fun sortUnavailableOffers(unavailableOffers: List<OnrampOffer>): List<OnrampOffer> {
        if (unavailableOffers.isEmpty()) return emptyList()

        val (amountErrorOffers, otherOffers) = unavailableOffers.partition { it.quote is OnrampQuote.AmountError }
        if (amountErrorOffers.isEmpty()) return otherOffers

        val (tooSmallOffers, tooBigOffers) = amountErrorOffers.partition { offer ->
            val error = (offer.quote as OnrampQuote.AmountError).error
            error is OnrampError.AmountError.TooSmallError
        }

        val sortedTooSmallOffers = tooSmallOffers.sortedBy { offer ->
            (offer.quote as OnrampQuote.AmountError).error.requiredAmount
        }
        val sortedTooBigOffers = tooBigOffers.sortedByDescending { offer ->
            (offer.quote as OnrampQuote.AmountError).error.requiredAmount
        }

        return sortedTooSmallOffers + sortedTooBigOffers + otherOffers
    }

    private fun countUniqueProviders(methodOffers: List<OnrampOffer>): Int {
        return methodOffers.map { it.quote.provider.id }.distinct().size
    }

    private fun determineMethodStatus(methodQuotes: List<OnrampQuote>): PaymentMethodStatus {
        val hasAtLeastOneValidQuote = methodQuotes.any { it is OnrampQuote.Data }
        return if (hasAtLeastOneValidQuote) {
            PaymentMethodStatus.Available
        } else {
            val amountErrorQuotes = methodQuotes.filterIsInstance<OnrampQuote.AmountError>()
            val minAmountErrors = amountErrorQuotes.filter { it.error is OnrampError.AmountError.TooSmallError }

            if (minAmountErrors.isNotEmpty()) {
                val minRequiredAmount = minAmountErrors.minOf { it.error.requiredAmount }
                PaymentMethodStatus.Unavailable.MinAmount(minRequiredAmount)
            } else {
                val maxAmount = amountErrorQuotes.maxOfOrNull { it.error.requiredAmount } ?: BigDecimal.ZERO
                PaymentMethodStatus.Unavailable.MaxAmount(maxAmount)
            }
        }
    }
}