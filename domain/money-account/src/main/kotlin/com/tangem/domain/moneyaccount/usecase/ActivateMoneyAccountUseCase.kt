package com.tangem.domain.moneyaccount.usecase

import arrow.core.Either
import arrow.core.Either.Companion.catch
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.repository.MoneyAccountActivationRepository
import com.tangem.domain.visa.model.MoneyAccountDepositCredentials

class ActivateMoneyAccountUseCase(
    private val repository: MoneyAccountActivationRepository,
) {

    suspend operator fun invoke(userWalletId: UserWalletId): Either<Throwable, MoneyAccountDepositCredentials> {
        return catch {
            repository.activateMoneyAccount(userWalletId)
        }
    }
}