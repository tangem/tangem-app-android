package com.tangem.domain.onramp

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.onramp.model.OnrampQuote
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.domain.onramp.repositories.OnrampErrorResolver
import com.tangem.domain.onramp.repositories.OnrampRepository
import com.tangem.domain.settings.repositories.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.util.Comparator

class GetOnrampV2QuotesUseCase(
    private val settingsRepository: SettingsRepository,
    private val repository: OnrampRepository,
    private val errorResolver: OnrampErrorResolver,
) {

    operator fun invoke(): Flow<Either<OnrampError, List<OnrampQuote>>> {
        return repository.getQuotes()
            .map<List<OnrampQuote>, Either<OnrampError, List<OnrampQuote>>> { quotes ->
                val isGooglePayAvailable = settingsRepository.isGooglePayAvailability()
                quotes.sortedWith(
                    Comparator.comparing<OnrampQuote, SortableBigDecimalWrapper> { quote ->
                        getQuoteSortPriority(quote)
                    }
                        .thenComparingInt { quote ->
                            quote.paymentMethod.type.getPriority(isGooglePayAvailable)
                        },
                ).right()
            }
            .catch {
                emit(errorResolver.resolve(it).left())
            }
    }

    private class SortableBigDecimalWrapper(
        val value: BigDecimal?,
        val negateForSort: Boolean = false,
    ) : Comparable<SortableBigDecimalWrapper> {
        override fun compareTo(other: SortableBigDecimalWrapper): Int {
            return when {
                value == null && other.value == null -> 0
                value == null -> 1
                other.value == null -> -1
                else -> {
                    val thisValue = if (negateForSort) value.negate() else value
                    val otherValue = if (other.negateForSort) other.value.negate() else other.value
                    thisValue.compareTo(otherValue)
                }
            }
        }
    }

    private fun getQuoteSortPriority(quote: OnrampQuote): SortableBigDecimalWrapper {
        return when (quote) {
            is OnrampQuote.Data -> SortableBigDecimalWrapper(quote.toAmount.value, negateForSort = true)
            is OnrampQuote.Error -> SortableBigDecimalWrapper(null)
            is OnrampQuote.AmountError -> {
                when (val error = quote.error) {
                    is OnrampError.AmountError.TooSmallError ->
                        SortableBigDecimalWrapper((quote.fromAmount.value - error.requiredAmount).abs())
                    is OnrampError.AmountError.TooBigError ->
                        SortableBigDecimalWrapper((error.requiredAmount - quote.fromAmount.value).abs())
                }
            }
        }
    }
}