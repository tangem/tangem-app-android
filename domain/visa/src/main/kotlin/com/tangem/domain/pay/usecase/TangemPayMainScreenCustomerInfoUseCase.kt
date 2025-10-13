package com.tangem.domain.pay.usecase

import com.tangem.domain.pay.model.MainScreenCustomerInfo
import com.tangem.domain.pay.repository.OnboardingRepository

/**
 * Returns tangem pay customer info for the main screen banner
 * Works only if the user already authorised at least once (won't emit anything otherwise)
 */
class TangemPayMainScreenCustomerInfoUseCase(
    private val repository: OnboardingRepository,
) {

    suspend operator fun invoke(): MainScreenCustomerInfo? = repository.getMainScreenCustomerInfo().getOrNull()
}