package com.tangem.domain.pay.usecase

import arrow.core.Either
import arrow.core.Either.Companion.catch
import com.tangem.domain.pay.repository.OnboardingRepository

class ProduceTangemPayInitialDataUseCase(
    private val repository: OnboardingRepository,
) {

    suspend operator fun invoke(): Either<Throwable, Unit> {
        return catch {
            val isDataProduced = repository.isTangemPayInitialDataProduced()
            if (isDataProduced) {
                return@catch Unit
            } else {
                repository.produceInitialData()
            }
        }
    }
}