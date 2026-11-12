package com.tangem.domain.tangempay.usecase.bankofframp

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.model.bankofframp.BankOfframpError
import com.tangem.domain.tangempay.model.bankofframp.BankOfframpOrderRequest
import com.tangem.domain.tangempay.model.bankofframp.BankOfframpOrderResult
import com.tangem.domain.tangempay.repository.BankOfframpOrderRepository

/**
 * The caller owns [idempotencyKey]: one key per Send attempt, reused when the same request is retried after a
 * transient failure, so the backend can never create a second withdrawal for a single tap.
 */
class CreateBankOfframpOrderUseCase(
    private val repository: BankOfframpOrderRepository,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        request: BankOfframpOrderRequest,
        idempotencyKey: String,
    ): Either<BankOfframpError, BankOfframpOrderResult> {
        return repository.createOrder(userWalletId = userWalletId, request = request, idempotencyKey = idempotencyKey)
    }
}