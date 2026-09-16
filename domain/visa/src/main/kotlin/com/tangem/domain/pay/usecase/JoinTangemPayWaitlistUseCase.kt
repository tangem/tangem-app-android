package com.tangem.domain.pay.usecase

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.TangemPayWaitlistEntry
import com.tangem.domain.pay.repository.TangemPayWaitlistRepository
import com.tangem.domain.visa.error.VisaApiError

class JoinTangemPayWaitlistUseCase(
    private val waitlistRepository: TangemPayWaitlistRepository,
) {
    suspend operator fun invoke(
        userWalletId: UserWalletId?,
        entry: TangemPayWaitlistEntry,
        idempotencyKey: String,
    ): Either<VisaApiError, Unit> = waitlistRepository.join(
        userWalletId = userWalletId,
        entry = entry,
        idempotencyKey = idempotencyKey,
    )
}