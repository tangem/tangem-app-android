package com.tangem.domain.earn.usecase

import arrow.core.Either
import com.tangem.domain.earn.repository.EarnRepository

class FetchTopEarnTokensUseCase(
    private val repository: EarnRepository,
) {

    suspend operator fun invoke(limit: Int = DEFAULT_LIMIT): Either<Throwable, Unit> {
        return Either.catch {
            repository.fetchTopEarnTokens(
                limit = limit,
            )
        }
    }

    private companion object {
        private const val DEFAULT_LIMIT = 5
    }
}