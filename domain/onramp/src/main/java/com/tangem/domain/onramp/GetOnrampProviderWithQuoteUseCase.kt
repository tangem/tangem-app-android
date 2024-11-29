package com.tangem.domain.onramp

import arrow.core.Either
import com.tangem.domain.onramp.model.OnrampPaymentMethod
import com.tangem.domain.onramp.model.OnrampProvider
import com.tangem.domain.onramp.model.OnrampProviderWithQuote
import com.tangem.domain.onramp.model.OnrampProviderWithQuote.Unavailable
import com.tangem.domain.onramp.model.OnrampQuote
import com.tangem.domain.onramp.repositories.OnrampRepository

class GetOnrampProviderWithQuoteUseCase(private val repository: OnrampRepository) {

    suspend operator fun invoke(paymentMethod: OnrampPaymentMethod): Either<Throwable, List<OnrampProviderWithQuote>> {
        return Either.catch {
            val quotes: List<OnrampQuote> = requireNotNull(repository.getQuotesSync()) { "Quotes must not be null" }
            quotes
                .groupBy { it.provider }
                .mapNotNull { (provider, quotes) ->
                    quotes.quoteWithProvider(provider = provider, selectedPaymentMethod = paymentMethod)
                }
        }
    }

    private fun List<OnrampQuote>.quoteWithProvider(
        provider: OnrampProvider,
        selectedPaymentMethod: OnrampPaymentMethod,
    ): OnrampProviderWithQuote? {
        val quoteData = this.filterIsInstance<OnrampQuote.Data>()
        val matchedPaymentMethodQuote = quoteData.firstOrNull { it.paymentMethod == selectedPaymentMethod }

        if (matchedPaymentMethodQuote != null) {
            return OnrampProviderWithQuote.Data(
                provider = matchedPaymentMethodQuote.provider,
                paymentMethod = matchedPaymentMethodQuote.paymentMethod,
                toAmount = matchedPaymentMethodQuote.toAmount,
                fromAmount = matchedPaymentMethodQuote.fromAmount,
            )
        }

        val paymentMethodNotSupported = quoteData.filterNot { it.paymentMethod == selectedPaymentMethod }
        if (paymentMethodNotSupported.isNotEmpty()) {
            return Unavailable.NotSupportedPaymentMethod(
                provider = provider,
                availablePaymentMethods = paymentMethodNotSupported.map(OnrampQuote.Data::paymentMethod),
            )
        }

        val amountError = this
            .filterIsInstance<OnrampQuote.Error>()
            .firstOrNull { it.paymentMethod == selectedPaymentMethod }

        return when {
            amountError != null -> {
                when (amountError) {
                    is OnrampQuote.Error.AmountTooBigError -> Unavailable.AvailableUpTo(
                        provider = amountError.provider,
                        amount = amountError.amount,
                    )
                    is OnrampQuote.Error.AmountTooSmallError -> Unavailable.AvailableFrom(
                        provider = amountError.provider,
                        amount = amountError.amount,
                    )
                }
            }
            else -> null
        }
    }
}