package com.tangem.domain.pay.usecase

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.OrderData
import com.tangem.domain.pay.repository.CustomerOrderRepository
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.visa.error.VisaApiError

/**
 * Validates a locally stored `orderId` hint before reusing it.
 *
 * - If the hint is still active → returns the order data.
 * - If the hint is terminal → clears the hint and returns null.
 *
 * The caller decides whether to fall back to `findOrders` to recover the real state.
 */
class ValidateLocalOrderHintUseCase(
    private val customerOrderRepository: CustomerOrderRepository,
    private val onboardingRepository: OnboardingRepository,
) {
    suspend operator fun invoke(userWalletId: UserWalletId): Either<VisaApiError, OrderData?> {
        val orderId = onboardingRepository.getOrderId(userWalletId) ?: return Either.Right(null)
        return customerOrderRepository.getOrderData(userWalletId = userWalletId, orderId = orderId)
            .map { data ->
                if (data.status.isTerminal) {
                    onboardingRepository.clearOrderId(userWalletId)
                    null
                } else {
                    data
                }
            }
    }
}