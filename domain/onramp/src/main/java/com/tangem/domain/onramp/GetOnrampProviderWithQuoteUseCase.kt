package com.tangem.domain.onramp

import arrow.core.Either
import com.tangem.domain.onramp.model.OnrampPaymentMethod
import com.tangem.domain.onramp.model.OnrampProvider
import com.tangem.domain.onramp.model.OnrampProviderWithQuote
import com.tangem.domain.onramp.model.OnrampProviderWithQuote.Unavailable
import com.tangem.domain.onramp.model.OnrampQuote
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.domain.onramp.repositories.OnrampErrorResolver
import com.tangem.domain.onramp.repositories.OnrampRepository

class GetOnrampProviderWithQuoteUseCase(
    private val repository: OnrampRepository,
    private val errorResolver: OnrampErrorResolver,
) {

    suspend operator fun invoke(
        paymentMethod: OnrampPaymentMethod,
    ): Either<OnrampError, List<OnrampProviderWithQuote>> {
        return Either.catch {
            val quotes: List<OnrampQuote> = requireNotNull(repository.getQuotesSync()) { "Quotes must not be null" }
            quotes
                .groupBy { it.provider }
                .mapNotNull { (provider, quotes) ->
                    quotes.quoteWithProvider(provider = provider, selectedPaymentMethod = paymentMethod)
                }
        }.mapLeft(errorResolver::resolve)
    }

    private fun List<OnrampQuote>.quoteWithProvider(
        provider: OnrampProvider,
        selectedPaymentMethod: OnrampPaymentMethod,
    ): OnrampProviderWithQuote {
        val matchedQuote = firstOrNull { it.paymentMethod == selectedPaymentMethod }
        return when (matchedQuote) {
            is OnrampQuote.Data -> OnrampProviderWithQuote.Data(
                provider = matchedQuote.provider,
                paymentMethod = matchedQuote.paymentMethod,
                toAmount = matchedQuote.toAmount,
                fromAmount = matchedQuote.fromAmount,
            )
            is OnrampQuote.Error -> Unavailable.Error(
                provider = matchedQuote.provider,
                quoteError = matchedQuote,
            )
            null -> Unavailable.NotSupportedPaymentMethod(
                provider = provider,
                availablePaymentMethods = provider.paymentMethods,
            )
        }
    }
}