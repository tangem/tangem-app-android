package com.tangem.domain.pay.usecase

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.model.TangemPayWaitlistEntry
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.pay.repository.TangemPayWaitlistRepository
import com.tangem.domain.visa.error.VisaApiError

class JoinTangemPayWaitlistUseCase(
    private val waitlistRepository: TangemPayWaitlistRepository,
    private val onboardingRepository: OnboardingRepository,
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher,
) {
    suspend operator fun invoke(
        userWalletId: UserWalletId?,
        entry: TangemPayWaitlistEntry,
        idempotencyKey: String,
    ): Either<VisaApiError, Unit> {
        return waitlistRepository.join(
            userWalletId = userWalletId,
            entry = entry,
            idempotencyKey = idempotencyKey,
        ).onRight {
            if (userWalletId != null) {
                hidePaymentAccount(userWalletId)
            }
        }
    }

    private suspend fun hidePaymentAccount(userWalletId: UserWalletId) {
        onboardingRepository.disableTangemPay(userWalletId)
            .onRight { paymentAccountStatusFetcher(userWalletId) }
    }
}