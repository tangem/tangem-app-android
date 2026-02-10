package com.tangem.domain.earn.usecase

import arrow.core.Either
import com.tangem.domain.earn.repository.EarnRepository

class FetchEarnNetworksUseCase(
    private val repository: EarnRepository,
) {

    suspend operator fun invoke(type: String): Either<Throwable, Unit> {
        return Either.catch {
            repository.fetchEarnNetworks(type = type)
        }
    }
}