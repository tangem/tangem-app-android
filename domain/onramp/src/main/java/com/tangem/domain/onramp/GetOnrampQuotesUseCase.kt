package com.tangem.domain.onramp

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.onramp.model.OnrampQuote
import com.tangem.domain.onramp.repositories.OnrampRepository
import com.tangem.domain.settings.repositories.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.math.BigDecimal

class GetOnrampQuotesUseCase(
    private val settingsRepository: SettingsRepository,
    private val repository: OnrampRepository,
) {

    operator fun invoke(): Flow<Either<Throwable, List<OnrampQuote>>> {
        return repository.getQuotes()
            .map<List<OnrampQuote>, Either<Throwable, List<OnrampQuote>>> { quotes ->
                val isGooglePayAvailable = settingsRepository.isGooglePayAvailability()

                quotes.groupBy { it.paymentMethod.type }
                    .asSequence()
                    .sortedBy { it.key.getPriority(isGooglePayAvailable) }
                    .map { grouped ->
                        grouped.value.sortedByDescending {
                            (it as? OnrampQuote.Data)?.toAmount?.value ?: BigDecimal.ZERO
                        }
                    }
                    .toList()
                    .flatten()
                    .right()
            }
            .catch { emit(it.left()) }
    }
}
