package com.tangem.domain.onramp

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.onramp.model.OnrampQuote
import com.tangem.domain.onramp.model.PaymentMethodType
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.domain.onramp.repositories.OnrampErrorResolver
import com.tangem.domain.onramp.repositories.OnrampRepository
import com.tangem.domain.settings.repositories.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class GetOnrampQuotesUseCase(
    private val settingsRepository: SettingsRepository,
    private val repository: OnrampRepository,
    private val errorResolver: OnrampErrorResolver,
) {

    operator fun invoke(): Flow<Either<OnrampError, List<OnrampQuote>>> {
        return repository.getQuotes()
            .map<List<OnrampQuote>, Either<OnrampError, List<OnrampQuote>>> { quotes ->
                val isGooglePayAvailable = settingsRepository.isGooglePayAvailability()

                quotes.groupBy { it.paymentMethod.type }
                    .asSequence()
                    .sortedBy { it.key.getPriority(isGooglePayAvailable) }
                    .sortByRate()
                    .toList()
                    .flatten()
                    .right()
            }
            .catch {
                emit(errorResolver.resolve(it).left())
            }
    }

    /**
     * Sorting providers by rule:
     *
     * 1. Highest rate
     * 2. Smallest difference between entered amount and required min/max amount
     */
    private fun Sequence<Map.Entry<PaymentMethodType, List<OnrampQuote>>>.sortByRate() = map { grouped ->
        grouped.value.sortedByDescending {
            when (it) {
                is OnrampQuote.Data -> it.toAmount.value

                // negative difference to sort both when data and unavailable is present
                is OnrampQuote.Error -> {
                    when (val error = it.error) {
                        is OnrampError.AmountError.TooSmallError -> it.fromAmount.value - error.requiredAmount
                        is OnrampError.AmountError.TooBigError -> error.requiredAmount - it.fromAmount.value
                        else -> null
                    }
                }
            }
        }
    }
}