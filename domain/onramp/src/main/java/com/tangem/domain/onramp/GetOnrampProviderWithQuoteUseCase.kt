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
    ): OnrampProviderWithQuote? {
        val quoteData = this.filterIsInstance<OnrampQuote.Data>()
        val matchedPaymentMethodQuote = quoteData.firstOrNull { it.paymentMethod == selectedPaymentMethod }
        val paymentMethodNotSupported = quoteData.filterNot { it.paymentMethod == selectedPaymentMethod }
        val amountError =
            filterIsInstance<OnrampQuote.Error>().firstOrNull { it.paymentMethod == selectedPaymentMethod }
        return when {
            matchedPaymentMethodQuote != null -> {
                OnrampProviderWithQuote.Data(
                    provider = matchedPaymentMethodQuote.provider,
                    paymentMethod = matchedPaymentMethodQuote.paymentMethod,
                    toAmount = matchedPaymentMethodQuote.toAmount,
                    fromAmount = matchedPaymentMethodQuote.fromAmount,
                )
            }
            paymentMethodNotSupported.isNotEmpty() -> {
                Unavailable.NotSupportedPaymentMethod(
                    provider = provider,
                    availablePaymentMethods = paymentMethodNotSupported.map(OnrampQuote.Data::paymentMethod),
                )
            }
            amountError != null -> {
                Unavailable.Error(
                    amountError.provider,
                    amountError,
                )
            }
            else -> null
        }
    }
}
