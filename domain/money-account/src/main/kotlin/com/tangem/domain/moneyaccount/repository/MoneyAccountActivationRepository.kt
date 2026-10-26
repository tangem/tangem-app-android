package com.tangem.domain.moneyaccount.repository

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.model.MoneyAccountActivationError
import com.tangem.domain.visa.model.MoneyAccountDepositCredentials

interface MoneyAccountActivationRepository {

    suspend fun activateMoneyAccount(
        userWalletId: UserWalletId,
    ): Either<MoneyAccountActivationError, MoneyAccountDepositCredentials>
}