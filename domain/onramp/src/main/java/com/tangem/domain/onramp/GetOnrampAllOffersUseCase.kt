package com.tangem.domain.onramp

import arrow.core.left
import arrow.core.right
import com.tangem.domain.core.utils.EitherFlow
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.onramp.model.OnrampOffer
import com.tangem.domain.onramp.model.OnrampOfferAdvantages
import com.tangem.domain.onramp.model.OnrampPaymentMethodGroup
import com.tangem.domain.onramp.model.OnrampQuote
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.domain.onramp.repositories.OnrampErrorResolver
import com.tangem.domain.onramp.repositories.OnrampRepository
import com.tangem.domain.onramp.utils.calculateRateDif
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
        val validQuotes = quotes.filterIsInstance<OnrampQuote.Data>()
        if (validQuotes.isEmpty()) return emptyList()
        val isGooglePayAvailable = settingsRepository.isGooglePayAvailability()

        val overallBestRateQuote = validQuotes.maxByOrNull { it.toAmount.value }
        val bestRate = overallBestRateQuote?.toAmount?.value

        val offersByPaymentMethod = validQuotes.groupBy { it.paymentMethod }

        return offersByPaymentMethod.map { (paymentMethod, methodQuotes) ->
            val methodOffers = methodQuotes.map { quote ->
                val advantages = if (quote == overallBestRateQuote) {
                    OnrampOfferAdvantages.BestRate
                } else {
                    OnrampOfferAdvantages.Default
                }
                val rateDif = calculateRateDif(quote.toAmount.value, bestRate)
                OnrampOffer(quote = quote, rateDif = rateDif, advantages = advantages)
            }

            val groupBestRateOfferData = methodQuotes.maxByOrNull { it.toAmount.value }
            val groupBestRateOffer = methodOffers.find {
                when (val quote = it.quote) {
                    is OnrampQuote.Data -> quote == groupBestRateOfferData
                    else -> false
                }
            }

            OnrampPaymentMethodGroup(
                paymentMethod = paymentMethod,
                offers = methodOffers.sortedByDescending { offer ->
                    when (val quote = offer.quote) {
                        is OnrampQuote.Data -> quote.toAmount.value
                        else -> BigDecimal.ZERO
                    }
                },
                providerCount = methodOffers.map { it.quote.provider.id }.distinct().size,
                bestRateOffer = groupBestRateOffer,
                isBestPaymentMethod = overallBestRateQuote?.paymentMethod == paymentMethod,
            )
        }.sortedBy { it.paymentMethod.type.getPriority(isGooglePayAvailable) }
    }
}