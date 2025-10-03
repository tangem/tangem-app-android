package com.tangem.domain.pay.usecase

import arrow.core.Either
import com.tangem.core.error.UniversalError
import com.tangem.domain.pay.repository.OnboardingRepository

class TangemPayIssueOrderUseCase(
    private val repository: OnboardingRepository,
) {

    suspend operator fun invoke(): Either<UniversalError, Unit> = repository.createOrder()
}