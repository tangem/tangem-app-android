package com.tangem.domain.onramp

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.onramp.model.OnrampQuote
import com.tangem.domain.onramp.repositories.OnrampRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class GetOnrampQuotesUseCase(private val repository: OnrampRepository) {

    operator fun invoke(): Flow<Either<Throwable, List<OnrampQuote>>> {
        return repository.getQuotes()
            .map<List<OnrampQuote>, Either<Throwable, List<OnrampQuote>>> { it.right() }
            .catch { emit(it.left()) }
    }
}