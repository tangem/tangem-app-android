package com.tangem.domain.pay.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.pay.repository.OnboardingRepository

class ProduceTangemPayInitialDataUseCase(
    private val repository: OnboardingRepository,
) {

    suspend operator fun invoke(): Either<Throwable, Unit> {
        return either {
            val isDataProduced = repository.isTangemPayInitialDataProduced()
            if (isDataProduced) {
                return@either Unit
            } else {
                repository.produceInitialData()
            }
        }
    }
}