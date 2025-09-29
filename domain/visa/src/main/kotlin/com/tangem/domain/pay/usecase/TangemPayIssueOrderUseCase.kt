package com.tangem.domain.pay.usecase

import com.tangem.domain.pay.repository.OnboardingRepository

class TangemPayIssueOrderUseCase(
    private val repository: OnboardingRepository,
) {

    suspend operator fun invoke(): Unit? = repository.createOrder().getOrNull()
}