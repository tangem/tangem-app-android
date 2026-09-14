package com.tangem.domain.pay.usecase.bankofframp

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.bankofframp.BankOfframpError
import com.tangem.domain.pay.repository.BankOfframpOrderRepository

class CancelBankOfframpOrderUseCase(
    private val repository: BankOfframpOrderRepository,
) {

    suspend operator fun invoke(userWalletId: UserWalletId, orderId: String): Either<BankOfframpError, Unit> {
        return repository.cancelOrder(userWalletId = userWalletId, orderId = orderId)
    }
}