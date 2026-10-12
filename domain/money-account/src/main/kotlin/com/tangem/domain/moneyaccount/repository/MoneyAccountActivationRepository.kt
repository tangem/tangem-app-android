package com.tangem.domain.moneyaccount.repository

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.visa.model.MoneyAccountDepositCredentials

interface MoneyAccountActivationRepository {

    @Throws
    suspend fun activateMoneyAccount(userWalletId: UserWalletId): MoneyAccountDepositCredentials
}