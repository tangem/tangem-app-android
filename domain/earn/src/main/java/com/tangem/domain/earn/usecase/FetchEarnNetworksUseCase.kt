package com.tangem.domain.earn.usecase

import arrow.core.Either
import com.tangem.domain.earn.repository.EarnRepository

class FetchEarnNetworksUseCase(
    private val repository: EarnRepository,
) {

    suspend operator fun invoke(): Either<Throwable, Unit> {
        return Either.catch {
            repository.fetchEarnNetworks()
        }
    }
}